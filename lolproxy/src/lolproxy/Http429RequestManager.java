package lolproxy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Http429RequestManager {

    private final int minWait;
    private final int retryAfterTimeIncrease;
    private Map<String, Long> nextAuthorizedCalls = new HashMap<>();

    public Http429RequestManager(int minWait, int retryAfterTimeIncrease) {
        this.minWait = minWait;
        this.retryAfterTimeIncrease = retryAfterTimeIncrease;
    }

    public synchronized void reportHttp429(String host, Integer retryAfterHeaderValue) {
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

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                Long nextAuthorizedCall = this.nextAuthorizedCalls.get(address.getHostAddress());

                if (nextAuthorizedCall == null) {
                    nextAuthorizedCall = 0L;
                }

                this.nextAuthorizedCalls.put(address.getHostAddress(), Math.max(this.getTime() + wait, nextAuthorizedCall));
            }
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized boolean canRunRequest(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                Long nextAuthorizedCall = this.nextAuthorizedCalls.get(address.getHostAddress());

                if (nextAuthorizedCall != null && nextAuthorizedCall > this.getTime()) {
                    return false;
                }

            }

            return true;
        } catch (UnknownHostException ex) {
            return true;
        }
    }

    long getTime() {
        return System.currentTimeMillis();
    }
}
