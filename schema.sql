-- 버틸집 — 스키마 정의
--
-- 데이터 파일(fss.db)은 리포에 올리지 않는다. 개인정보가 들어갈 수 있는
-- user_profile / house 테이블이 있고, git 히스토리에 한 번 들어간 값은
-- 실질적으로 지워지지 않는다. 데이터는 팀 내부에서 파일로 공유한다.
--
-- 빈 DB 만들기:
--   sqlite3 fss.db < schema.sql
-- 채우기 (FSS API 키 필요):
--   python3 fss_ingest.py && python3 fund_ingest.py
--   python3 policy_seed.py && python3 eligibility.py --load
--
-- 생성 시점 기준 행 수:
--   credit_loan_product            44
--   credit_loan_rate_option       139
--   eligibility_condition          27
--   financial_institution          18
--   house                           0
--   loan_product                   41
--   loan_product_rate_option       80
--   policy                          9
--   policy_preferential_rate        5
--   policy_rate_matrix             83
--   policy_rate_rule                5
--   product_exclusion               0
--   user_profile                    0

PRAGMA foreign_keys = ON;   -- SQLite 는 기본 OFF. 켜지 않으면 FK 가 무의미하다

CREATE TABLE credit_loan_product (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    dcls_month        TEXT NOT NULL,
    fin_co_no         TEXT NOT NULL,
    fin_prdt_cd       TEXT NOT NULL,
    crdt_prdt_type    TEXT,
    crdt_prdt_type_nm TEXT,   -- 일반신용대출 / 마이너스한도대출 / 카드론
    kor_co_nm         TEXT,
    fin_prdt_nm       TEXT,
    join_way          TEXT,
    cb_name           TEXT,
    dcls_strt_day     TEXT,
    ingested_at       TEXT, bank_nm TEXT, extraction_status TEXT DEFAULT 'RAW_ONLY',
    UNIQUE (dcls_month, fin_co_no, fin_prdt_cd, crdt_prdt_type)
);

CREATE TABLE credit_loan_rate_option (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    dcls_month             TEXT NOT NULL,
    fin_co_no              TEXT NOT NULL,
    fin_prdt_cd            TEXT NOT NULL,
    crdt_prdt_type         TEXT,
    crdt_lend_rate_type    TEXT,  -- A:대출금리 B:기준금리 C:가산금리 D:가감조정금리
    crdt_lend_rate_type_nm TEXT,
    grade_900_over         REAL,  -- crdt_grad_1
    grade_801_900          REAL,  -- crdt_grad_4
    grade_701_800          REAL,  -- crdt_grad_5
    grade_601_700          REAL,  -- crdt_grad_6
    grade_501_600          REAL,  -- crdt_grad_10
    grade_401_500          REAL,  -- crdt_grad_11
    grade_301_400          REAL,  -- crdt_grad_12
    grade_300_under        REAL,  -- crdt_grad_13
    grade_avg              REAL, credit_loan_product_id INTEGER,
    UNIQUE (dcls_month, fin_co_no, fin_prdt_cd, crdt_prdt_type, crdt_lend_rate_type)
);

