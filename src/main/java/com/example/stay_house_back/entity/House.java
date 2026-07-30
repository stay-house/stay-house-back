package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "house")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address")
    private String address;

    @Column(name = "deposit", nullable = false)
    private Integer deposit;

    @Column(name = "monthly_rent")
    @Builder.Default
    private Integer monthlyRent = 0;

    @Column(name = "maintenance_fee")
    @Builder.Default
    private Integer maintenanceFee = 0;

    @Column(name = "area_sqm")
    private Double areaSqm;

    @Column(name = "appraisal_value")
    private Integer appraisalValue;

    @Column(name = "region")
    private String region;

    @Column(name = "area_source", length = 20)
    private String areaSource = "MANUAL";

    @Column(name = "house_type")
    private String houseType;
}
