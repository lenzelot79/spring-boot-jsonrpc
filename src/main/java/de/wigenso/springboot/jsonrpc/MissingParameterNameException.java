package de.wigenso.springboot.jsonrpc;

public class MissingParameterNameException extends RuntimeException {

    public MissingParameterNameException(final String parameter, final String method) {
        super("No parameter '" + parameter + "' given for method '" + method + "'");
    }

}
