package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

public class JsonRpcController {

    static List<String> SUPPORTED_VERSIONS = List.of("2.0");

    @Autowired
    private ApplicationContext ctx;

    @PostMapping
    @ResponseBody
    public JsonRpcResponse jsonRpcCall(@RequestBody JsonRpcRequest request) throws IllegalAccessException {

        if (!SUPPORTED_VERSIONS.contains(request.getJsonrpc())) {
            throw new UnsupportedJsonRpcVersionException(request.getJsonrpc());
        }

        if (StringUtils.isEmpty(request.getMethod())) {
            throw new MethodMissingException();
        }

        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonRpcController self = ctx.getBean(this.getClass()); // TODO: kann eventuell wieder weg, falls Test korrekt funktioniert prüfen!

        for (final Method method : AopUtils.getTargetClass(this).getMethods()) {

            if (method.isAnnotationPresent(JsonRpc.class) && method.getName().equals(request.getMethod())) {

                Object methodReturnValue = null;
                InvocationTargetException methodReturnException = null;

                final int numberOfParams = request.getParams() == null ? 0 : request.getParams().size();
                if (method.getParameterCount() > 0 && request.getParams() == null
                        || method.getParameterCount() > numberOfParams) {
                    throw new ToFewNamedParametersForMethodException(method.getName(), numberOfParams, method.getParameterCount());
                }

                if (request.getParams() == null) {

                    try {
                        methodReturnValue = method.invoke(self);
                    } catch (InvocationTargetException e) {
                        methodReturnException = e;
                    }

                } else if (request.getParams().isArray()) {

                    Object[] params = new Object[method.getParameterCount()];

                    for (int i  = 0; i < method.getParameterCount(); i++) {
                        JsonNode paramNode = request.getParams().get(i);
                        Object param = objectMapper.convertValue(paramNode, method.getParameters()[i].getType());
                        params[i] = param;
                    }

                    try {
                        methodReturnValue = method.invoke(self, params);
                    } catch (InvocationTargetException e) {
                        methodReturnException = e;
                    }

                } else if (request.getParams() != null) {

                    Object[] params = new Object[method.getParameterCount()];

                    for (int i  = 0; i < method.getParameterCount(); i++) {
                        Parameter parameter = method.getParameters()[i];
                        JsonNode paramNode = request.getParams().findValue(parameter.getName());
                        if (paramNode == null) {
                            throw new MissingParameterNameException(parameter.getName(), method.getName());
                        }
                        Object param = objectMapper.convertValue(paramNode, method.getParameters()[i].getType());
                        params[i] = param;
                    }

                    try {
                        methodReturnValue = method.invoke(self, params);
                    } catch (InvocationTargetException e) {
                        methodReturnException = e;
                    }
                }

                JsonRpcResponse result = new JsonRpcResponse();
                result.setJsonrpc(request.getJsonrpc());
                result.setId(request.getId());
                if (methodReturnValue != null) {
                    result.setResult(objectMapper.convertValue(methodReturnValue, JsonNode.class));
                }
                if (methodReturnException != null) {
                    result.setError(objectMapper.convertValue(methodReturnException.getTargetException(), JsonNode.class));
                }
                return result;
            }

        }

        throw new MethodNotFoundException(request.getMethod());

    }



}
