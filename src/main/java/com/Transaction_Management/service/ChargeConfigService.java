package com.Transaction_Management.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.Transaction_Management.dao.IChargeConfigDao;

@Service
public class ChargeConfigService {

	@Autowired
	IChargeConfigDao chargeConfigDao;

	private Map<String, String> getChargeCodes = new HashMap<>();

	@Scheduled(fixedRate = 60000) // Refresh every 60 seconds
	public void refreshCache() {
		getChargeCodes.clear();
		chargeConfigDao.findAll()
				.forEach(chargeConfig -> getChargeCodes.put(chargeConfig.getOperator(), chargeConfig.getChargeCode()));

	}

	public String getChargeCodes(String operator) {
		return getChargeCodes.get(operator);
	}

}