CREATE TABLE eligibility_condition (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    loan_product_id        INTEGER REFERENCES loan_product(id) ON DELETE CASCADE,
    policy_id              INTEGER REFERENCES policy(id) ON DELETE CASCADE,
    credit_loan_product_id INTEGER REFERENCES credit_loan_product(id) ON DELETE CASCADE,

    age_min             INTEGER,   -- 만 나이 하한
    age_max             INTEGER,   -- 만 나이 상한
    income_min          INTEGER,   -- 최소 소득 요건(원). 있는 상품이 있다
    income_max          INTEGER,   -- 부부합산 연소득 상한(원)
    -- 순자산가액 상한(원). 청년·기금 제도에는 거의 항상 붙는다 (3.45억 등).
    -- 소득만 보면 자산 많은 사람이 통과하므로 별도 축이다.
    asset_limit         INTEGER,
    -- 받아 주는 소득 유형의 **집합**이다. 한 상품이 급여소득자와 사업소득자를
    -- 둘 다 받는 경우가 흔해서 단일값 CHECK 로는 담기지 않는다.
    -- 쉼표로 이어 둔다 ('급여소득자,사업소득자'). NULL = 제한 없음.
    employment_type     TEXT,
    employment_months   INTEGER,   -- 최소 재직기간(개월)
    house_owner_type    TEXT,      -- 무주택 / 무주택또는1주택
    -- 대상 임차보증금 상한. 거의 모든 상품이 '수도권 7억 / 그 외 5억' 처럼
    -- 이원화돼 있어 단일값으로는 못 담는다. 한쪽만 있는 상품은 다른 쪽을 NULL 로 둔다.
    deposit_limit_metro INTEGER,   -- 수도권
    deposit_limit_other INTEGER,   -- 그 외 지역
    monthly_rent_limit  INTEGER,   -- 대상 월세 상한(원). 월세 계열 상품용
    area_limit          REAL,      -- 전용면적 상한(m2)
    -- 그 제도의 존재 이유가 되는 생애사건. NONE / NEWBORN / NEWLYWED / JEONSE_VICTIM.
    -- 모름을 통과로 처리하면 전세피해를 안 겪은 사람에게 전세피해 대출이 뜬다.
    -- 정책 9건은 요건이 없어도 NONE 을 채운다 — 비워 두면 '요건 없음'과
    -- '미입력'이 구별되지 않는다. 은행 전세 상품은 이 축이 없어 NULL 이다.
    required_life_event TEXT,
    -- 보증금 대비 대출 비율. 부족분 = 보증금 - min(max_amount, 보증금 x ltv_ratio)
    -- loan_lmt 는 금액만 적혀 있어 파싱으로 못 뽑는다. 자격조건 페이지에만 있다.
    ltv_ratio           REAL,
    loan_term_month     INTEGER,   -- 최초 대출기간(개월)
    extension_max       INTEGER,   -- 연장 최대 횟수
    other_loan_allowed  INTEGER,   -- 타 전세대출 보유 시 취급 가능(1/0)
    contract_paid_ratio REAL,      -- 계약금 최소 지불 비율(0.05 = 5%)
    guarantee_agency    TEXT,      -- 전세 HF/HUG/SGI · 신용 서민금융진흥원 등
    guarantee_fee_rate  REAL,      -- 연 보증료율(%). 단일 숫자로 뭉갤 수 있을 때만
    -- 보증료율은 한 숫자가 아닌 경우가 많다. 특약보증과 반환보증의 요율·부담 주체가
    -- 다르고, 보증금 구간이나 신용도에 따라 갈리기도 한다. 숫자 한 칸으로 뭉개면
    -- 월 부담이 틀리므로 **원문을 그대로** 여기 둔다 (D8 과 같은 취지).
    guarantee_fee_rate_text TEXT,

    -- 신용대출 전용. KCB 와 NICE 는 독립 산정이라 서로 환산되지 않는다.
    -- 상품이 어느 CB 를 쓰는지는 credit_loan_product.cb_name 에 있다.
    credit_score_kcb_min  INTEGER,
    credit_score_nice_min INTEGER,
    max_amount            INTEGER, -- 대출 한도(원). 전세는 loan_product 에 있지만
                                   -- credit_loan_product 에는 없어 여기서 받는다

    raw_condition_text  TEXT,      -- 원문 그대로. 파싱이 틀려도 복구 가능하게
    source_url          TEXT,
    checked_by          TEXT,
    checked_at          TEXT, no_deposit_limit INTEGER,

    -- 은행 전세 / 정책 / 은행 신용 중 정확히 하나만 참조
    CHECK ((loan_product_id        IS NOT NULL) +
           (policy_id              IS NOT NULL) +
           (credit_loan_product_id IS NOT NULL) = 1)
);

CREATE TABLE financial_institution (
    fin_co_no   TEXT PRIMARY KEY,
    kor_co_nm   TEXT,
    homp_url    TEXT,
    cal_tel     TEXT,
    raw_json    TEXT
);

CREATE TABLE house (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    address           TEXT,
    deposit           INTEGER NOT NULL,
    monthly_rent      INTEGER DEFAULT 0,   -- 0 = 순수 전세
    maintenance_fee   INTEGER DEFAULT 0,   -- 관리비. 월 현금흐름에서 작지 않다
    area_sqm          REAL,
    appraisal_value   INTEGER,
    region            TEXT,
    area_source       TEXT DEFAULT 'MANUAL'
, house_type TEXT);

