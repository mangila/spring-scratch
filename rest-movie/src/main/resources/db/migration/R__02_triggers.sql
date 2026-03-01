DROP TRIGGER IF EXISTS trg_actor_history ON actor;

CREATE TRIGGER trg_actor_history
    AFTER INSERT OR UPDATE
    ON actor
    FOR EACH ROW
EXECUTE FUNCTION fn_actor_history();

DROP TRIGGER IF EXISTS trg_director_history ON director;

CREATE TRIGGER trg_director_history
    AFTER INSERT OR UPDATE
    ON director
    FOR EACH ROW
EXECUTE FUNCTION fn_director_history();

DROP TRIGGER IF EXISTS trg_movie_history ON movie;

CREATE TRIGGER trg_movie_history
    AFTER INSERT OR UPDATE
    ON movie
    FOR EACH ROW
EXECUTE FUNCTION fn_movie_history();