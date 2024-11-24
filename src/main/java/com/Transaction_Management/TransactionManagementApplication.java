package com.Transaction_Management;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.Transaction_Management.service.ContentConsumerServiceImpl;
import com.Transaction_Management.service.ContentRetrievalServiceImpl;

@SpringBootApplication
public class TransactionManagementApplication {

	public static void main(String[] args) {
		org.springframework.context.ApplicationContext context = SpringApplication
				.run(TransactionManagementApplication.class, args);
		ContentConsumerServiceImpl consumerService = context.getBean(ContentConsumerServiceImpl.class);
		consumerService.getAllContent();
		
		ContentRetrievalServiceImpl contentRetrievalServiceImpl= context.getBean(ContentRetrievalServiceImpl.class);
		contentRetrievalServiceImpl.getContentAndCharge();
		System.out.println(consumerService);
	}

}
