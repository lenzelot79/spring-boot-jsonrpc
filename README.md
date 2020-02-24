# JSON RPC for Spring Boot 2

## Motivation
In modern web-applications and microservice achitectures a RESTful API is one of the most popular technique to communicate between different services. RESTful APIs are ressource-oriented. That means, a simple remote-procedure-call (RPC) cannot be deisnged in a convenient way. For that JSON-RPC has been introduced as protcoll based on REST and JSON (https://www.jsonrpc.org/). Still I miss a simple to use integration for Spring Boot. **spring-boot-jsonrpc** is a library to accomplish this.

## Features
* Simple annoation-based integration in spring-boot controllers
* Simple client implementation via prototyping (like feign)
* Annotation based headers
* Works with spring-security and other method or controller based annotations

## Compatibility
Tested with `spring-boot-starter-parent:2.x.x`

## How to use it 

### Include via Maven
```xml
    <dependency>
        <groupId>de.wigenso.springboot</groupId>
        <artifactId>lenzelot-jsonrpc</artifactId>
        <version>1.0-RELEASE</version>
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

### Errorhandling

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

... TBD

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



... TBD


