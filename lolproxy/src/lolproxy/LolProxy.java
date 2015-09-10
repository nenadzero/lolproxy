package lolproxy;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LolProxy extends Thread {

    private final int port;
    private final int proxyTimeout;
    private final int minWaitAfter429;
    private final int retryAfterTimeIncreaseAfter429;
    private final int maxRequestsPerSecond;
    private final boolean upgradeToHttps;
    private final int threadsCount; // 0 for unlimited
    private HttpServer httpServer = null;

    public LolProxy(int port, int proxyTimeout, int minWaitAfter429, int retryAfterTimeIncreaseAfter429, int maxRequestsPerSecond, boolean upgradeToHttps, int threadsCount) {
        this.port = port;
        this.proxyTimeout = proxyTimeout;
        this.minWaitAfter429 = minWaitAfter429;
        this.retryAfterTimeIncreaseAfter429 = retryAfterTimeIncreaseAfter429;
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.upgradeToHttps = upgradeToHttps;
        this.threadsCount = threadsCount;
    }

    @Override
    public void run() {
        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);

            ExecutorService threadPool = threadsCount == 0 ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(threadsCount);
            this.httpServer.setExecutor(threadPool);
            this.httpServer.createContext("/", new LolProxyTask(upgradeToHttps, new Http429RequestManager(this.minWaitAfter429, this.retryAfterTimeIncreaseAfter429), new HttpRpsManager(maxRequestsPerSecond), proxyTimeout));
            this.httpServer.start();

            System.out.println("[" + (new Date()) + "] Started Proxy on port " + this.port);

        } catch (Exception e) {
            System.out.println("LolProxy Thread error: " + e);
        }
    }

    public void stop(int i) {
        this.httpServer.stop(i);
    }

}
