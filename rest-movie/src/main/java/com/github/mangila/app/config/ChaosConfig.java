package com.github.mangila.app.config;

import com.github.mangila.app.shared.chaos.ChaosAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@ConditionalOnProperty(name = "app.chaos.enabled", havingValue = "true")
public class ChaosConfig {

	private static final Logger log = LoggerFactory.getLogger(ChaosConfig.class);

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		log.info("### CHAOS ENABLED ###");
	}

	@Bean
	ChaosAspect chaosAspect() {
		return new ChaosAspect();
	}

}
