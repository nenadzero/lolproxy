package lolproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class LolProxyTask implements HttpHandler {

    private static final AtomicLong requestMaxId = new AtomicLong();
    private final boolean upgradeToHttps;
    private final Http429RequestManager http429RequestManager;
    private final HttpRpsManager httpRpsManager;
    private final int tasksTimeout;

    public LolProxyTask(boolean upgradeToHttps, Http429RequestManager http429RequestManager, HttpRpsManager httpRpsManager, int tasksTimeout) {
        this.upgradeToHttps = upgradeToHttps;
        this.http429RequestManager = http429RequestManager;
        this.httpRpsManager = httpRpsManager;
        this.tasksTimeout = tasksTimeout;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            long startTime = System.currentTimeMillis();
            long requestId = requestMaxId.incrementAndGet();

            String targetUrl = he.getRequestURI().toString();
            if (upgradeToHttps) {
                targetUrl = targetUrl.replace("http://", "https://");
            }

            System.out.println("[" + (new Date()) + "]\t#" + requestId + "\tReceived Request to " + targetUrl);

            if (!this.http429RequestManager.canRunRequest()) {
                this.returnHttp429ToClient(he, "Too recent Http429 from Riot");

                System.out.println("[" + (new Date()) + "]\t#" + requestId + "\tRequest blocked by proxy (Too recent Http429 from Riot) to " + targetUrl + "\t" + (System.currentTimeMillis() - startTime) + "ms" + "\t" + 429);
                return;
            }

            if (!this.httpRpsManager.addRequestIfCanHandle()) {
                this.returnHttp429ToClient(he, "Too many RPS!");

                System.out.println("[" + (new Date()) + "]\t#" + requestId + "\tRequest blocked by proxy (Too many RPS) to " + targetUrl + "\t" + (System.currentTimeMillis() - startTime) + "ms" + "\t" + 429);
                return;
            }

            RequestDownloadTask downloadTask = new RequestDownloadTask(new URL(targetUrl), tasksTimeout);
            forwardSpecificRequestHeaders(downloadTask, he);

            boolean isTimeout = false;

            try {
                downloadTask.run();
            } catch (DownloadException ex) {
                isTimeout = isTimeoutException(ex);
            }

            Integer responseCode;
            byte[] response;

            if (isTimeout) {
                responseCode = 504;
                response = "LoLProxy: Distant timeout".getBytes();
            } else {
                responseCode = downloadTask.getResponseCode();
                response = downloadTask.getDownloadedData();
            }

            forwardSpecificResponseHeaders(downloadTask, he);

            System.out.println("[" + (new Date()) + "]\t#" + requestId + "\tEnd of Request to " + targetUrl
                    + "\t" + (System.currentTimeMillis() - startTime) + "ms"
                    + "\t" + responseCode);

            this.reportHttp429IfNeeded(downloadTask);

            if (response == null) {
                response = "LoLProxy: NoData".getBytes();
                he.sendResponseHeaders(responseCode == 200 ? 500 : responseCode, response.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(response);
                }
            } else {
                he.sendResponseHeaders(responseCode, response.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(response);
                }
            }
        } catch (Exception ex) {
            System.err.println("[" + (new Date()) + "] Unexpected exception: " + ex);
            try {
                he.sendResponseHeaders(500, ex.toString().length());
                try (OutputStream os = he.getResponseBody()) {
                    os.write(ex.toString().getBytes());
                }
            } catch (Exception ex2) {
                System.err.println("[" + (new Date()) + "] Cannot send http 500: " + ex2);
            }
        }
    }

    private void returnHttp429ToClient(HttpExchange he, String message) throws IOException {

        he.sendResponseHeaders(429, message.getBytes().length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    private void reportHttp429IfNeeded(RequestDownloadTask downloadTask) {
        Integer responseCode = downloadTask.getResponseCode();
        if (responseCode != null && responseCode == 429) {
            Integer retryAfter = null;
            Map<String, List<String>> headers = downloadTask.getResponseHeaders();

            for (Map.Entry<String, List<String>> entrySet : headers.entrySet()) {
                String key = entrySet.getKey();

                if ("Retry-After".equalsIgnoreCase(key)) {
                    retryAfter = Integer.parseInt(entrySet.getValue().get(0).trim());
                }
            }

            System.out.println("[" + (new Date()) + "]\tHttp429 received from Riot with RetryAfter: " + retryAfter);

            this.http429RequestManager.reportHttp429(retryAfter);
        }
    }

    private boolean isTimeoutException(Throwable ex) {

        while (ex.getCause() != null) {
            ex = ex.getCause();

            if ((ex instanceof SocketTimeoutException) || (ex instanceof SocketException)) {
                return true;
            }
        }

        return false;
    }

    private void forwardSpecificResponseHeaders(RequestDownloadTask task, HttpExchange he) {
        this.forwardSpecificResponseHeader(task, he, "Content-Type");
        this.forwardSpecificResponseHeader(task, he, "Content-Encoding");
    }

    private void forwardSpecificResponseHeader(RequestDownloadTask task, HttpExchange he, String headerName) {
        List<String> values = task.getResponseHeaders().get(headerName);
        if (values != null && !values.isEmpty()) {
            he.getResponseHeaders().add(headerName, values.get(0));
        }
    }

    private void forwardSpecificRequestHeaders(RequestDownloadTask task, HttpExchange he) {
        this.forwardSpecificRequestHeader(task, he, "Accept");
        this.forwardSpecificRequestHeader(task, he, "Accept-Language");
        this.forwardSpecificRequestHeader(task, he, "Accept-Encoding");
        this.forwardSpecificRequestHeader(task, he, "User-Agent");

    }

    private void forwardSpecificRequestHeader(RequestDownloadTask task, HttpExchange he, String headerName) {
        List<String> values = he.getRequestHeaders().get(headerName);
        if (values != null && !values.isEmpty()) {
            task.addCustomHeader(headerName, values.get(0));
        }
    }
}
