/**
 * The Monitor checks if all destinations have been successfully delivered,
 * then updates the outbox status to success, and updates the version table so outboxes can be sent in ordered sequence
 *
 */

package com.github.mangila.app.movie.scheduler.outbox.monitor;