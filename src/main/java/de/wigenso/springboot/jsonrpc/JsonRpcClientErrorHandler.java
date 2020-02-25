package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcClientErrorHandler {

    void handleError(JsonNode errorNode);

}
