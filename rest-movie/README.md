# Rest movie

Spring boot Vaadin application for managing movie, directors and actors.

The 2026 version of repository – https://github.com/mangila/spring-restful-jpa-flyway

## Links
- http://localhost:8080/swagger-ui/index.html - Swagger UI
- http://localhost:8000 - JobRunr dashboard

## Outbox

A multi-destination transactional outbox pattern is implemented, with ordered messages on each domain.

Traditionally an outbox pattern is used to send one message to the message broker.
But sometimes data needs to be shared to multiple destinations.

Use cases for multi destinations outbox can be:

- Internal kafka message broker that is on a private network
- External third party API
- Legacy systems that require specific message formats
- Emails

This can also be implemented with a single message broker and use middleware instead of route messages to the correct destinations.

`(outbox) system -> message broker -> (inbox) middleware -> destinations`

But then the middleware needs the inbox pattern, and the inbox middleware can have specific network rules and such.
And different middleware services can be used for different destinations.

The tricky part is to ensure that messages are delivered in the correct order. 
Sometimes it's better to design without a hard dependency on the order of messages.
