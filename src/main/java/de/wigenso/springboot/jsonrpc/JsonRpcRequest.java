package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonRpcRequest {

    private String jsonrpc; // always 2.0

    private String method;

    private Integer id;

    private JsonNode params;

    public JsonRpcRequest() {
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }
}
