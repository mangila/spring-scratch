package com.github.mangila.movie.properties;

import jakarta.validation.constraints.Positive;
import org.intellij.lang.annotations.Language;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.outbox")
@Validated
public class OutboxProperties {

    private boolean enabled = false;
    @Language("CronExp")
    private String cron = "0 * * * * *";
    @Positive
    private int limit = 20;

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

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
