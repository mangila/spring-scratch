package com.github.mangila.app.actor.properties;

import com.github.mangila.app.shared.persistence.type.Destination;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.actor")
@Validated
public class ActorProperties {

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

		private ActorOutboxMonitorProperties monitor = new ActorOutboxMonitorProperties();

		private ActorOutboxRecoverProperties recover = new ActorOutboxRecoverProperties();

		private ActorOutboxRelayProperties relay = new ActorOutboxRelayProperties();

		private ActorOutboxPurgeProperties purge = new ActorOutboxPurgeProperties();

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

		public ActorOutboxMonitorProperties getMonitor() {
			return monitor;
		}

		public void setMonitor(ActorOutboxMonitorProperties monitor) {
			this.monitor = monitor;
		}

		public ActorOutboxRecoverProperties getRecover() {
			return recover;
		}

		public void setRecover(ActorOutboxRecoverProperties recover) {
			this.recover = recover;
		}

		public ActorOutboxRelayProperties getRelay() {
			return relay;
		}

		public void setRelay(ActorOutboxRelayProperties relay) {
			this.relay = relay;
		}

		public ActorOutboxPurgeProperties getPurge() {
			return purge;
		}

		public void setPurge(ActorOutboxPurgeProperties purge) {
			this.purge = purge;
		}

	}

}
