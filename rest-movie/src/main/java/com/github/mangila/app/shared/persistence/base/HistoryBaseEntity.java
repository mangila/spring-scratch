package com.github.mangila.app.shared.persistence.base;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

@MappedSuperclass
@Immutable
public class HistoryBaseEntity extends AuditBaseEntity {

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
    private ObjectNode payload;

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

    public ObjectNode getPayload() {
        return payload;
    }

    public void setPayload(ObjectNode payload) {
        this.payload = payload;
    }

}
