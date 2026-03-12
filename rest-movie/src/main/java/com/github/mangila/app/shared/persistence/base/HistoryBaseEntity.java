package com.github.mangila.app.shared.persistence.base;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Immutable
@EntityListeners(AuditingEntityListener.class)
public class HistoryBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "aggregate_id", columnDefinition = "UUID", nullable = false)
	private UUID aggregateId;

	@Column(name = "aggregate_version", nullable = false)
	private Integer aggregateVersion;

	@Column(name = "operation", nullable = false)
	private String operation;

	@Type(JsonType.class)
	@Column(columnDefinition = "jsonb", nullable = false)
	private JsonNode payload;

	@Column(name = "created_at", updatable = false, nullable = false)
	@CreatedDate
	private Instant createdAt;

	public HistoryBaseEntity() {
		// do nothing, for JPA
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(UUID aggregateId) {
		this.aggregateId = aggregateId;
	}

	public Integer getAggregateVersion() {
		return aggregateVersion;
	}

	public void setAggregateVersion(Integer version) {
		this.aggregateVersion = version;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public JsonNode getPayload() {
		return payload;
	}

	public void setPayload(JsonNode payload) {
		this.payload = payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
