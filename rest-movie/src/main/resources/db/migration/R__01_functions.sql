CREATE OR REPLACE FUNCTION fn_actor_history() RETURNS trigger AS
$$
DECLARE
    gen_history_id UUID;
BEGIN
    INSERT INTO actor_history (aggregate_id, version, operation, payload, created_at)
    VALUES (NEW.id, NEW.version, tg_op, to_jsonb(NEW), transaction_timestamp())
    RETURNING id INTO gen_history_id;

    INSERT INTO outbox (history_id, aggregate_id, status, version, created_at, updated_at)
    VALUES (gen_history_id, NEW.id, 'PENDING', NEW.version, transaction_timestamp(), transaction_timestamp());

    IF (NEW.version = 0) THEN
        INSERT INTO outbox_version (aggregate_id, current_version, created_at, updated_at)
        VALUES (NEW.id, NEW.version, transaction_timestamp(), transaction_timestamp());
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_director_history() RETURNS trigger AS
$$
DECLARE
    gen_history_id UUID;
BEGIN
    INSERT INTO director_history (aggregate_id, version, operation, payload, created_at)
    VALUES (NEW.id, NEW.version, tg_op, to_jsonb(NEW), transaction_timestamp())
    RETURNING id INTO gen_history_id;

    INSERT INTO outbox (history_id, aggregate_id, status, version, created_at, updated_at)
    VALUES (gen_history_id, NEW.id, 'PENDING', NEW.version, transaction_timestamp(), transaction_timestamp());

    IF (NEW.version = 0) THEN
        INSERT INTO outbox_version (aggregate_id, current_version, created_at, updated_at)
        VALUES (NEW.id, NEW.version, transaction_timestamp(), transaction_timestamp());
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_movie_history() RETURNS trigger AS
$$
DECLARE
    gen_history_id UUID;
BEGIN
    INSERT INTO movie_history (aggregate_id, version, operation, payload, created_at)
    VALUES (NEW.id, NEW.version, tg_op, to_jsonb(NEW), transaction_timestamp())
    RETURNING id INTO gen_history_id;

    INSERT INTO outbox (history_id, aggregate_id, status, version, created_at, updated_at)
    VALUES (gen_history_id, NEW.id, 'PENDING', NEW.version, transaction_timestamp(), transaction_timestamp());

    IF (NEW.version = 0) THEN
        INSERT INTO outbox_version (aggregate_id, current_version, created_at, updated_at)
        VALUES (NEW.id, NEW.version, transaction_timestamp(), transaction_timestamp());
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;