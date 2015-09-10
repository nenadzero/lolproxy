package lolproxy;

public class HttpRpsManager {

    private final long[] lastSecondEvents;
    private int currentIndexLastSecond;

    public HttpRpsManager(int maxRequestsPerSecond) {
        this.lastSecondEvents = new long[maxRequestsPerSecond];
        this.currentIndexLastSecond = 0;

        for (int i = 0; i < this.lastSecondEvents.length; i++) {
            this.lastSecondEvents[i] = Long.MIN_VALUE;
        }
    }

    public final synchronized boolean addRequestIfCanHandle() {
        if (this.canHandleRequest()) {
            this.addRequest();
            return true;
        } else {
            return false;
        }
    }

    private void addRequest() {
        long now = this.getTime();

        this.lastSecondEvents[this.currentIndexLastSecond] = now;

        if (this.currentIndexLastSecond + 1 >= this.lastSecondEvents.length) {
            this.currentIndexLastSecond = 0;
        } else {
            this.currentIndexLastSecond++;
        }
    }

    private boolean canHandleRequest() {
        long now = this.getTime();
        long oneSecondAgo = now - 1000;

        return this.lastSecondEvents[this.currentIndexLastSecond] < oneSecondAgo;
    }

    long getTime() {
        return System.currentTimeMillis();
    }
}
