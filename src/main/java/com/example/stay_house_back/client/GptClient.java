package com.example.stay_house_back.client;

import com.example.stay_house_back.dto.ws.PreferenceOption;
import com.example.stay_house_back.dto.ws.PreferenceQuestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class GptClient {

    private static final String MODEL = "gpt-4o-mini";
    private static final String TOOL_NAME = "submit_questions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GptClient(@Value("${openai.api-key}") String apiKey) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<PreferenceQuestion> generatePreferenceQuestions(List<Map<String, Object>> planSummaries) {
        try {
            Map<String, Object> requestBody = buildRequest(planSummaries);
            String responseJson = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            log.info("===== GPT raw 응답 =====\n{}", responseJson);
            return parseResponse(responseJson);
        } catch (Exception e) {
            log.warn("GPT API 호출 실패 (선호도 질문 생략): {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> buildRequest(List<Map<String, Object>> planSummaries) throws Exception {
        Map<String, Object> optionProperties = new LinkedHashMap<>();
        optionProperties.put("label", Map.of("type", "string"));
        optionProperties.put("boostPlanIds", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "description", "이 선택지를 골랐을 때 가산점을 줄 planId 목록. 상관없음 선택지는 빈 배열."
        ));

        Map<String, Object> optionItem = Map.of(
                "type", "object",
                "properties", optionProperties,
                "required", List.of("label", "boostPlanIds")
        );

        Map<String, Object> questionProperties = new LinkedHashMap<>();
        questionProperties.put("questionId", Map.of("type", "string"));
        questionProperties.put("text", Map.of("type", "string"));
        questionProperties.put("options", Map.of(
                "type", "array",
                "minItems", 2,
                "maxItems", 4,
                "items", optionItem
        ));

        // OpenAI tool 형식: {"type": "function", "function": {...}}
        Map<String, Object> tool = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", TOOL_NAME,
                        "description", "플랜 분석 후 사용자 선호도 질문 목록을 반환합니다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "questions", Map.of(
                                                "type", "array",
                                                "maxItems", 3,
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", questionProperties,
                                                        "required", List.of("questionId", "text", "options")
                                                )
                                        )
                                ),
                                "required", List.of("questions")
                        )
                )
        );

        String plansJson = objectMapper.writeValueAsString(planSummaries);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content",
                        "다음 플랜 후보들의 차이를 분석하고 선호도 질문을 생성해주세요:\n\n" + plansJson)
        );

        return Map.of(
                "model", MODEL,
                "messages", messages,
                "tools", List.of(tool),
                "tool_choice", Map.of("type", "function", "function", Map.of("name", TOOL_NAME))
        );
    }

    private String buildSystemPrompt() {
        return """
                당신은 청년 주거 금융 플래닝 시스템의 선호도 질문 생성기입니다.
                플랜 후보 목록을 분석해 사용자 선호를 파악할 질문을 최대 5개 생성하세요.
                각 질문의 답이 플랜 순위에 실질적으로 영향을 줘야 합니다.

                ## 꼭 확인할 차이 항목 (우선순위 순, 이 항목 외에 추가로 확인해야할 사항이 있다면 다른 질문 생성하세요.)

                1. 상품 유형: category가 POLICY_LOAN인 플랜과 BANK_LOAN인 플랜이 섞여 있으면
                   → "정부 지원 대출과 은행 대출 중 어떤 것을 선호하시나요?" 질문 생성
                   → "정부 지원 대출" 선택지: boostPlanIds = category가 POLICY_LOAN인 플랜들의 planId
                   → "은행 대출" 선택지: boostPlanIds = category가 null(BANK_LOAN)인 플랜들의 planId

                2. 가입 방법: joinWay 값이 다른 플랜들이 있으면
                   → 가입 방법 선호도 질문 생성
                   → 영업점 방문을 포함하는 joinWay: boostPlanIds = 해당 플랜들의 planId
                   → 스마트폰·인터넷 등 비대면을 포함하는 joinWay: boostPlanIds = 해당 플랜들의 planId

                3. 선호 은행: BANK_LOAN 플랜이 2개 이상 은행에 걸쳐 있으면
                   → "특별히 선호하는 은행이 있으신가요?" 질문 생성
                   → 각 은행 선택지: boostPlanIds = 해당 bankName의 플랜들의 planId

                ## 절대 금지

                - 금리 수준(낮은 금리 vs 높은 금리) 질문 금지. 금리는 이미 점수에 반영됩니다.
                - 모든 플랜이 같은 값을 가진 항목은 질문하지 마세요.

                ## 출력 규칙

                - 모든 질문과 선택지는 한국어로 작성하세요.
                - 모든 질문에 반드시 "상관없음" 선택지를 마지막에 추가하고 boostPlanIds는 빈 배열로 두세요.
                - boostPlanIds에는 입력받은 planId 값을 정확히 그대로 사용하세요.
                - 차이가 없으면 빈 배열을 반환하세요.
                """;
    }

    private List<PreferenceQuestion> parseResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        // OpenAI 응답: choices[0].message.tool_calls[0].function.arguments (JSON 문자열)
        JsonNode toolCalls = root.path("choices").path(0).path("message").path("tool_calls");
        for (JsonNode toolCall : toolCalls) {
            if (TOOL_NAME.equals(toolCall.path("function").path("name").asText())) {
                String argumentsJson = toolCall.path("function").path("arguments").asText();
                JsonNode arguments = objectMapper.readTree(argumentsJson);
                return parseQuestions(arguments.path("questions"));
            }
        }
        log.warn("GPT 응답에서 {} 함수 호출 결과를 찾지 못함", TOOL_NAME);
        return List.of();
    }

    private List<PreferenceQuestion> parseQuestions(JsonNode questionsNode) {
        List<PreferenceQuestion> result = new ArrayList<>();
        for (JsonNode q : questionsNode) {
            List<PreferenceOption> options = parseOptions(q.path("options"));
            if (options.size() >= 2) {
                result.add(new PreferenceQuestion(
                        q.path("questionId").asText(),
                        q.path("text").asText(),
                        options
                ));
            }
        }
        return result;
    }

    private List<PreferenceOption> parseOptions(JsonNode optionsNode) {
        List<PreferenceOption> options = new ArrayList<>();
        for (JsonNode o : optionsNode) {
            List<String> boostPlanIds = new ArrayList<>();
            for (JsonNode id : o.path("boostPlanIds")) {
                boostPlanIds.add(id.asText());
            }
            options.add(new PreferenceOption(o.path("label").asText(), boostPlanIds));
        }
        return options;
    }
}
