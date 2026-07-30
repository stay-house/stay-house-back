package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.ProductExclusion;
import com.example.stay_house_back.entity.ProductExclusionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 이미 참조되고 있으나 리포에 없어 컴파일이 안 되던 파일이다.
 *
 * <p>product_exclusion 은 현재 0행이라 PlanningEngineService 의 배타 제거는
 * 아무것도 걸러내지 않는다.
 */
@Repository
public interface ProductExclusionRepository
        extends JpaRepository<ProductExclusion, ProductExclusionId> {
}
