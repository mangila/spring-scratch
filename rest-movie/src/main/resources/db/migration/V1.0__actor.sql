create table actor
(
    id            UUID                        NOT NULL,
    name          TEXT                        NOT NULL,
    picture       TEXT                        NOT NULL,
    biography     TEXT                        NOT NULL,
    date_of_birth DATE                        NOT NULL,
    movies        JSONB                       NOT NULL,
    audit_version INTEGER                     NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    TEXT                        NOT NULL,
    modified_by   TEXT                        NOT NULL,
    primary key (id)
);

create table actor_history
(
    id                UUID DEFAULT gen_random_uuid() NOT NULL,
    aggregate_id      UUID                           NOT NULL,
    aggregate_version INTEGER,
    operation         TEXT                           NOT NULL,
    payload           JSONB                          NOT NULL,
    audit_version     INTEGER                        NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    created_by        TEXT                           NOT NULL,
    modified_by       TEXT                           NOT NULL,
    primary key (id)
);

create table actor_outbox
(
    id                UUID DEFAULT gen_random_uuid() NOT NULL,
    history_id        UUID                           NOT NULL,
    aggregate_id      UUID                           NOT NULL,
    aggregate_version INTEGER                        NOT NULL,
    status            varchar(255) check ((status in ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))),
    audit_version     INTEGER                        NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    created_by        TEXT                           NOT NULL,
    modified_by       TEXT                           NOT NULL,
    primary key (id)
);

create table actor_outbox_version
(
    aggregate_id    UUID                        NOT NULL,
    current_version INTEGER                     NOT NULL,
    audit_version   INTEGER                     NOT NULL,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by      TEXT                        NOT NULL,
    modified_by     TEXT                        NOT NULL,
    primary key (aggregate_id)
);