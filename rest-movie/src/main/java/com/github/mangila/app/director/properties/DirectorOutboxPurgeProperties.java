package com.github.mangila.app.director.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.intellij.lang.annotations.Language;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.director.outbox.purge")
@Validated
public class DirectorOutboxPurgeProperties {

	private boolean enabled = false;

	@Language("CronExp")
	@NotBlank
	private String cron = "0 0/5 * * * ?";

	@Positive
	private int limit = 20;

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCron() {
		return cron;
	}

	public void setCron(@Language("CronExp") String cron) {
		this.cron = cron;
	}

}
