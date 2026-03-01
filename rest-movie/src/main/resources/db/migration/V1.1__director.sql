create table director
(
    id         UUID                        NOT NULL,
    name       VARCHAR(255),
    picture    VARCHAR(255),
    bio        VARCHAR(255),
    movies     JSONB,
    version    INTEGER,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    primary key (id)
);
create table director_history
(
    id           UUID DEFAULT gen_random_uuid() NOT NULL,
    aggregate_id UUID                           NOT NULL,
    version      INTEGER,
    operation    VARCHAR(255) NOT NULL,
    payload      JSONB NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE    NOT NULL,
    primary key (id)
);
