package com.example.stay_house_back.entity;

import com.example.stay_house_back.entity.enums.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductExclusionId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "excluded_product_type", nullable = false, length = 20)
    private ProductType excludedProductType;

    @Column(name = "excluded_product_id", nullable = false)
    private Long excludedProductId;
}
