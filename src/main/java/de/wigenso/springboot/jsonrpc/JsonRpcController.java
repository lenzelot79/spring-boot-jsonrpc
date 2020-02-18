package de.wigenso.springboot.jsonrpc;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

public class JsonRpcController {

    @PostMapping
    @ResponseBody
    public JsonRpcResponse jsonRpcCall(@RequestBody JsonRpcRequest request) throws Throwable {
        return JsonRpcHandler.jsonRpcCall(request, this);
    }


}
