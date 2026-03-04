package com.github.mangila.app.movie.properties;

import com.github.mangila.app.shared.persistence.type.Destination;
import org.intellij.lang.annotations.Language;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.movie")
@Validated
public class MovieProperties {

    private Outbox outbox = new Outbox();

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    public static class Outbox {

        private boolean enabled = false;
        @Language("CronExp")
        private String cron = "0 0/5 * * * ?";
        private int limit = 20;
        private List<Destination> destinations = new ArrayList<>();

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

        public List<Destination> getDestinations() {
            return destinations;
        }

        public void setDestinations(List<Destination> destinations) {
            this.destinations = destinations;
        }
    }
}
