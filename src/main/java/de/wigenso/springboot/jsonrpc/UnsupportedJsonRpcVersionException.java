package de.wigenso.springboot.jsonrpc;

public class UnsupportedJsonRpcVersionException extends RuntimeException {
    public UnsupportedJsonRpcVersionException(final String version) {
        super("JSON RPC version '" + version + "' is not supported, supported versions are: " + String.join(", ", JsonRpcController.SUPPORTED_VERSIONS));
    }
}
