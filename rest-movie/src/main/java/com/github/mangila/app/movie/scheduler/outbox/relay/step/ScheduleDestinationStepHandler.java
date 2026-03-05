package com.github.mangila.app.movie.scheduler.outbox.relay.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mangila.app.movie.scheduler.MovieScheduler;
import com.github.mangila.app.movie.scheduler.outbox.destination.http.MovieHttpDestinationJobRequest;
import com.github.mangila.app.movie.scheduler.outbox.destination.kafka.MovieKafkaDestinationJobRequest;
import com.github.mangila.app.shared.persistence.type.Destination;
import org.jobrunr.jobs.JobId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.github.mangila.app.shared.persistence.type.Destination.HTTP;
import static com.github.mangila.app.shared.persistence.type.Destination.KAFKA;

@Component
public class ScheduleDestinationStepHandler {

    private final Map<Destination, Function<ScheduleDestination, JobId>> destinationMap;

    public ScheduleDestinationStepHandler(MovieScheduler movieScheduler) {
        this.destinationMap = Map.of(
                HTTP, obj -> movieScheduler.schedule(new MovieHttpDestinationJobRequest(obj.destinationId, obj.payload, obj.destination)),
                KAFKA, obj -> movieScheduler.schedule(new MovieKafkaDestinationJobRequest(obj.destinationId, obj.payload, obj.destination)));
    }

    public JobId handle(UUID destinationId, JsonNode payload, Destination destination) {
        final var fn = destinationMap.get(destination);
        if (fn == null) {
            throw new IllegalStateException("Unknown destination: " + destination);
        }
        return fn.apply(new ScheduleDestination(destinationId, payload, destination));
    }


    private record ScheduleDestination(UUID destinationId, JsonNode payload, Destination destination) {

    }
}
