package com.Transaction_Management.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.Transaction_Management.model.ChargeFailureLog;
import com.Transaction_Management.model.ChargeSuccessLog;
import com.Transaction_Management.model.InboxModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("deprecation")
@Service
public class ContentRetrievalServiceImpl implements IContentRetrievalService {

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

	@Autowired
	KeywordDetailsService keywordDetailsService;

	@Autowired
	ChargeConfigService chargeConfigService;

	private final RestTemplate restTemplate = new RestTemplate();

	private final ObjectMapper objectMapper = new ObjectMapper();

	List<InboxModel> contentForUpdate = new ArrayList<>();
	List<ChargeSuccessLog> chargeSuccessLogs = new ArrayList<>();
	List<ChargeFailureLog> chargeFailureLogs = new ArrayList<>();

	@Override
	public void getContentAndCharge() {
		try {

			List<InboxModel> content = inboxDao.findByStatus("N");
			if (content.isEmpty()) {
				return;
			}
			try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
				List<Runnable> tasks = content.stream().map(this::createTask).toList();
				tasks.forEach(executor::execute);
			}
			
			if (chargeSuccessLogs != null) {
				chargeSuccessLogDao.saveAll(chargeSuccessLogs);

			}
			if (chargeFailureLogs != null) {
				chargeFailureLogDao.saveAll(chargeFailureLogs);
			}
			if (contentForUpdate != null) {
				inboxDao.saveAll(contentForUpdate);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private Runnable createTask(InboxModel inboxModel) {
		return () -> {
			try {
				if (keywordDetailsService.isValidKeyword(inboxModel.getKeyword())) {
					UnlockCodeRequestDto unlockCodeRequestDto = new UnlockCodeRequestDto();
					BeanUtils.copyProperties(inboxModel, unlockCodeRequestDto);

					String unlockCode = getUnlockCode(unlockCodeRequestDto);
					if (unlockCode != null) {
						String chargeCode = chargeConfigService.getChargeCodes(inboxModel.getOperator());

						if (chargeCode != null) {
							ChargeCodeRequestDto chargeCodeRequestDto = new ChargeCodeRequestDto();
							chargeCodeRequestDto.setChargeCode(chargeCode);
							BeanUtils.copyProperties(inboxModel, chargeCodeRequestDto);

							getCharging(chargeCodeRequestDto, inboxModel);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}

	public String getUnlockCode(UnlockCodeRequestDto unlockCodeRequestDto) {
		try {
			HttpEntity<UnlockCodeRequestDto> entity = new HttpEntity<>(unlockCodeRequestDto);
			ResponseEntity<UnlockCodeResponseDto> response = restTemplate.exchange(baseUrl + unlockCodeApiUrl,
					HttpMethod.POST, entity, UnlockCodeResponseDto.class);

			if (response != null && response.getStatusCodeValue() == 200) {
				return response.getBody().getUnlockCode();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void getCharging(ChargeCodeRequestDto chargeCodeRequestDto, InboxModel inboxModel) {
		try {
			HttpEntity<ChargeCodeRequestDto> entity = new HttpEntity<>(chargeCodeRequestDto);
			ResponseEntity<ChargeCodeResponseDto> response = restTemplate.exchange(baseUrl + chargingApiUrl,
					HttpMethod.POST, entity, ChargeCodeResponseDto.class);

			if (response != null && response.getStatusCodeValue() == 200) {
				handleSuccess(response.getBody(), inboxModel);
			} else {
				handleFailure(response.getBody(), response.getStatusCodeValue(), inboxModel, chargeCodeRequestDto);
			}
		} catch (HttpServerErrorException e) {
			handleServerError(e, inboxModel, chargeCodeRequestDto);
		}
	}

	private void handleSuccess(ChargeCodeResponseDto response, InboxModel inboxModel) {
		ChargeSuccessLog successLog = new ChargeSuccessLog();
		successLog.setSmsId(inboxModel.getId());
		successLog.setKeyword(inboxModel.getKeyword());
		successLog.setGameName(inboxModel.getGameName());
		successLog.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		BeanUtils.copyProperties(response, successLog);

		chargeSuccessLogs.add(successLog);

		inboxModel.setStatus("S");
		inboxModel.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
		contentForUpdate.add(inboxModel);
	}

	private void handleFailure(ChargeCodeResponseDto response, int statusCode, InboxModel inboxModel,
			ChargeCodeRequestDto chargeCodeRequestDto) {
		ChargeFailureLog failureLog = new ChargeFailureLog();
		failureLog.setSmsId(inboxModel.getId());
		failureLog.setKeyword(inboxModel.getKeyword());
		failureLog.setGameName(inboxModel.getGameName());
		failureLog.setStatusCode(statusCode);
		failureLog.setMessage(response != null ? response.getMessage() : "Unknown error");
		failureLog.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
		BeanUtils.copyProperties(chargeCodeRequestDto, failureLog);

		chargeFailureLogs.add(failureLog);

		inboxModel.setStatus("F");
		inboxModel.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
		contentForUpdate.add(inboxModel);
	}

	private void handleServerError(HttpServerErrorException e, InboxModel inboxModel,
			ChargeCodeRequestDto chargeCodeRequestDto) {
		try {
			JsonNode jsonNode = objectMapper.readTree(e.getResponseBodyAsString());
			int statusCode = jsonNode.get("statusCode").asInt();
			String message = jsonNode.get("message").asText();

			ChargeFailureLog failureLog = new ChargeFailureLog();
			failureLog.setSmsId(inboxModel.getId());
			failureLog.setKeyword(inboxModel.getKeyword());
			failureLog.setGameName(inboxModel.getGameName());
			failureLog.setStatusCode(statusCode);
			failureLog.setMessage(message);
			failureLog.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
			BeanUtils.copyProperties(chargeCodeRequestDto, failureLog);

			chargeFailureLogs.add(failureLog);

			inboxModel.setStatus("F");
			inboxModel.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
			contentForUpdate.add(inboxModel);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
