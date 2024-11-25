package com.Transaction_Management.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.Transaction_Management.dao.IKeywordDetailsDao;
import com.Transaction_Management.model.KeywordDetailsModel;

public class KeywordDetailsService {

	@Autowired
	IKeywordDetailsDao keywordDetailsDao;

	private Set<String> cachedKeywords = ConcurrentHashMap.newKeySet();

	@Scheduled(fixedRate = 60000) // Refresh every 60 seconds
	public void refreshCache() {
		cachedKeywords.clear();
		cachedKeywords.addAll(
				keywordDetailsDao.findAll().stream().map(KeywordDetailsModel::getKeyword).collect(Collectors.toSet()));
		
	}

	public boolean isValidKeyword(String keyword) {
		return cachedKeywords.contains(keyword);
	}

}
