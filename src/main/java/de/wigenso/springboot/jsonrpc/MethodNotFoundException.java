package de.wigenso.springboot.jsonrpc;

public class MethodNotFoundException extends RuntimeException {

    public MethodNotFoundException(String method) {

        super("No implementation for method '" + method + "' found.");

    }

}
