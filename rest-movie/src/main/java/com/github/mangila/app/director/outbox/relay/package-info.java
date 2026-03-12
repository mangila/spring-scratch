/**
 * The outbox relay.
 * <p>
 * Claims a batch of pending outboxes from the database. Then proceeds and enqueues
 * one-by-one to a processing job.
 */

package com.github.mangila.app.director.outbox.relay;