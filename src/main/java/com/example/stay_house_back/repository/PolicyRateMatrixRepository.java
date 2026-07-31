package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.PolicyRateMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRateMatrixRepository extends JpaRepository<PolicyRateMatrix, Long> {

    // 정책 금리 격자에서 연소득·보증금 구간에 맞는 금리 행 조회
    @Query("SELECT m FROM PolicyRateMatrix m " +
           "WHERE m.policy.id = :policyId " +
           "AND (m.incomeMin IS NULL OR m.incomeMin <= :income) " +
           "AND (m.incomeMax IS NULL OR m.incomeMax >= :income) " +
           "AND (m.depositMin IS NULL OR m.depositMin <= :deposit) " +
           "AND (m.depositMax IS NULL OR m.depositMax >= :deposit) " +
           "AND m.incomeType IS NULL")
    List<PolicyRateMatrix> findMatchingRates(@Param("policyId") Long policyId,
                                             @Param("income") int income,
                                             @Param("deposit") int deposit);
}
