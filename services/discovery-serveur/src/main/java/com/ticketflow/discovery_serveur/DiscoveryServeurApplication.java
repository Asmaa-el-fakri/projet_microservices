package com.ticketflow.discovery_serveur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServeurApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServeurApplication.class, args);
	}

}
