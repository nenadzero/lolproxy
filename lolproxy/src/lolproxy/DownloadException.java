package lolproxy;

public class DownloadException extends Exception {

    public DownloadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DownloadException(Throwable cause) {
        super(cause);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException() {
    }
}
