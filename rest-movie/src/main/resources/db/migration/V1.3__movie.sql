create table movie
(
    id            UUID                        NOT NULL,
    title         TEXT UNIQUE,
    genres        JSONB,
    actors        JSONB,
    directors     JSONB,
    budget        NUMERIC(38, 2),
    release_date  DATE,
    audit_version INTEGER                     NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    TEXT                        NOT NULL,
    modified_by   TEXT                        NOT NULL,
    primary key (id)
);

create table movie_history
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

create table movie_outbox
(
    id                UUID DEFAULT gen_random_uuid() NOT NULL,
    history_id        UUID                           NOT NULL,
    aggregate_id      UUID                           NOT NULL,
    aggregate_version INTEGER                        NOT NULL,
    status            Status                         NOT NULL,
    audit_version     INTEGER                        NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    created_by        TEXT                           NOT NULL,
    modified_by       TEXT                           NOT NULL,
    primary key (id)
);

create table movie_outbox_version
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

create table movie_outbox_destination
(
    id            UUID                        NOT NULL,
    outbox_id     UUID                        NOT NULL,
    destination   DESTINATION                 NOT NULL,
    status        STATUS                      NOT NULL,
    audit_version INTEGER                     NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    TEXT                        NOT NULL,
    modified_by   TEXT                        NOT NULL,
    primary key (id)
);