package com.github.mangila.movie.persistence.outbox.version;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "outbox_version")
@Table(name = "outbox_version")
@EntityListeners(AuditingEntityListener.class)
public class OutboxVersionEntity {

	@Id
	@Column(name = "aggregate_id", columnDefinition = "UUID", nullable = false)
	private UUID aggregateId;

	@Column(name = "latest_version", nullable = false)
	private Integer latestVersion;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", updatable = true, nullable = false)
	private Instant updatedAt;

	public UUID getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(UUID aggregateId) {
		this.aggregateId = aggregateId;
	}

	public Integer getLatestVersion() {
		return latestVersion;
	}

	public void setLatestVersion(Integer latestVersion) {
		this.latestVersion = latestVersion;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

}
