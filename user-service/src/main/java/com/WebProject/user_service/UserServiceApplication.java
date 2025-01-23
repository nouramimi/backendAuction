package com.WebProject.user_service;

import com.WebProject.user_service.event.ProductCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.KafkaListener;


@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class UserServiceApplication {
	private static final Logger log = LoggerFactory.getLogger(UserServiceApplication.class);

	public static void main(String[] args){
		SpringApplication.run(UserServiceApplication.class,args);
	}

	@KafkaListener(
			topics = "notificationTopic")
	public void handleNotification(ProductCreatedEvent productCreatedEvent)
	{
		log.info("received notification for Product creation - {}",productCreatedEvent.getId());

	}


}
