void main() {
    var greeting = new Greeting("Hello, World!");
    IO.println(greeting.message());
}

record Greeting(String message) {
}