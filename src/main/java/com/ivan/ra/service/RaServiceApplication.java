package com.ivan.ra.service;

import com.ivan.ra.service.cache.RaServiceCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class RaServiceApplication implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(RaServiceApplication.class);

	@Value("${access.control.settings.path}")
	private String accessControlSettingsPath;

	public static void main(String[] args) {
		SpringApplication.run(RaServiceApplication.class, args);
	}

	public void run(String... args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		RaServiceCache.loadAccessControlSettings(accessControlSettingsPath);
		logger.info("RA service started successfully");
	}
}
