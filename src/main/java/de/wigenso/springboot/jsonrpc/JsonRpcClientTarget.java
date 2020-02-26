package de.wigenso.springboot.jsonrpc;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

public interface JsonRpcClientTarget {
    ResponseEntity<JsonRpcResponse> execute(final HttpEntity<JsonRpcRequest> entity);
}
