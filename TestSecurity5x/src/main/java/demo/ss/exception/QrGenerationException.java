package demo.ss.exception;

public class QrGenerationException extends RuntimeException {
    public QrGenerationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public QrGenerationException(String msg) {
        super(msg);
    }
}