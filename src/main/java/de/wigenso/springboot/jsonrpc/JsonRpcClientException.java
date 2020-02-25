package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonRpcClientException extends RuntimeException {

    private JsonNode error;

    public JsonNode getError() {
        return this.error;
    }

    public JsonRpcClientException(final JsonNode error) {
        super(error.isTextual() ? error.asText() : error.toPrettyString());
        this.error = error;
    }

}
