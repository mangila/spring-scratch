package com.github.mangila.movie.persistence.outbox.destination;

import com.github.mangila.movie.persistence.outbox.type.Destination;
import com.github.mangila.movie.persistence.outbox.type.Status;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity(name = "outbox_destination")
@Table(name = "outbox_destination")
@EntityListeners(AuditingEntityListener.class)
public class OutboxDestinationEntity {

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

    public OutboxDestinationEntity() {
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
