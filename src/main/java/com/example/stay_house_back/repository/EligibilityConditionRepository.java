package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.EligibilityCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityConditionRepository extends JpaRepository<EligibilityCondition, Long> {
    List<EligibilityCondition> findByLoanProductIsNotNull();
    List<EligibilityCondition> findByPolicyIsNotNull();
    java.util.Optional<EligibilityCondition> findByLoanProductId(Long loanProductId);
}
