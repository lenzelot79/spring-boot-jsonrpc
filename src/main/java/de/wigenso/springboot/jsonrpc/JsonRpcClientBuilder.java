package de.wigenso.springboot.jsonrpc;

import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Proxy;

public class JsonRpcClientBuilder<T> {

    private final Class<T> client;
    private String baseUrl = "";
    private RestTemplate restTemplate = new RestTemplate();
    private JsonRpcClientErrorHandler jsonRpcClientErrorHandler = new DefaultJsonRpcClientErrorHandler();
    private JsonRpcClientInterceptor interceptor;

    private JsonRpcClientBuilder(Class<T> client)  {
        this.client = client;
    }

    public static <T> JsonRpcClientBuilder<T> of(Class<T> client) {
        return new JsonRpcClientBuilder<T>(client);
    }

    public JsonRpcClientBuilder<T> withBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public JsonRpcClientBuilder<T> withRestTemplate(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        return this;
    }

    public JsonRpcClientBuilder<T> withErrorHandler(final JsonRpcClientErrorHandler jsonRpcClientErrorHandler) {
        this.jsonRpcClientErrorHandler = jsonRpcClientErrorHandler;
        return this;
    }

    public JsonRpcClientBuilder<T> withInterceptor(JsonRpcClientInterceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T build() {
        final String apiUrl = baseUrl + (client.isAnnotationPresent(JsonRpcClient.class) ?
                client.getDeclaredAnnotation(JsonRpcClient.class).value() : "");
        return (T) Proxy.newProxyInstance(
                client.getClassLoader(),
                new Class[] { client },
                new JsonRpcInvocationHandler(restTemplate, apiUrl, jsonRpcClientErrorHandler, interceptor));
    }

}
