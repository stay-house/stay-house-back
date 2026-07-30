package com.example.stay_house_back.dto.plan;

import com.example.stay_house_back.dto.eligibility.EligibleLoanProductDto;
import com.example.stay_house_back.dto.eligibility.EligiblePolicyDto;
import com.example.stay_house_back.entity.enums.ProductType;
import lombok.Getter;

/**
 * 대출 방법 (택1 축).
 * SELF_FUNDED / BANK_LOAN / POLICY_LOAN 세 종류.
 * product_exclusion 체크를 위해 productType + productId 노출.
 */
@Getter
public class FundingSource {

    private final FundingSourceType type;
    private final EligibleLoanProductDto bankLoan;   // BANK_LOAN일 때 non-null
    private final EligiblePolicyDto policyLoan;      // POLICY_LOAN일 때 non-null

    private FundingSource(FundingSourceType type,
                          EligibleLoanProductDto bankLoan,
                          EligiblePolicyDto policyLoan) {
        this.type = type;
        this.bankLoan = bankLoan;
        this.policyLoan = policyLoan;
    }

    public static FundingSource selfFunded() {
        return new FundingSource(FundingSourceType.SELF_FUNDED, null, null);
    }

    public static FundingSource ofBankLoan(EligibleLoanProductDto loan) {
        return new FundingSource(FundingSourceType.BANK_LOAN, loan, null);
    }

    public static FundingSource ofPolicyLoan(EligiblePolicyDto policy) {
        return new FundingSource(FundingSourceType.POLICY_LOAN, null, policy);
    }

    /** product_exclusion 테이블의 product_type 컬럼 값. SELF_FUNDED면 null. */
    public ProductType getProductType() {
        return switch (type) {
            case BANK_LOAN -> ProductType.BANK_LOAN;
            case POLICY_LOAN -> ProductType.POLICY;
            case SELF_FUNDED -> null;
        };
    }

    /** product_exclusion 테이블의 product_id 컬럼 값. SELF_FUNDED면 null. */
    public Long getProductId() {
        return switch (type) {
            case BANK_LOAN -> bankLoan.getId();
            case POLICY_LOAN -> policyLoan.getId();
            case SELF_FUNDED -> null;
        };
    }
}
