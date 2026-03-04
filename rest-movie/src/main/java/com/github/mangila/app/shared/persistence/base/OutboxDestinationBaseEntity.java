package com.github.mangila.app.shared.persistence.base;

import com.github.mangila.app.shared.persistence.type.Destination;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.persistence.*;

import java.util.UUID;

@MappedSuperclass
public class OutboxDestinationBaseEntity extends AuditBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "outbox_id", columnDefinition = "UUID", nullable = false)
    private UUID outboxId;

    @Enumerated(EnumType.STRING)
    private Destination destination;

    @Enumerated(EnumType.STRING)
    private Status status;

    public OutboxDestinationBaseEntity() {
        // do nothing, for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOutboxId() {
        return outboxId;
    }

    public void setOutboxId(UUID outboxId) {
        this.outboxId = outboxId;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
