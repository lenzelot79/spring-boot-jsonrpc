package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public interface JsonExceptionConverter {

    default JsonNode messageToJsonNode(Throwable t) {
        return new TextNode(t.getMessage());
    }

}
