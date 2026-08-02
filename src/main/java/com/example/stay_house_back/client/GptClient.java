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
                플랜 후보 목록을 분석해 사용자 선호를 파악할 질문을 최소 1개 최대 5개 생성하세요.
                각 질문의 답이 플랜 순위에 실질적으로 영향을 줘야 합니다.

                ## 무조건 확인할 차이 항목 (우선순위 순)

                1. 가입 방법: joinWay 값이 다른 플랜들이 있으면
                   → 가입 방법 선호도 질문 생성
                   → 영업점 방문을 포함하는 joinWay: boostPlanIds = 해당 플랜들의 planId
                   → 스마트폰·인터넷 등 비대면을 포함하는 joinWay: boostPlanIds = 해당 플랜들의 planId

                2. 선호 은행: BANK_LOAN 플랜이 2개 이상 은행에 걸쳐 있으면
                   → "특별히 선호하는 은행이 있으신가요?" 질문 생성
                   → 각 은행 선택지: boostPlanIds = 해당 bankName의 플랜들의 planId

                3. 위의 1번, 2번 조건이 아니어도 후보들 중 유의미한 차이를 발견해 질문을 생성하세요
                    - 의미 있는 차이의 예
                        - 변동금리 vs 고정금리 플랜이 섞여 있을 때 (variableRate 차이)
                        - 대출 기간(loanTermMonth)이 크게 다를 때
                        - 그 밖에 플랜 데이터에서 발견한 실질적 차이
                        
                ## 절대 금지

                - 금리 수준(낮은 금리 vs 높은 금리) 질문 금지. 금리는 이미 점수에 반영됩니다.
                - 상품 유형(정부 지원 대출 vs 은행 대출) 질문 금지 — 시스템이 규칙으로 별도 생성합니다.
                - 월세지원(rentSubsidyMonthlyAmount)은 대출 유형이 아닙니다. 이를 근거로
                  "정부 지원" 여부를 판단하거나 boostPlanIds 에 넣지 마세요.
                - 모든 플랜이 같은 값을 가진 항목은 질문하지 마세요.

                ## 출력 규칙

                - 모든 질문과 선택지는 한국어로 작성하세요.
                - 모든 질문에 반드시 "상관없음" 선택지를 마지막에 추가하고 boostPlanIds는 빈 배열로 두세요.
                - boostPlanIds에는 입력받은 planId 값을 정확히 그대로 사용하세요.
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

    /**
     * 플랜 설명 생성 — 계산은 하지 않고 주어진 숫자를 문장으로 엮는다.
     * 실패 시 예외를 던진다 — 호출자가 규칙 기반 폴백으로 전환한다.
     */
    public JsonNode generatePlanExplanation(Map<String, Object> planSummary) throws Exception {
        Map<String, Object> tool = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "submit_explanation",
                        "description", "플랜 설명을 반환합니다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "recommendReason", Map.of("type", "string",
                                                "description", "이 플랜을 추천하는 이유 2~3문장"),
                                        "cautions", Map.of("type", "array", "maxItems", 3,
                                                "items", Map.of("type", "string"),
                                                "description", "주의할 점 (변동금리·부족분·신청기간 등)"),
                                        "actionGuide", Map.of("type", "array", "maxItems", 4,
                                                "items", Map.of("type", "string"),
                                                "description", "다음 행동 단계 (신청처·서류 등)")
                                ),
                                "required", List.of("recommendReason", "cautions", "actionGuide")
                        )
                )
        );

        String system = """
                당신은 청년 주거 금융 전문가입니다. 제공된 플랜 데이터를 바탕으로 상세하고 실질적인 설명을 작성하세요.
                제공된 숫자 외에 새로운 수치를 계산하거나 만들지 마세요. 존댓말로 작성하세요.

                ## recommendReason — 5~7문장, 아래 순서로 작성하세요
                [플랜 설명]
                1. 금리와 대출금액: "연 {annualRate}% 금리로 {loanAmount/10000}만원을 조달합니다" 형식으로 시작
                2. 월 주거비: monthlyHousingCost 수치를 만원 단위로 언급 ("월 예상 주거비 약 XX만원")
                3. 부담률 평가: burdenRatio를 %로 환산해 명시적으로 평가
                   - 20% 미만: "월 소득 대비 주거비 부담이 XX%로 안정적인 수준입니다"
                   - 20~35%: "월 소득의 XX%를 주거비로 지출하는 수준으로 적정 범위입니다"
                   - 35% 이상: "월 소득의 XX%를 주거비로 지출해 부담이 높은 편입니다"
                4. variableRate=false(고정금리)이면: "고정금리라 금리 변동 리스크 없이 월 부담을 예측할 수 있습니다" 언급
                   variableRate=true(변동금리)이면: scenarios의 +2%p 시 월 부담 수치를 언급해 리스크 경고
                5. rentSubsidyName이 있으면: 월세지원 이름과 govRentSubsidyAmount를 만원 단위로 언급

                ## cautions — 해당하는 항목만, 최대 3개
                - variableRate=true: scenarios 배열에서 +1%p·+2%p 시 monthlyLoanRepayment 수치를 직접 인용해 경고
                  예: "금리가 2%p 오르면 월 대출 상환액이 약 XX만원으로 늘어납니다"
                - shortfall>0: "보증금 대비 대출 한도 부족분이 {shortfall/10000}만원입니다. 신용대출이나 추가 자기자금으로 메워야 합니다"
                - rentSubsidyBudgetStatus=EXHAUSTED: "포함된 {rentSubsidyName}은 올해 접수가 마감됐습니다. 내년 공고를 확인하세요"
                - burdenRatio≥0.35: "주거비 부담률이 높으므로 비상금 3개월치 이상 확보를 권장합니다"
                - 해당 없으면 빈 배열 반환 (억지로 채우지 마세요)

                ## actionGuide — 반드시 3~4개, 상품 유형별 맞춤 단계
                fundingType=POLICY_LOAN:
                  1. "주택도시기금 앱(기금e든든) 또는 취급은행(우리·국민·기업·농협·신한·하나은행) 영업점에서 신청하세요"
                  2. "전세계약서(확정일자 포함), 재직증명서, 건강보험료 납부확인서, 주민등록등본을 준비하세요"
                  3. "임대차계약 후 잔금일 전까지 대출 실행이 완료되어야 하므로 최소 2주 전에 신청하세요"
                  4. shortfall>0이면: "부족분 {shortfall/10000}만원은 신용대출 또는 추가 자기자금으로 마련해야 합니다"
                fundingType=BANK_LOAN:
                  1. "{bankName} 앱 또는 영업점에서 전세대출 신청서를 작성하세요"
                  2. "전세계약서(확정일자), 소득확인서류(근로소득원천징수영수증 또는 급여명세서), 주민등록등본을 준비하세요"
                  3. "보증기관(HF/HUG/SGI) 보증 심사가 포함되므로 잔금일 2~3주 전 신청을 권장합니다"
                  4. shortfall>0이면: "부족분 {shortfall/10000}만원은 신용대출 또는 추가 자기자금으로 마련해야 합니다"
                - 구체적 날짜·금리 수치를 지어내지 마세요. shortfall 계산도 하지 마세요.
                """;

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content",
                        "다음 플랜을 설명해주세요:\n\n" + objectMapper.writeValueAsString(planSummary))
        );

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", messages,
                "tools", List.of(tool),
                "tool_choice", Map.of("type", "function", "function", Map.of("name", "submit_explanation"))
        );

        String responseJson = restClient.post()
                .uri("/v1/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode toolCalls = root.path("choices").path(0).path("message").path("tool_calls");
        for (JsonNode toolCall : toolCalls) {
            if ("submit_explanation".equals(toolCall.path("function").path("name").asText())) {
                return objectMapper.readTree(toolCall.path("function").path("arguments").asText());
            }
        }
        throw new IllegalStateException("GPT 응답에 submit_explanation 호출이 없음");
    }
}
