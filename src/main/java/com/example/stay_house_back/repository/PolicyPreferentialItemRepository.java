package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.PolicyPreferentialItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PolicyPreferentialItemRepository extends JpaRepository<PolicyPreferentialItem, Long> {
    List<PolicyPreferentialItem> findByPolicyIdIn(Collection<Long> policyIds);
}
