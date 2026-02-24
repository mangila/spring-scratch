package io.github.mangila.spring1.common;

public final class SecretProperty<T> {

  private final T value;

  public SecretProperty(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "******";
  }
}
