package org.tastefuljava.jedo;

public class JedoException extends RuntimeException {
    public JedoException(String msg) {
        super(msg);
    }

    public JedoException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