CREATE TABLE loan_product (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    dcls_month        TEXT NOT NULL,
    fin_co_no         TEXT NOT NULL,
    fin_prdt_cd       TEXT NOT NULL,
    kor_co_nm         TEXT,
    bank_nm           TEXT,   -- 정규화된 은행명 (필터/조인용)
    fin_prdt_nm       TEXT,
    join_way          TEXT,
    loan_inci_expn    TEXT,   -- 부대비용 원문 (보증료율이 여기 들어있다)
    erly_rpay_fee     TEXT,   -- 중도상환수수료 원문
    dly_rate          TEXT,   -- 연체이자율 원문
    loan_lmt          TEXT,   -- 한도 원문
    -- 아래는 파생 필드 (파싱 결과)
    max_amount        INTEGER,
    ltv_ratio         REAL,
    product_class     TEXT,   -- FUND / BANK
    is_youth          INTEGER DEFAULT 0,  -- 청년 대상 여부 (재원 구분과 독립)
    guarantee_agency  TEXT,   -- HF / HUG / SGI (추정)
    extraction_status TEXT DEFAULT 'RAW_ONLY',
    dcls_strt_day     TEXT,
    dcls_end_day      TEXT,
    ingested_at       TEXT, region_limit TEXT, region_source TEXT, housing_target TEXT DEFAULT 'JEONSE',
    UNIQUE (dcls_month, fin_co_no, fin_prdt_cd)
);

CREATE TABLE loan_product_rate_option (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    dcls_month        TEXT NOT NULL,
    fin_co_no         TEXT NOT NULL,
    fin_prdt_cd       TEXT NOT NULL,
    rpay_type         TEXT,
    rpay_type_nm      TEXT,   -- 만기일시상환 / 분할상환
    lend_rate_type    TEXT,
    lend_rate_type_nm TEXT,   -- 고정금리 / 변동금리
    lend_rate_min     REAL,
    lend_rate_max     REAL,
    lend_rate_avg     REAL, loan_product_id INTEGER,
    UNIQUE (dcls_month, fin_co_no, fin_prdt_cd, rpay_type, lend_rate_type)
);

CREATE TABLE policy (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_nm           TEXT NOT NULL,
    category            TEXT NOT NULL
        CHECK (category IN ('POLICY_LOAN','RENT_SUBSIDY','GUARANTEE_FEE_REFUND')),
    -- 기금 포털 상품코드(FP05020101). 재수집 upsert 키.
    -- id 는 내부 FK 전용이라 외부 페이지와 대조할 수단이 따로 있어야 한다.
    source_code         TEXT UNIQUE,
    operating_agency    TEXT,
    source_url          TEXT,
    target_text         TEXT,               -- 대출대상 원문
    raw_path            TEXT,

    -- 요약 원문. 파싱 결과가 틀려도 원문이 있으면 복구할 수 있다 (D7·D8).
    rate_text           TEXT,               -- "연 2.5%~3.5%"
    limit_text          TEXT,
    term_text           TEXT,

    -- POLICY_LOAN 에서만 채움 (표시용. 실제 계산은 policy_rate_matrix)
    loan_lmt_min        INTEGER,
    loan_lmt_max        INTEGER,
    rate_min            REAL,
    rate_max            REAL,
    guarantee_agency    TEXT,

    -- RENT_SUBSIDY 에서만 채움
    monthly_amount      INTEGER,
    max_duration_months INTEGER,

    -- GUARANTEE_FEE_REFUND 에서만 채움
    refund_rate         REAL,
    refund_cap          INTEGER,

    apply_start_date    TEXT,
    apply_end_date      TEXT,
    budget_status       TEXT DEFAULT 'OPEN'
        CHECK (budget_status IN ('OPEN','CLOSING_SOON','EXHAUSTED')),

    extraction_status   TEXT DEFAULT 'RAW_ONLY',   -- FULL / PARTIAL / RAW_ONLY (D5)
    dcls_month          TEXT,
    fetched_at          TEXT,
    source_type         TEXT DEFAULT 'MANUAL'
, monthly_limit INTEGER, loan_lmt_metro INTEGER, loan_lmt_other INTEGER);

