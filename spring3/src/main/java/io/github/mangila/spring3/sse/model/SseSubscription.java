package io.github.mangila.spring3.sse.model;

public record SseSubscription(String channel, String userAgent, String sessionId) {}
