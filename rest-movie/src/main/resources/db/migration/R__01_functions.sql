CREATE OR REPLACE FUNCTION fn_actor_history() RETURNS trigger AS
$$
BEGIN
    INSERT INTO actor_history (aggregate_id, version, operation, payload, created_at)
    VALUES (NEW.id, NEW.version, tg_op, to_jsonb(NEW), transaction_timestamp());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_director_history() RETURNS trigger AS
$$
BEGIN
    INSERT INTO director_history (aggregate_id, version, operation, payload, created_at)
    VALUES (NEW.id, NEW.version, tg_op, to_jsonb(NEW), transaction_timestamp());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_movie_history() RETURNS trigger AS
$$
BEGIN
    INSERT INTO movie_history (aggregate_id, version, operation, payload, created_at)
    VALUES (NEW.id, NEW.version, tg_op, to_jsonb(NEW), transaction_timestamp());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;