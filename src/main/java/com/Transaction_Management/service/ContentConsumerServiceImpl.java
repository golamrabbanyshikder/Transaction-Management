package com.Transaction_Management.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.Transaction_Management.dao.IInboxDao;
import com.Transaction_Management.dto.ApiResponse;
import com.Transaction_Management.dto.ContentResponseDto;
import com.Transaction_Management.model.InboxModel;

@Service
public class ContentConsumerServiceImpl implements IContentConsumerService {

	@Value("${base.url}")
	private String baseUrl;
	@Value("${content.api.url}")
	private String contentApiUrl;

	private final IInboxDao inboxDao;

	private final RestTemplate restTemplate;

	public ContentConsumerServiceImpl(IInboxDao inboxDao) {
		this.inboxDao = inboxDao;
		this.restTemplate = new RestTemplate();
	}

	@Override
	public void getAllContent() {
		try {
			ResponseEntity<ApiResponse<ContentResponseDto>> response = restTemplate.exchange(baseUrl + contentApiUrl,
					HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<ContentResponseDto>>() {
					});
			if (response.getStatusCode() == HttpStatus.OK) {
				ApiResponse<ContentResponseDto> apiResponse = response.getBody();
				List<ContentResponseDto> allContent = apiResponse.getContents();

				List<InboxModel> allsavedContent = new ArrayList<>();

				for (ContentResponseDto contentResponseDto : allContent) {
					String[] parts = contentResponseDto.getSms().split(" ");
					InboxModel inboxModel = new InboxModel();
					inboxModel.setKeyword(parts[0]);
					inboxModel.setGameName(parts[1]);
					inboxModel.setStatus("N");
					inboxModel.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
					BeanUtils.copyProperties(contentResponseDto, inboxModel, InboxModel.class);
					allsavedContent.add(inboxModel);

				}

				inboxDao.saveAll(allsavedContent);

			} else {
				throw new RuntimeException("Failed to fetch content. Status code: " + response.getStatusCode());
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
	}

}
