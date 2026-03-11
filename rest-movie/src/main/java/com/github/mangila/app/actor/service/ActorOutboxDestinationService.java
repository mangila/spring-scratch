package com.github.mangila.app.actor.service;

import com.github.mangila.app.actor.persistence.outbox.destination.ActorOutboxDestinationEntity;
import com.github.mangila.app.actor.persistence.outbox.destination.ActorOutboxDestinationJdbcRepository;
import com.github.mangila.app.actor.persistence.outbox.destination.ActorOutboxDestinationJpaRepository;
import com.github.mangila.app.actor.properties.ActorProperties;
import com.github.mangila.app.shared.chaos.Chaos;
import com.github.mangila.app.shared.persistence.base.projection.OutboxDestinationProjection;
import com.github.mangila.app.shared.persistence.type.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Validated
public class ActorOutboxDestinationService {

    private final ActorOutboxDestinationJdbcRepository jdbc;

    private final ActorOutboxDestinationJpaRepository jpa;

    private final ActorProperties actorProperties;

    public ActorOutboxDestinationService(ActorOutboxDestinationJdbcRepository actorOutboxDestinationJdbcRepository,
                                         ActorOutboxDestinationJpaRepository actorOutboxDestinationJpaRepository, ActorProperties actorProperties) {
        this.jdbc = actorOutboxDestinationJdbcRepository;
        this.jpa = actorOutboxDestinationJpaRepository;
        this.actorProperties = actorProperties;
    }

    @Chaos
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ActorOutboxDestinationEntity> createDestinations(@NotNull UUID id) {
        var destinations = actorProperties.getOutbox()
                .getDestinations()
                .stream()
                .map(destination -> new ActorOutboxDestinationEntity(id, destination, Status.PENDING))
                .toList();
        return jpa.persistAll(destinations);
    }

    @Chaos
    public boolean updateStatus(UUID destinationId, Status from, Status to) {
        int result = jpa.changeStatus(destinationId, from, to);
        return result > 0;
    }

    @Chaos
    public List<OutboxDestinationProjection> findAllByOutboxIdAndStatus(UUID outboxId, Status status) {
        return jpa.findAllByOutboxIdAndStatus(outboxId, status, OutboxDestinationProjection.class);
    }

    @Chaos
    public List<ActorOutboxDestinationEntity> findAllByStatus(Status status, @Positive int limit) {
        var sort = Sort.by(Sort.Direction.ASC, "updated_at", "created_at");
        return jpa.findAllByStatus(status, Limit.of(limit), sort);
    }

    @Chaos
    public void deleteAllById(ArrayList<UUID> destinationsIds) {
        jpa.deleteAllByIdInBatch(destinationsIds);
    }

    @Chaos
    public List<OutboxDestinationProjection> findAllByOutboxId(UUID outboxId) {
        return jpa.findAllByOutboxId(outboxId, OutboxDestinationProjection.class);
    }

    @Chaos
    public List<OutboxDestinationProjection> claimBatch(UUID outboxId, Status from, Status to) {
        return jdbc.claimBatch(outboxId, from, to);
    }
}
