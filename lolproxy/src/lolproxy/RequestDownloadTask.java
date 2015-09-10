package lolproxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.*;
import java.net.SocketException;

public final class RequestDownloadTask {

    private static final int bufferSize = 8 * 1024;
    private static final int connectionTimeout = 1000;
    private final Map<String, String> customHeaders = new HashMap<>();
    private HttpURLConnection connection;
    private Integer responseCode;
    private final URL url;
    private final int timeout;
    private byte[] downloadedData;

    public RequestDownloadTask(URL url, int timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    public final byte[] getDownloadedData() {
        return downloadedData;
    }

    public final Map<String, List<String>> getResponseHeaders() {
        if (connection != null) {
            return connection.getHeaderFields();
        } else {
            return null;
        }
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    private synchronized void applyHeaders() {
        for (Entry<String, String> e : this.customHeaders.entrySet()) {
            connection.setRequestProperty(e.getKey(), e.getValue());
        }
    }

    public synchronized final void addCustomHeader(String key, String value) {
        this.customHeaders.put(key.intern(), value.intern());
    }

    private byte[] getDataFromInputStream() throws IOException, DownloadException {
        return getDataFromStream(connection.getInputStream());
    }

    private byte[] getDataFromErrorStream() {
        try {
            return getDataFromStream(connection.getErrorStream());
        } catch (Exception ex) {
            return null;
        }
    }

    private void cleanStreams() {
        byte[] buf = new byte[bufferSize];

        try {
            try (InputStream is0 = connection.getInputStream(); InputStream is = new BufferedInputStream(is0, bufferSize)) {
                if (is0 != null) {
                    int ret = 0;
                    while ((ret = is.read(buf)) > 0) {
                    }
                }
            }
        } catch (Exception e) {
        }

        try {
            try (InputStream es0 = connection.getErrorStream(); InputStream es = new BufferedInputStream(es0, bufferSize)) {
                if (es0 != null) {
                    int ret = 0;
                    while ((ret = es.read(buf)) > 0) {
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    private byte[] getDataFromStream(InputStream stream) throws IOException, DownloadException {
        InputStream in = new BufferedInputStream(stream, bufferSize);
        byte[] raw = streamToByteArray(in);

        return raw;
    }

    private byte[] streamToByteArray(InputStream in) throws IOException, DownloadException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[bufferSize];
        int n = 0;

        while (-1 != (n = in.read(buf))) {
            out.write(buf, 0, n);
        }
        out.close();
        in.close();

        return out.toByteArray();
    }

    private void initializeConnection() throws IOException, DownloadException {
        this.responseCode = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (ClassCastException ex) {
            throw new DownloadException("Incorrect protocol", ex);
        }

        connection.setInstanceFollowRedirects(true);

        // Timeouts
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(timeout);

        applyHeaders();
    }

    /**
     * Connect and download the page
     */
    private void connectAndDownload() throws DownloadException {

        try {
            initializeConnection();

            // Data reading
            connection.connect();

            responseCode = connection.getResponseCode();

            if (responseCode < 400) {
                downloadedData = getDataFromInputStream();
            } else {
                // Error code (http returned code >=400)
                downloadedData = getDataFromErrorStream();

                throw new DownloadException("ErrorCode: " + responseCode);
            }
        } catch (SocketTimeoutException | SocketException ex) {
            /* In case of a socket timeout exception or a socket(connection) exception, sometimes data can still be emitted to the client. 
             * To prevent that, we close the underlying exception, not reading this stream and closing the socket.
             * This is not really good for keep-alive stuff, but this is the expected behavior of the system.
             */
            connection.disconnect(); // Close the underlying connection
            throw new DownloadException(ex);
        } catch (IOException | DownloadException | RuntimeException ex) {
            cleanStreams();

            if (ex instanceof DownloadException) {
                throw (DownloadException) ex;
            } else {
                throw new DownloadException(ex);
            }
        }
    }

    public void run() throws DownloadException {
        DownloadException exception = null;

        try {
            connectAndDownload();
        } catch (DownloadException ex) {
            exception = ex;
        }

        if (responseCode == null || exception != null) { // if there is an exception, a download exception is thrown
            throw new DownloadException("Unable to reach the page " + url + "\tErrorCode : " + responseCode + "\t" + exception, exception);
        }
    }
}
