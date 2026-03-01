package com.github.mangila.movie.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Immutable
public class HistoryBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "aggregate_id", columnDefinition = "UUID", nullable = false)
	private UUID aggregateId;

	private Integer version;

	@Column(name = "operation", nullable = false)
	private String operation;

	@Type(JsonType.class)
	@Column(columnDefinition = "jsonb", nullable = false)
	private ObjectNode payload;

	@Column(name = "created_at", nullable = false)
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

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public ObjectNode getPayload() {
		return payload;
	}

	public void setPayload(ObjectNode payload) {
		this.payload = payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
