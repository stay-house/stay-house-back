package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.PolicyRateRule;
import com.example.stay_house_back.entity.enums.PolicyRuleAppliesTo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRateRuleRepository extends JpaRepository<PolicyRateRule, Long> {

    // 월세 금액 구간에 맞는 금리 행 조회 (청년전용 보증부월세대출, 주거안정월세대출 등)
    @Query("SELECT r FROM PolicyRateRule r " +
           "WHERE r.policy.id = :policyId " +
           "AND r.appliesTo = :appliesTo " +
           "AND (r.amountMin IS NULL OR r.amountMin <= :amount) " +
           "AND (r.amountMax IS NULL OR r.amountMax >= :amount)")
    List<PolicyRateRule> findMatchingRules(@Param("policyId") Long policyId,
                                           @Param("appliesTo") PolicyRuleAppliesTo appliesTo,
                                           @Param("amount") int amount);
}
