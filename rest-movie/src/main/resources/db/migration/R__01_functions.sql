CREATE OR REPLACE FUNCTION fn_actor_history() RETURNS trigger AS
$$
DECLARE
    v_aggregate_id      uuid    := NEW.id;
    v_aggregate_version integer := NEW.audit_version;
    v_history_id        uuid;
BEGIN
    INSERT INTO actor_history (aggregate_id, aggregate_version, operation,
                               payload, audit_version, created_at, updated_at, created_by, modified_by)
    VALUES (v_aggregate_id,
            v_aggregate_version,
            tg_op,
            to_jsonb(NEW),
            0,
            transaction_timestamp(),
            transaction_timestamp(),
            'fn_actor_history',
            'fn_actor_history')
    RETURNING id INTO v_history_id;

    INSERT INTO actor_outbox (history_id, aggregate_id, aggregate_version,
                              status, audit_version, created_at, updated_at, created_by, modified_by)
    VALUES (v_history_id,
            v_aggregate_id,
            v_aggregate_version,
            'PENDING',
            0,
            transaction_timestamp(),
            transaction_timestamp(),
            'fn_actor_history',
            'fn_actor_history');

    IF (v_aggregate_version = 0) THEN
        INSERT INTO actor_outbox_version (aggregate_id, current_version, audit_version,
                                          created_at, updated_at, created_by, modified_by)
        VALUES (v_aggregate_id,
                v_aggregate_version,
                0,
                transaction_timestamp(),
                transaction_timestamp(),
                'fn_actor_history',
                'fn_actor_history');
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
    INSERT INTO director_history (aggregate_id, aggregate_version, operation,
                                  payload, audit_version, created_at, updated_at, created_by, modified_by)
    VALUES (v_aggregate_id,
            v_aggregate_version,
            tg_op,
            to_jsonb(NEW),
            0,
            transaction_timestamp(),
            transaction_timestamp(),
            'fn_director_history',
            'fn_director_history')
    RETURNING id INTO v_history_id;

    INSERT INTO director_outbox (history_id, aggregate_id, aggregate_version,
                                 status, audit_version, created_at, updated_at, created_by, modified_by)
    VALUES (v_history_id,
            v_aggregate_id,
            v_aggregate_version,
            'PENDING',
            0,
            transaction_timestamp(),
            transaction_timestamp(),
            'fn_director_history',
            'fn_director_history');

    IF (v_aggregate_version = 0) THEN
        INSERT INTO director_outbox_version (aggregate_id, current_version, audit_version,
                                             created_at, updated_at, created_by, modified_by)
        VALUES (v_aggregate_id,
                v_aggregate_version,
                0,
                transaction_timestamp(),
                transaction_timestamp(),
                'fn_director_history',
                'fn_director_history');
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
    INSERT INTO movie_history (aggregate_id, aggregate_version, operation,
                               payload, audit_version, created_at, updated_at, created_by, modified_by)
    VALUES (v_aggregate_id,
            v_aggregate_version,
            tg_op,
            to_jsonb(NEW),
            0,
            transaction_timestamp(),
            transaction_timestamp(),
            'fn_movie_history',
            'fn_movie_history')
    RETURNING id INTO v_history_id;

    INSERT INTO movie_outbox (history_id, aggregate_id, aggregate_version,
                              status, audit_version, created_at, updated_at, created_by, modified_by)
    VALUES (v_history_id,
            v_aggregate_id,
            v_aggregate_version,
            'PENDING',
            0,
            transaction_timestamp(),
            transaction_timestamp(),
            'fn_movie_history',
            'fn_movie_history');

    IF (v_aggregate_version = 0) THEN
        INSERT INTO movie_outbox_version (aggregate_id, current_version, audit_version,
                                          created_at, updated_at, created_by, modified_by)
        VALUES (v_aggregate_id,
                v_aggregate_version,
                0,
                transaction_timestamp(),
                transaction_timestamp(),
                'fn_movie_history',
                'fn_movie_history');
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;