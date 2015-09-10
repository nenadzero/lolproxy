package lolproxy;

public class Http429RequestManager {

    private final int minWait;
    private final int retryAfterTimeIncrease;
    private long nextAuthorizedCall = 0;

    public Http429RequestManager(int minWait, int retryAfterTimeIncrease) {
        this.minWait = minWait;
        this.retryAfterTimeIncrease = retryAfterTimeIncrease;
    }

    public synchronized void reportHttp429(Integer retryAfterHeaderValue) {
        if (retryAfterHeaderValue == null) {
            retryAfterHeaderValue = 0;
        }

        if (retryAfterHeaderValue >= 1000) {
            System.err.println("Warning: RetryAfter value seems incorrect. MS instead of Seconds ?");
        }

        // Retry After Header is in Seconds, converting it to ms
        retryAfterHeaderValue *= 1000;

        // Adding retryAfterTimeIncrease
        retryAfterHeaderValue += retryAfterTimeIncrease;

        int wait = Math.max(retryAfterHeaderValue, minWait);

        this.nextAuthorizedCall = Math.max(this.getTime() + wait, this.nextAuthorizedCall);
    }

    public synchronized boolean canRunRequest() {
        return this.getTime() >= this.nextAuthorizedCall;
    }

    long getTime() {
        return System.currentTimeMillis();
    }
}
