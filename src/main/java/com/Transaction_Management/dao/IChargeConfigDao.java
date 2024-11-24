package com.Transaction_Management.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Transaction_Management.model.ChargeConfig;
@Repository
public interface IChargeConfigDao extends JpaRepository<ChargeConfig, Long> {

	ChargeConfig findByOperator(String operator);
	
}
