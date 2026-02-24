# spring scratch

Spring scratch stuffs just playing around with spring and try out some use cases or something

```bash
mise build
```
will build all the projects with Buildpack to containers. (Podman)

```bash
mise //<SPRING PROJECT>:container
```
will run the target container in the spring scratch project

## spring1

spring modulith REST C.R.U.D, JobRunr and third party integration

- JVM

```bash
mise //spring1:container
```

## spring2

spring vaadin with spring modulith C.R.U.D

- JVM

```bash
mise //spring2:container
```

## spring3

spring boot SSE with Thymeleaf and HTMX

- GraalVM Native image

```bash
mise //spring3:container
```

## spring4

spring boot Websocket with Thymeleaf and HTMX

- GraalVM Native image

```bash
mise //spring4:container
```