CREATE TABLE policy_preferential_rate (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_id         INTEGER NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
    raw_text          TEXT NOT NULL,      -- 우대금리 문단 원문. 파싱은 하지 않는다.
    UNIQUE (policy_id, raw_text)
);

CREATE TABLE policy_rate_matrix (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_id         INTEGER NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
    income_min        INTEGER,            -- 부부합산 연소득 하한(원). NULL = 무제한
    income_max        INTEGER,
    deposit_min       INTEGER,            -- 임차보증금 하한(원)
    deposit_max       INTEGER,
    rate              REAL NOT NULL,      -- 기본금리(%)
    income_type       TEXT,               -- '맞벌이' = 맞벌이 가구 전용 구간.
                                          -- NULL = 가구 유형 무관 (제한 없음)
    income_label      TEXT,               -- 원문 라벨 (파싱 검증용)
    deposit_label     TEXT,
    source            TEXT DEFAULT 'auto',-- auto = 정적 HTML / manual = 사람 입력
    checked_by        TEXT,
    checked_at        TEXT
);

CREATE TABLE policy_rate_rule (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    policy_id      INTEGER NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
    -- 어느 자금에 붙는 금리인가. 청년전용 보증부월세는 보증금분과 월세금분의
    -- 금리가 아예 다르다 (1.3% vs 0%/1.0%).
    applies_to     TEXT NOT NULL CHECK (applies_to IN ('DEPOSIT','RENT','ALL')),
    tier_label     TEXT,          -- '우대형' / '일반형' / '월 20만원 초과' 등
    amount_min     INTEGER,       -- 구간이 있으면. 월세금 20만원 초과 -> 200000
    amount_max     INTEGER,
    rate           REAL NOT NULL,
    condition_text TEXT,          -- 원문. 파싱이 틀려도 대조할 수 있게 (D7)
    source         TEXT DEFAULT 'auto',
    checked_by     TEXT,
    checked_at     TEXT,
    UNIQUE (policy_id, applies_to, tier_label)
);

CREATE TABLE product_exclusion (
    product_type          TEXT NOT NULL CHECK (product_type IN ('BANK_LOAN','POLICY')),
    product_id            INTEGER NOT NULL,
    excluded_product_type TEXT NOT NULL CHECK (excluded_product_type IN ('BANK_LOAN','POLICY')),
    excluded_product_id   INTEGER NOT NULL,
    PRIMARY KEY (product_type, product_id, excluded_product_type, excluded_product_id)
);

CREATE TABLE user_profile (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    credit_score_kcb  INTEGER CHECK (credit_score_kcb  BETWEEN 0 AND 1000),
    credit_score_nice INTEGER CHECK (credit_score_nice BETWEEN 0 AND 1000),
    updated_at        TEXT
, birth_year INTEGER, annual_income INTEGER, net_asset INTEGER, is_dual_income INTEGER, house_count INTEGER, is_household_head INTEGER, marriage_status TEXT, has_newborn INTEGER, own_capital INTEGER, monthly_income_net INTEGER, monthly_fixed_cost INTEGER, monthly_living_cost INTEGER, savings_goal INTEGER, employment_type TEXT, employment_months INTEGER, region TEXT);

CREATE INDEX idx_loan_bank ON loan_product(bank_nm);

CREATE INDEX idx_loan_class ON loan_product(product_class);

CREATE INDEX idx_loan_co ON loan_product(fin_co_no);

CREATE INDEX idx_loan_youth ON loan_product(is_youth);

CREATE UNIQUE INDEX ux_elig_credit
    ON eligibility_condition(credit_loan_product_id)
    WHERE credit_loan_product_id IS NOT NULL;

CREATE UNIQUE INDEX ux_elig_loan
    ON eligibility_condition(loan_product_id) WHERE loan_product_id IS NOT NULL;

CREATE UNIQUE INDEX ux_elig_policy
    ON eligibility_condition(policy_id) WHERE policy_id IS NOT NULL;

CREATE UNIQUE INDEX ux_prm_cell
    ON policy_rate_matrix (policy_id, income_label, deposit_label,
                           COALESCE(income_type, ''));
