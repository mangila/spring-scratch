CREATE OR REPLACE FUNCTION fn_actor_history() RETURNS trigger AS
$$
DECLARE
    v_aggregate_id      uuid    := NEW.id;
    v_aggregate_version integer := NEW.audit_version;
    v_history_id        uuid;
BEGIN
    INSERT INTO actor_history (aggregate_id, aggregate_version, operation, payload, created_at)
    VALUES (v_aggregate_id,
            v_aggregate_version,
            tg_op,
            to_jsonb(NEW),
            transaction_timestamp())
    RETURNING id INTO v_history_id;

    INSERT INTO actor_outbox (history_id, aggregate_id, aggregate_version,
                              status, modified_by, created_at, updated_at)
    VALUES (v_history_id,
            v_aggregate_id,
            v_aggregate_version,
            'PENDING',
            tg_name,
            transaction_timestamp(),
            transaction_timestamp());

    IF (v_aggregate_version = 0) THEN
        INSERT INTO actor_outbox_version (aggregate_id, current_version, created_at, updated_at)
        VALUES (v_aggregate_id,
                v_aggregate_version,
                transaction_timestamp(),
                transaction_timestamp());
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_director_history() RETURNS trigger AS
$$
DECLARE
    v_aggregate_id      uuid    := NEW.id;
    v_aggregate_version integer := NEW.audit_version;
    v_history_id        uuid;
BEGIN
    INSERT INTO director_history (aggregate_id, aggregate_version, operation, payload, created_at)
    VALUES (v_aggregate_id,
            v_aggregate_version,
            tg_op,
            to_jsonb(NEW),
            transaction_timestamp())
    RETURNING id INTO v_history_id;

    INSERT INTO director_outbox (history_id, aggregate_id, aggregate_version, status,
                                 modified_by, created_at, updated_at)
    VALUES (v_history_id,
            v_aggregate_id,
            v_aggregate_version,
            'PENDING',
            tg_name,
            transaction_timestamp(),
            transaction_timestamp());

    IF (v_aggregate_version = 0) THEN
        INSERT INTO director_outbox_version (aggregate_id, current_version, created_at, updated_at)
        VALUES (v_aggregate_id,
                v_aggregate_version,
                transaction_timestamp(),
                transaction_timestamp());
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_movie_history() RETURNS trigger AS
$$
DECLARE
    v_aggregate_id      uuid    := NEW.id;
    v_aggregate_version integer := NEW.audit_version;
    v_history_id        uuid;
BEGIN
    INSERT INTO movie_history (aggregate_id, aggregate_version, operation, payload, created_at)
    VALUES (v_aggregate_id,
            v_aggregate_version,
            tg_op,
            to_jsonb(NEW),
            transaction_timestamp())
    RETURNING id INTO v_history_id;

    INSERT INTO movie_outbox (history_id, aggregate_id, aggregate_version, status,
                              modified_by, created_at, updated_at)
    VALUES (v_history_id,
            v_aggregate_id,
            v_aggregate_version,
            'PENDING',
            tg_name,
            transaction_timestamp(),
            transaction_timestamp());

    IF (v_aggregate_version = 0) THEN
        INSERT INTO movie_outbox_version (aggregate_id, current_version, created_at, updated_at)
        VALUES (v_aggregate_id,
                v_aggregate_version,
                transaction_timestamp(),
                transaction_timestamp());
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;