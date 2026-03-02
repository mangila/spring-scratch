create table outbox
(
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    history_id   UUID                           NOT NULL,
    aggregate_id UUID                           NOT NULL,
    status       varchar(255) check ((status in ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))),
    version      INTEGER                        NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE,
    updated_at   TIMESTAMP(6) WITH TIME ZONE,
    primary key (id)
);

create table outbox_version
(
    aggregate_id    UUID    NOT NULL,
    current_version INTEGER NOT NULL,
    created_at      TIMESTAMP(6) WITH TIME ZONE,
    updated_at      TIMESTAMP(6) WITH TIME ZONE,
    primary key (aggregate_id)
);