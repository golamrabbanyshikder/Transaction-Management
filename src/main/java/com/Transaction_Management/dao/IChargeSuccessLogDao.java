package com.Transaction_Management.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Transaction_Management.model.ChargeSuccessLog;
@Repository
public interface IChargeSuccessLogDao extends JpaRepository<ChargeSuccessLog, Long> {

}
