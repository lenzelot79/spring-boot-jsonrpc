package de.wigenso.springboot.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class JsonRpcHandler {

    static List<String> SUPPORTED_VERSIONS = List.of("2.0");

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpServletRequest httpServletRequest;

    public JsonRpcResponse jsonRpcCall(JsonRpcRequest request) throws Throwable {

        if (!SUPPORTED_VERSIONS.contains(request.getJsonrpc())) {
            throw new UnsupportedJsonRpcVersionException(request.getJsonrpc());
        }

        if (StringUtils.isEmpty(request.getMethod())) {
            throw new MethodMissingException();
        }

        final JsonRpcHandler self = ctx.getBean(this.getClass());

        for (final Method method : self.getClass().getMethods()) {

            Method parentOfProxyMethod = AopUtils.getMostSpecificMethod(method, self.getClass()); // for annotations

            if (parentOfProxyMethod.isAnnotationPresent(RemoteProcedure.class) && method.getName().equals(request.getMethod())) {

                Object methodReturnValue = null;
                InvocationTargetException methodReturnException = null;

                final int numberOfParams = request.getParams() == null ? 0 : request.getParams().size();
                final int numberOfParamsToInject = getNumberOfParamsToInject(method);
                if (method.getParameterCount() - numberOfParamsToInject > 0 && request.getParams() == null
                        || method.getParameterCount() - numberOfParamsToInject > numberOfParams) {
                    throw new ToFewNamedParametersForMethodException(method.getName(), numberOfParams, method.getParameterCount());
                }

                final Object[] params;

                if (request.getParams() == null) {
                    params = new Object[0];
                } else if (request.getParams().isArray()) {
                    params = getParamsFromArray(request, method, parentOfProxyMethod);
                } else {
                    params = getParams(request, method, parentOfProxyMethod);
                }

                try {
                    methodReturnValue = method.invoke(self, params);
                } catch (InvocationTargetException e) {
                    methodReturnException = e;
                }

                JsonRpcResponse result = new JsonRpcResponse();
                result.setJsonrpc(request.getJsonrpc());
                result.setId(request.getId());
                if (methodReturnValue != null) {
                    result.setResult(objectMapper.convertValue(methodReturnValue, JsonNode.class));
                }
                if (methodReturnException != null) {
                    try {
                        final Throwable targetException = methodReturnException.getTargetException();
                        result.setError(tryToConvert(targetException).orElseThrow(() -> targetException));
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
                return result;
            }

        }

        throw new MethodNotFoundException(request.getMethod());
    }

    private Object[] getParams(JsonRpcRequest request, Method method, Method parentOfProxyMethod) {
        Object[] params = new Object[method.getParameterCount()];

        for (int i  = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = parentOfProxyMethod.getParameters()[i];
            Object param = tryGetInjectionParameter(parameter, httpServletRequest.getUserPrincipal(), httpServletRequest);
            if (param == null) {
                JsonNode paramNode = request.getParams().findValue(parameter.getName());
                if (paramNode == null) {
                    throw new MissingParameterNameException(parameter.getName(), method.getName());
                }
                param = objectMapper.convertValue(paramNode, parameter.getType());
            }
            params[i] = param;
        }
        return params;
    }

    private Object[] getParamsFromArray(JsonRpcRequest request, Method method, Method parentOfProxyMethod) {
        Object[] params = new Object[method.getParameterCount()];

        Iterator<JsonNode> requestParametersIterator = request.getParams().iterator();
        for (int i  = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = parentOfProxyMethod.getParameters()[i];
            Object param = tryGetInjectionParameter(parameter, httpServletRequest.getUserPrincipal(), httpServletRequest);
            if (param == null) {
                param = objectMapper.convertValue(requestParametersIterator.next(), parameter.getType());
            }
            params[i] = param;
        }
        return params;
    }

    private Optional<JsonNode> tryToConvert(Throwable throwable) throws InvocationTargetException, IllegalAccessException {

        for (JsonExceptionConverter converter : ctx.getBeansOfType(JsonExceptionConverter.class).values()) {
            for (Method method : converter.getClass().getMethods()) {
                Method parentOfProxyMethod = AopUtils.getMostSpecificMethod(method, converter.getClass());
                ExceptionHandler exceptionHandler = parentOfProxyMethod.getAnnotation(ExceptionHandler.class);
                if (exceptionHandler != null && Arrays.asList(exceptionHandler.value()).contains(throwable.getClass())) {
                    return Optional.of((JsonNode) method.invoke(converter, throwable));
                }
            }
        }

        return Optional.empty();
    }


    private int getNumberOfParamsToInject(Method method) {
        return (int) Stream.of(method.getParameters()).filter(p -> List.of(Principal.class, HttpServletRequest.class).contains(p.getType())).count();
    }


    private static Object tryGetInjectionParameter(final Parameter parameter, final Principal principal, final HttpServletRequest httpServletRequest) {

        if (Principal.class.equals(parameter.getType())) {
            return principal;
        } else if (HttpServletRequest.class.equals(parameter.getType())) {
            return httpServletRequest;
        } else {
            return null;
        }
    }


}
