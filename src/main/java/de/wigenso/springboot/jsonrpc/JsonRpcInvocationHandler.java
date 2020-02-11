package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class JsonRpcInvocationHandler implements InvocationHandler {

    private final static int RQ_ID = 1;
    private final static String JSON_RPC_VERSION = "2.0";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;

    JsonRpcInvocationHandler(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {

        final JsonRpcRequest rq = new JsonRpcRequest();
        rq.setId(RQ_ID);
        rq.setJsonrpc(JSON_RPC_VERSION);
        rq.setMethod(method.getName());

        final List<Object> params = new ArrayList<>();
        final Map<String, List<String>> headers = new HashMap<>();
        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];

            if (parameter.isAnnotationPresent(RequestHeader.class)) {
                final String key = parameter.getAnnotation(RequestHeader.class).value();
                if (!headers.containsKey(key)) {
                    headers.put(key, new ArrayList<>());
                }
                headers.get(key).add(objects[i].toString());
            } else {
                params.add(objects[i]);
            }
        }

        rq.setParams(objectMapper.convertValue(params, JsonNode.class));

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            httpHeaders.add(entry.getKey(), String.join("; ", entry.getValue()));
        }

        final HttpEntity<JsonRpcRequest> entity = new HttpEntity<>(rq,  httpHeaders);
        final ResponseEntity<JsonRpcResponse> result = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, JsonRpcResponse.class);

        if (result.getBody() != null && result.getBody().getError() != null) {
            throw objectMapper.convertValue(result.getBody().getError(), Exception.class); // TODO: specific ?? -> einen Interceptor einfügen / User-Defined parser
        } else if (result.getBody() != null && result.getBody().getResult() != null) {
            return objectMapper.convertValue(result.getBody().getResult(), method.getReturnType());
        } else {
            return null;
        }
    }

}
