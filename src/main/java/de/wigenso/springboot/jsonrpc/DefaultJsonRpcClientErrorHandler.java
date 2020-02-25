package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJsonRpcClientErrorHandler implements JsonRpcClientErrorHandler {

    public void handleError(final JsonNode errorNode) {
        throw new JsonRpcClientException(errorNode);
    }

}
