package com.example.stay_house_back.repository;

import com.example.stay_house_back.entity.LoanProduct;
import com.example.stay_house_back.entity.LoanProductRateOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanProductRateOptionRepository extends JpaRepository<LoanProductRateOption, Long> {
    List<LoanProductRateOption> findByLoanProduct(LoanProduct loanProduct);
}
