package de.wigenso.springboot.jsonrpc;

public class MethodMissingException extends RuntimeException {

    public MethodMissingException() {

        super("No method given in request.");

    }

}
