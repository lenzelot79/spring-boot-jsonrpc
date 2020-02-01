package de.wigenso.springboot.jsonrpc;

public class ToFewNamedParametersForMethodException extends RuntimeException {
    public ToFewNamedParametersForMethodException(final String method, final int given, final int required) {
        super("To few parameters given for method '" + method + "' (given " + given + ", requred " + required + ") ");
    }
}
