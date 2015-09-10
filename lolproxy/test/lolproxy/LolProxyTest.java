package lolproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Test;

public class LolProxyTest {

    private LolProxy lolProxy;
    private HttpServer targetServer;

    @After
    public void clearTest() {
        lolProxy.stop(0);
        targetServer.stop(0);
    }

    @Test
    public void runRpsTest() {
        lolProxy = new LolProxy(23830, 60000, 3000, 1000, 5, false, 20);

        AtomicInteger serverReceivedRequests = new AtomicInteger();
        targetServer = this.getEchoServer(serverReceivedRequests);

        lolProxy.start();
        targetServer.start();

        ExecutorService pool = Executors.newCachedThreadPool();

        for (int it = 0; it < 2; it++) { // Two times, with a sleep between each

            List<Future<String>> submittedTasks = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                Future<String> submitted = pool.submit(this.getGetTask());
                submittedTasks.add(submitted);
            }

            int received200 = 0;
            int received429 = 0;
            for (Future<String> submittedTask : submittedTasks) {
                try {
                    String response = submittedTask.get();

                    if (response.startsWith("200;")) {
                        received200++;

                        Assert.assertEquals(response, "200;Toto");
                    } else if (response.startsWith("429;")) {
                        received429++;
                    } else {
                        Assert.fail();
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }

            Assert.assertEquals(5, received200);
            Assert.assertEquals(3, received429);

            Assert.assertEquals(5, serverReceivedRequests.get());

            if (it == 0) {
                serverReceivedRequests.set(0);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    @Test
    public void run429TestWithoutRetryAfter() throws Exception {
        lolProxy = new LolProxy(23830, 60000, 2000, 1000, 5, false, 20);

        AtomicInteger serverReceivedRequests = new AtomicInteger();
        targetServer = this.get429HttpServer(serverReceivedRequests, false);

        lolProxy.start();
        targetServer.start();

        String response = getGetTask().call();
        Assert.assertTrue(response.startsWith("429;"));

        for (int i = 0; i < 10; i++) {
            response = getGetTask().call();
            Assert.assertTrue(response.startsWith("429;"));
        }

        Thread.sleep(2000);
        response = getGetTask().call();
        Assert.assertTrue(response.startsWith("429;"));

        Assert.assertEquals(2, serverReceivedRequests.get());
    }

    @Test
    public void run429TestWithRetryAfter() throws Exception {
        lolProxy = new LolProxy(23830, 60000, 2000, 1000, 5, false, 20);

        AtomicInteger serverReceivedRequests = new AtomicInteger();
        targetServer = this.get429HttpServer(serverReceivedRequests, true);

        lolProxy.start();
        targetServer.start();

        String response = getGetTask().call();
        Assert.assertTrue(response.startsWith("429;"));

        for (int i = 0; i < 10; i++) {
            response = getGetTask().call();
            Assert.assertTrue(response.startsWith("429;"));
        }

        Thread.sleep(2000);
        response = getGetTask().call();
        Assert.assertTrue(response.startsWith("429;"));
        Assert.assertEquals(1, serverReceivedRequests.get());

        Thread.sleep(2200);
        response = getGetTask().call();
        Assert.assertTrue(response.startsWith("429;"));

        Assert.assertEquals(2, serverReceivedRequests.get());
    }

    private HttpServer getEchoServer(final AtomicInteger receivedRequests) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(23831), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", new HttpHandler() {

                @Override
                public void handle(HttpExchange he) throws IOException {
                    receivedRequests.incrementAndGet();

                    String response = "Toto";
                    he.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            });

            return server;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Callable<String> getGetTask() {
        return new Callable<String>() {

            @Override
            public String call() throws Exception {

                URL obj = new URL("http://127.0.0.1:23831/toto");
                HttpURLConnection con = (HttpURLConnection) obj.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 23830)));

                int responseCode = con.getResponseCode();
                StringBuilder builder = new StringBuilder();

                try {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            builder.append(inputLine);
                        }
                    }
                } catch (Exception ex) {
                }

                return responseCode + ";" + builder.toString();
            }
        };
    }

    private HttpServer get429HttpServer(final AtomicInteger receivedRequests, final boolean retryAfter) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(23831), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", new HttpHandler() {

                @Override
                public void handle(HttpExchange he) throws IOException {
                    receivedRequests.incrementAndGet();

                    String response = "429";

                    if (retryAfter) {
                        he.getResponseHeaders().add("Retry-After", "3");
                    }

                    he.sendResponseHeaders(429, response.getBytes().length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(response.getBytes());
                    }

                }

            });

            return server;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
