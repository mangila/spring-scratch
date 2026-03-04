package com.github.mangila.app.shared.persistence.base;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public class OutboxVersionBaseEntity extends AuditBaseEntity {

    @Id
    @Column(name = "aggregate_id", columnDefinition = "UUID", nullable = false)
    private UUID aggregateId;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    public OutboxVersionBaseEntity() {
        // do nothing, for JPA
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }
}
