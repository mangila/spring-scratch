package com.github.mangila.app.shared.persistence.base;

import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.persistence.*;

import java.util.UUID;

@MappedSuperclass
public class OutboxBaseEntity extends AuditBaseEntity {

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

    @Enumerated(EnumType.STRING)
    private Status status;

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

}
