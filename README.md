# JSON RPC for Spring Boot 2

## Motivation
In modern web-applications and microservice architectures a RESTfull API is one of the most popular technique to communicate between different services. RESTfull APIs are resource-oriented. That means, a simple remote-procedure-call (RPC) cannot be designed in a convenient way. For that JSON-RPC has been introduced as protocol based on REST and JSON (https://www.jsonrpc.org/). Still I miss a simple to use integration for Spring Boot. **spring-boot-jsonrpc** is a library to accomplish this.

## Features
* Simple annotation-based integration in spring-boot controllers
* Simple client implementation via prototyping (like feign)
* Annotation based headers
* Works with spring-security and other method or controller based annotations

## Compatibility
Implemented for `spring-boot-starter-parent:2.x.x`

## Changelog 

**Version 1.0.0-RELEASE**
* First public version of the library published

## How to use it 

### Include via Maven
```xml
<dependency>
    <groupId>de.wigenso.springboot</groupId>
    <artifactId>lenzelot-jsonrpc</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```

### Include via Gradle 

```groovy
compile group: 'de.wigenso.springboot', name: 'lenzelot-jsonrpc', version: '1.0-RELEASE'
```

### Creating a JSON RPC Controller

Basic example how to create a JSON RPC endpoint inside a Spring-Boot contoller:

```java
@RequestMapping(value = "/jsonrpc/api") // (2) 
@RestController
public class MyJsonRpcController extends JsonRpcController { // (1)

    @RemoteProcedure // (3)
    public void voidParamAndVoidReturn() {
    }

    @RemoteProcedure
    public String voidParamAndStringReturn() {
        return "Hello World";
    }

    @RemoteProcedure // (4)
    public void throwsRuntimeExceptions() {
        throw new RuntimeException("Hello Error");
    }

    @RemoteProcedure
    public String twoParamsAndStringReturn(final String str1, final int int1) {
        return str1 + " " + int1;
    }

    @RemoteProcedure
    public TestParam complexParamAndReturn(final TestParam testParam) {
        final TestParam r = new TestParam();
        r.setStr1(testParam.getStr1() + "+");
        r.setInt1(testParam.getInt1() + 1);
        return r;
    }
}
```
To allow controllers to publish a procedure for JSON RPC extend `JsonRpcController` from your controller (1). 
The API endpoint can be defined using the `@RequestMapping` annotation in the spring way (2). A procedure that should  
be callable via JSON RPC must be tagged with `@RemoteProcedure` (3). As shown in the example simple and complex types
can be used in parameters. Complex types must be serializable via JSON.     

### Error handling

One method in the previous example (4) throws an exception. If you wand to return a JSON RPC error you can implement 
a mapping for any exception using `@ControllerAdvice` implementing `JsonExceptionConverter`

```java
@ControllerAdvice
public class DefaultJsonExceptionConverter implements JsonExceptionConverter {

    @ExceptionHandler(RuntimeException.class)
    public JsonNode convertRuntimeException(RuntimeException e) {
        return messageToJsonNode(e); 
    }

    @ExceptionHandler(AccessDeniedException.class)
    public JsonNode convertAccessDeniedException(AccessDeniedException e) {
        return messageToJsonNode(e);
    }

}
``` 

The `JsonExceptionConverter` contains simple method to convert exception to a JsonNode. Feel free to implement custom 
converters. The resulting JsonNode is serialized into the *error* field in the `JsonRpcResponse` object.  

### Using Spring interceptor methods

JSON-RPC for spring-boot is designed in a way that you are allowed to use any spring interceptor annotation: 

Example for authorization with spring security
```java
@RemoteProcedure
@PreAuthorize("hasRole('ADMIN')")
public String onlyForAdmin() {
    return "Hello Admin!";
}
```

Other interceptor annotations, eg. for transaction are also supported.

### Mixed controllers

If you need to implement a controller containing JSON-RPC and other endpoint together, you can define the JSON-RPC endpoint
manually inside the controller.

```java
@PostMapping("/rpc")
@ResponseBody
public JsonRpcResponse rpcEndpoint(@RequestBody JsonRpcRequest request) throws Throwable {
    return JsonRpcHandler.jsonRpcCall(request, this); // (1)
}
```
The example defines a JSON-PRC endpoint on '/rpc'. The static in (1) will handle JSON-RPC calls. The second parameter 
to the static call must be the class containing the target procedure(s) annotated with `@RemoteProcedure`

### Parameter Injection 

Parameters of the following types will be injected by the framework (as in spring-boot) when they are added to the remote-procedures signature: 
* `Principal`
* `HttpServletRequest`

In the following example the `Principal` will be injected by the framework, this parameter need not to be added to the `JsonRpcRequest`: 
```java
@RemoteProcedure
public String helloPrincipal(String say, String mark, Principal principal) {
    return say + " " + principal.getName() + mark;
}
```

### JSON-RPC Java Client   

To handle JSON-RPC calls in Java the `@JsonRpcClient` can be used to define a proxy.

A client for the above example will look like this: 

```java
@JsonRpcClient("/jsonrpc/api")
public interface MyJsonRpcControllerClient {

    void voidParamAndVoidReturn();

    String voidParamAndStringReturn();

    void throwsRuntimeExceptions();

    String twoParamsAndStringReturn(final String str1, final int int1);

    TestParam complexParamAndReturn(final TestParam testParam);
}
```

**Header injection**

If you need to inject parameters via a http-header and not as JSON-RPC parameter, it can be annotated with `@RequestHeader` from spring web:

```java
String helloHeader(@RequestHeader("x-test") final String value);
```

In this example the parameter *value* will not be added to JSON-RPC parameters, it will be send as http-header with key "x-test".

**Registration and instantiation of the Client**

To get an instance of the client a bean as follows must be registered. For test the instance can be created directly in the test class. 

```java
MyJsonRpcControllerClient client() {
    return JsonRpcClientBuilder.of(MyJsonRpcControllerClient.class) // (1)
            .withRestTemplate(restTemplate) // (2)
            .withBaseUrl("http://localhost:" + port) // (3)
            .withErrorHandler(new MyJsonRpcClientErrorHandler()) // (4)
            .withInterceptor(retryInterceptor) // (5)
            .build();
}
```

* (1) In this example an instance of `MyJsonRpcControllerClient` is generated by te the framework. 
* (2) *optional* If it is missing the library uses the default `RestTemplate` from spring. Use your own RestTemplate 
if you need to inject some headers like *Authorization*, or if you need a special RestTemplate
like `KeycloakRestTemplate` if you are working with Keycloak. 
* (3) The base URL of your service must be defined as shown.
* (4) *optional* Custom error handler
* (5) *optional* Interceptor that will be called before sending request to server 

**Error handling**

For REST errors (HTTP Status 4xx, 5xx) the spring exception knwon form `RestTemplate` were thrown. 
A JSON-RPC error (HTTP Status 200, with filled error field in response body) will be transformed into a `JsonRpcClientException`.
If you need a custom error handler you implement an own error handler and register it explained above in (4). 

Example for a custom error handler: 

```java
public class MyJsonRpcClientErrorHandler implements JsonRpcClientErrorHandler {
    @Override
    public void handleError(JsonNode errorNode) {
        throw new MyJsonRpcClientException(errorNode);
    }
}
```  

**Retry policy**

The client library has no own retry policy implemented. But using an interceptor as shown in example above in step (5), 
a retry policy can implemented very easy using spring-retry. 

Following steps are required for that: 

(1) Include spring retry and aop: 

```xml 
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

(2) Annotate a spring configuration with `@EnableRetry`

(3) Implement your own retry interceptor like this: 

```java
@Service
public class JsonRpcClientRetryInterceptor implements JsonRpcClientInterceptor {

    @Retryable(value = ExceptionYouWantARetryFor.class, maxAttempts = 4)
    public ResponseEntity<JsonRpcResponse> execute(final HttpEntity<JsonRpcRequest> entity, 
            final JsonRpcClientTarget target) {
        return target.execute(entity);
    }
}
```  

 (4) Register it in `MyJsonRpcControllerClient client()` using `withInterceptor(retryInterceptor)` (example above)


## Test project 

For advanced testing there is a test project on GitHub, containing all the examples above: [spring-boot-jsonrpc-test](https://github.com/lenzelot79/spring-boot-jsonrpc-test)