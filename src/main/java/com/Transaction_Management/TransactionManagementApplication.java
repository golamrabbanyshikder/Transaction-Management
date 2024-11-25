package com.Transaction_Management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.Transaction_Management.service.ChargeConfigService;
import com.Transaction_Management.service.ContentConsumerServiceImpl;
import com.Transaction_Management.service.ContentRetrievalServiceImpl;
import com.Transaction_Management.service.KeywordDetailsService;

@SpringBootApplication
public class TransactionManagementApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(TransactionManagementApplication.class, args);
		ContentConsumerServiceImpl consumerService = context.getBean(ContentConsumerServiceImpl.class);
		consumerService.getAllContent();

		ChargeConfigService chargeConfigService = context.getBean(ChargeConfigService.class);
		chargeConfigService.refreshCache();

		KeywordDetailsService keywordDetailsService = context.getBean(KeywordDetailsService.class);
		keywordDetailsService.refreshCache();
		
		ContentRetrievalServiceImpl contentRetrievalServiceImpl = context.getBean(ContentRetrievalServiceImpl.class);
		contentRetrievalServiceImpl.getContentAndCharge();
	}

}
