package demo.ss.exception;

import org.springframework.security.core.AuthenticationException;

public class MFAInvalideException extends AuthenticationException {
    public MFAInvalideException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MFAInvalideException(String msg) {
        super(msg);
    }
}
