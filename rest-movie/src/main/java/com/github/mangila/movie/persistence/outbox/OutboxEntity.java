package com.github.mangila.movie.persistence.outbox;

import com.github.mangila.movie.persistence.outbox.type.Status;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "outbox")
@Table(name = "outbox")
@EntityListeners(AuditingEntityListener.class)
public class OutboxEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "history_id", columnDefinition = "UUID", nullable = false)
	private UUID historyId;

	@Column(name = "aggregate_id", columnDefinition = "UUID", nullable = false)
	private UUID aggregateId;

	@Column(name = "version", nullable = false)
	private Integer version;

	@Enumerated(EnumType.STRING)
	private Status status;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", updatable = true, nullable = false)
	private Instant updatedAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getHistoryId() {
		return historyId;
	}

	public void setHistoryId(UUID historyId) {
		this.historyId = historyId;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(UUID aggregateId) {
		this.aggregateId = aggregateId;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
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
