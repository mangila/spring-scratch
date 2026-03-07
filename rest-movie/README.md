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

The tricky part is to ensure that messages are delivered in the correct order.
Sometimes it's better to design without a hard dependency on the order of messages.

JobRunr is used for outbox processing and delivery. 
The outbox can be offloaded to a separate process to improve performance and scalability.

### Alternatives

#### One Message broker to rule them all

This can also be implemented with one/cluster message broker and use middleware instead of route messages to the correct
destinations.

`
(outbox) system -> message broker -> (inbox) middleware -> destinations
`

The middleware needs the transactional inbox pattern, and the inbox middleware services can have specific network rules and such,
instead of having the outbox system juggle different networks and protocols.

#### Let clients decide

Expose REST endpoint for replay history of messages and let clients juggle with their own offset.

Clients can replay messages in the correct order and send them to the correct destinations.

Then clients need to poll the system for new messages.

#### Postgres LISTEN/NOTIFY

Attach pg_notify to the database and use LISTEN/NOTIFY to receive new messages for a consuming system.

High chance of missed messages, but the system is simpler.
Can be used in conjuction with the outbox where LISTEN/NOTIFY is used as an high performance happy path and fallbacks to relay when failing.

