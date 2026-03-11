package com.github.mangila.app.director.properties;

import com.github.mangila.app.shared.persistence.type.Destination;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.director")
@Validated
public class DirectorProperties {

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
		private DirectorOutboxMonitorProperties monitor = new DirectorOutboxMonitorProperties();
		private DirectorOutboxRecoverProperties recover = new DirectorOutboxRecoverProperties();
		private DirectorOutboxRelayProperties relay = new DirectorOutboxRelayProperties();
		private DirectorOutboxPurgeProperties purge = new DirectorOutboxPurgeProperties();

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

		public DirectorOutboxMonitorProperties getMonitor() {
			return monitor;
		}

		public void setMonitor(DirectorOutboxMonitorProperties monitor) {
			this.monitor = monitor;
		}

		public DirectorOutboxRecoverProperties getRecover() {
			return recover;
		}

		public void setRecover(DirectorOutboxRecoverProperties recover) {
			this.recover = recover;
		}

		public DirectorOutboxRelayProperties getRelay() {
			return relay;
		}

		public void setRelay(DirectorOutboxRelayProperties relay) {
			this.relay = relay;
		}

		public DirectorOutboxPurgeProperties getPurge() {
			return purge;
		}

		public void setPurge(DirectorOutboxPurgeProperties purge) {
			this.purge = purge;
		}
	}

}
