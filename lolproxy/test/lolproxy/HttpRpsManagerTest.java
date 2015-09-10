package lolproxy;

import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpRpsManagerTest {

    @Test
    public void testAddRequestIfCanHandle() {
        HttpRpsManagerTimeMock httpRpsManagerTimeMock = new HttpRpsManagerTimeMock(5);

        for (int t = 0; t < 500; t += 100) {
            httpRpsManagerTimeMock.time = t;
            Assert.assertTrue(httpRpsManagerTimeMock.addRequestIfCanHandle());
        }

        for (int t = 500; t < 1000; t += 100) {
            httpRpsManagerTimeMock.time = t;
            Assert.assertFalse(httpRpsManagerTimeMock.addRequestIfCanHandle());
        }

        httpRpsManagerTimeMock.time = 1001;
        Assert.assertTrue(httpRpsManagerTimeMock.addRequestIfCanHandle());
        httpRpsManagerTimeMock.time = 1002;
        Assert.assertFalse(httpRpsManagerTimeMock.addRequestIfCanHandle());
        httpRpsManagerTimeMock.time = 1201;
        Assert.assertTrue(httpRpsManagerTimeMock.addRequestIfCanHandle());
    }

    public static class HttpRpsManagerTimeMock extends HttpRpsManager {

        public long time = 0;

        public HttpRpsManagerTimeMock(int maxRequestsPerSecond) {
            super(maxRequestsPerSecond);
        }

        @Override
        long getTime() {
            return time;
        }

    }

}
