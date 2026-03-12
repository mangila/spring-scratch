package com.github.mangila.app.shared.persistence.base;

import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class OutboxBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "history_id", columnDefinition = "UUID", nullable = false)
	private UUID historyId;

	@Column(name = "aggregate_id", columnDefinition = "UUID", nullable = false)
	private UUID aggregateId;

	@Column(name = "aggregate_version", nullable = false)
	private Integer aggregateVersion;

	@Column(name = "status", columnDefinition = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	private Status status;

	@Column(name = "modified_by", columnDefinition = "text", updatable = true, nullable = false)
	@LastModifiedBy
	private String modifiedBy;

	@Column(name = "created_at", updatable = false, nullable = false)
	@CreatedDate
	private Instant createdAt;

	@Column(name = "updated_at", updatable = true, nullable = false)
	@LastModifiedDate
	private Instant updatedAt;

	public OutboxBaseEntity() {
		// do nothing, for JPA
	}

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

	public Integer getAggregateVersion() {
		return aggregateVersion;
	}

	public void setAggregateVersion(Integer aggregateVersion) {
		this.aggregateVersion = aggregateVersion;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
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
