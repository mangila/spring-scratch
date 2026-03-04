create table director
(
    id            UUID                        NOT NULL,
    name          TEXT,
    picture       TEXT,
    biography     TEXT,
    date_of_birth DATE,
    movies        JSONB,
    audit_version INTEGER                     NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    TEXT                        NOT NULL,
    modified_by   TEXT                        NOT NULL,
    primary key (id)
);

create table director_history
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

create table director_outbox
(
    id                UUID DEFAULT gen_random_uuid() NOT NULL,
    history_id        UUID                           NOT NULL,
    aggregate_id      UUID                           NOT NULL,
    aggregate_version INTEGER                        NOT NULL,
    status            STATUS                         NOT NULL,
    audit_version     INTEGER                        NOT NULL,
    created_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    created_by        TEXT                           NOT NULL,
    modified_by       TEXT                           NOT NULL,
    primary key (id)
);

create table director_outbox_version
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
