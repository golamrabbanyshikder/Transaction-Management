package com.Transaction_Management.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.Transaction_Management.dao.IChargeConfigDao;
import com.Transaction_Management.dao.IChargeFailureLogDao;
import com.Transaction_Management.dao.IChargeSuccessLogDao;
import com.Transaction_Management.dao.IInboxDao;
import com.Transaction_Management.dao.IKeywordDetailsDao;
import com.Transaction_Management.dto.ChargeCodeRequestDto;
import com.Transaction_Management.dto.ChargeCodeResponseDto;
import com.Transaction_Management.dto.UnlockCodeRequestDto;
import com.Transaction_Management.dto.UnlockCodeResponseDto;
import com.Transaction_Management.model.ChargeConfig;
import com.Transaction_Management.model.ChargeFailureLog;
import com.Transaction_Management.model.ChargeSuccessLog;
import com.Transaction_Management.model.InboxModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ContentRetrievalServiceImpl implements IcontentRetrievalService {

	@Value("${base.url}")
	private String baseUrl;

	@Value("${unlockCode.api.url}")
	private String unlockCodeApiUrl;

	@Value("${charging.api.url}")
	private String chargingApiUrl;

	@Autowired
	IInboxDao inboxDao;

	@Autowired
	IKeywordDetailsDao keywordDetailsDao;

	@Autowired
	IChargeConfigDao chargeConfigDao;

	@Autowired
	IChargeSuccessLogDao chargeSuccessLogDao;

	@Autowired
	IChargeFailureLogDao chargeFailureLogDao;

	private final RestTemplate restTemplate = new RestTemplate();

	List<InboxModel> contentForUpdate = new ArrayList<>();
	List<ChargeSuccessLog> chargeSuccessLogs = new ArrayList<>();
	List<ChargeFailureLog> chargeFailureLogs = new ArrayList<>();

	@Override
	public void getContentAndCharge() {
		try {

			List<InboxModel> content = inboxDao.findByStatus("N");
			for (InboxModel inboxModel : content) {

				if (keywordDetailsDao.existsByKeyword(inboxModel.getKeyword())) {
					UnlockCodeRequestDto unlockCodeRequestDto = new UnlockCodeRequestDto();
					BeanUtils.copyProperties(inboxModel, unlockCodeRequestDto, UnlockCodeRequestDto.class);

					if (getUnlockCode(unlockCodeRequestDto) != null) {

						ChargeConfig chargeConfig = chargeConfigDao.findByOperator(inboxModel.getOperator());
						String getChargeCode = chargeConfig.getChargeCode();

						if (getChargeCode != null) {
							ChargeCodeRequestDto chargeCodeRequestDto = new ChargeCodeRequestDto();
							chargeCodeRequestDto.setChargeCode(getChargeCode);
							BeanUtils.copyProperties(inboxModel, chargeCodeRequestDto, ChargeCodeRequestDto.class);
							getCharging(chargeCodeRequestDto, inboxModel);

						}
					}

				}

			}

			chargeSuccessLogDao.saveAll(chargeSuccessLogs);
			chargeFailureLogDao.saveAll(chargeFailureLogs);
			inboxDao.saveAll(contentForUpdate);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getUnlockCode(UnlockCodeRequestDto unlockCodeRequestDto) {
		try {
			HttpEntity<UnlockCodeRequestDto> entity = new HttpEntity<>(unlockCodeRequestDto);
			ResponseEntity<UnlockCodeResponseDto> response = restTemplate.exchange(baseUrl + unlockCodeApiUrl,
					HttpMethod.POST, entity, UnlockCodeResponseDto.class);

			if (response != null && response.getStatusCodeValue() == 200) {
				UnlockCodeResponseDto apiResponse = response.getBody();
				return apiResponse.getUnlockCode();
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public void getCharging(ChargeCodeRequestDto chargeCodeRequestDto, InboxModel inboxModel) {
		try {
			HttpEntity<ChargeCodeRequestDto> entity = new HttpEntity<>(chargeCodeRequestDto);
			ResponseEntity<ChargeCodeResponseDto> response = restTemplate.exchange(baseUrl + chargingApiUrl,
					HttpMethod.POST, entity, ChargeCodeResponseDto.class);

			if (response != null && response.getStatusCodeValue() == 200) {
				ChargeCodeResponseDto allContent = response.getBody();

				ChargeSuccessLog chargeSuccessLog = new ChargeSuccessLog();
				chargeSuccessLog.setSmsId(inboxModel.getId());
				chargeSuccessLog.setKeyword(inboxModel.getKeyword());
				chargeSuccessLog.setGameName(inboxModel.getGameName());
				chargeSuccessLog.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
				BeanUtils.copyProperties(allContent, chargeSuccessLog, ChargeSuccessLog.class);
				
				chargeSuccessLogs.add(chargeSuccessLog);

				inboxModel.setStatus("S");
				inboxModel.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
				contentForUpdate.add(inboxModel);
			} else {
				ChargeFailureLog chargeFailureLog = new ChargeFailureLog();
				chargeFailureLog.setSmsId(inboxModel.getId());
				chargeFailureLog.setKeyword(inboxModel.getKeyword());
				chargeFailureLog.setGameName(inboxModel.getGameName());
				chargeFailureLog.setStatusCode(response.getStatusCodeValue());
				chargeFailureLog.setMessage(response.getBody().getMessage());
				chargeFailureLog.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
				BeanUtils.copyProperties(chargeCodeRequestDto, chargeFailureLog, ChargeFailureLog.class);
				chargeFailureLogs.add(chargeFailureLog);

				inboxModel.setStatus("F");
				inboxModel.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
				contentForUpdate.add(inboxModel);
			}

		} catch (HttpServerErrorException e) {
			String responseBody = e.getResponseBodyAsString();
			ObjectMapper objectMapper = new ObjectMapper(); // Jackson library
			try {
				JsonNode jsonNode = objectMapper.readTree(responseBody);
				int statusCode = jsonNode.get("statusCode").asInt();
				String message = jsonNode.get("message").asText();

				ChargeFailureLog chargeFailureLog = new ChargeFailureLog();
				chargeFailureLog.setSmsId(inboxModel.getId());
				chargeFailureLog.setKeyword(inboxModel.getKeyword());
				chargeFailureLog.setGameName(inboxModel.getGameName());
				chargeFailureLog.setStatusCode(statusCode);
				chargeFailureLog.setMessage(message);
				chargeFailureLog.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
				BeanUtils.copyProperties(chargeCodeRequestDto, chargeFailureLog, ChargeFailureLog.class);
				chargeFailureLogs.add(chargeFailureLog);

				inboxModel.setStatus("F");
				inboxModel.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
				contentForUpdate.add(inboxModel);
			} catch (JsonProcessingException ex) {
				ex.printStackTrace();
			}
		}

	}

}
