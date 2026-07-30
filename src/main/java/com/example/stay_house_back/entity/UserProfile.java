package com.example.stay_house_back.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profile")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credit_score_kcb")
    private Integer creditScoreKcb;

    @Column(name = "credit_score_nice")
    private Integer creditScoreNice;

    @Column(name = "updated_at")
    private String updatedAt;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "annual_income")
    private Integer annualIncome;

    @Column(name = "net_asset")
    private Integer netAsset;

    @Column(name = "is_dual_income")
    private Boolean isDualIncome;

    @Column(name = "house_count")
    private Integer houseCount;

    @Column(name = "is_household_head")
    private Boolean isHouseholdHead;

    @Column(name = "marriage_status", length = 20)
    private String marriageStatus;

    @Column(name = "has_newborn")
    private Boolean hasNewborn;

    @Column(name = "own_capital")
    private Integer ownCapital;

    @Column(name = "monthly_income_net")
    private Integer monthlyIncomeNet;

    @Column(name = "monthly_fixed_cost")
    private Integer monthlyFixedCost;

    @Column(name = "monthly_living_cost")
    private Integer monthlyLivingCost;

    @Column(name = "savings_goal")
    private Integer savingsGoal;

    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(name = "employment_months")
    private Integer employmentMonths;

    @Column(name = "region")
    private String region;
}
