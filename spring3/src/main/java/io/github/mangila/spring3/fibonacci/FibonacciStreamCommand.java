package io.github.mangila.spring3.fibonacci;

public record FibonacciStreamCommand(int limit, long delayMillis) {}
