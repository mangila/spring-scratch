package com.github.mangila.app.movie.properties;

import com.github.mangila.app.shared.persistence.type.Destination;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.movie")
@Validated
public class MovieProperties {

    public static final List<Destination> SUPPORTED_DESTINATIONS = List.of(Destination.KAFKA, Destination.HTTP);

    private Outbox outbox = new Outbox();

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    public static class Outbox {

        private boolean enabled = false;
        private List<Destination> destinations = new ArrayList<>();
        private MovieOutboxMonitorProperties monitor = new MovieOutboxMonitorProperties();
        private MovieOutboxRecoverProperties recover = new MovieOutboxRecoverProperties();
        private MovieOutboxRelayProperties relay = new MovieOutboxRelayProperties();
        private MovieOutboxPurgeProperties purge = new MovieOutboxPurgeProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<Destination> getDestinations() {
            return destinations;
        }

        public void setDestinations(List<Destination> destinations) {
            boolean retained = destinations.retainAll(SUPPORTED_DESTINATIONS);
            if (retained) {
                throw new IllegalArgumentException("Only " + SUPPORTED_DESTINATIONS + " are supported");
            }
            this.destinations = destinations;
        }

        public MovieOutboxMonitorProperties getMonitor() {
            return monitor;
        }

        public void setMonitor(MovieOutboxMonitorProperties monitor) {
            this.monitor = monitor;
        }

        public MovieOutboxRecoverProperties getRecover() {
            return recover;
        }

        public void setRecover(MovieOutboxRecoverProperties recover) {
            this.recover = recover;
        }

        public MovieOutboxRelayProperties getRelay() {
            return relay;
        }

        public void setRelay(MovieOutboxRelayProperties relay) {
            this.relay = relay;
        }

        public MovieOutboxPurgeProperties getPurge() {
            return purge;
        }

        public void setPurge(MovieOutboxPurgeProperties purge) {
            this.purge = purge;
        }
    }

}
