# JSON RPC for Spring Boot 2

## Motivation
In modern web-applications and microservice achitectures RESTful APIs are one of the most popular technique to communicate between different services. RESTful APIs are ressource oriented, simple remote-procedure-call (RPC) cannot be deisnged in a convenient way. For that JSON-RPC has been introduced as protcoll based on REST and JSON (https://www.jsonrpc.org/). Still I miss a good integration for Spring Boot. **spring-boot-jsonrpc** is a library to close this gap.


## Features
* Simple annoation-based integration in spring-boot controllers
* Simple client implementation via prototyping (like feign)
* Annotation based headers
* Works with spring-security
