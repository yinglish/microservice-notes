package com.yin.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

import com.yin.gateway.filters.pre.SimpleFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@EnableZuulProxy
@SpringBootApplication
public class GatewayApplication {
	
	private static Logger log = LoggerFactory.getLogger(GatewayApplication.class);

	public static void main(String[] args) {
		log.info("test");
		SpringApplication.run(GatewayApplication.class, args);
		
	}
	
	@Bean
	public SimpleFilter simpleFilter() {
		return new SimpleFilter();
	}

}
