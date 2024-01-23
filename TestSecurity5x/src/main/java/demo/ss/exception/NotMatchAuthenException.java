package demo.ss.exception;

import org.springframework.security.core.AuthenticationException;

public class NotMatchAuthenException extends AuthenticationException {
    public NotMatchAuthenException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public NotMatchAuthenException(String msg) {
        super(msg);
    }
}
