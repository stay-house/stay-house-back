package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.PolicyPreferentialCap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyPreferentialCapRepository extends JpaRepository<PolicyPreferentialCap, Long> {
    List<PolicyPreferentialCap> findByPolicyIdOrderByPriorityAsc(Long policyId);
}
