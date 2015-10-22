package lolproxy;

import junit.framework.Assert;
import org.junit.Test;

public class Http429RequestManagerTest {

    @Test
    public void testCanRunRequest() {
        Http429RequestManagerTimeMock http429RequestManagerTimeMock = new Http429RequestManagerTimeMock(3000, 1000);
        http429RequestManagerTimeMock.time = 0;

        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));

        http429RequestManagerTimeMock.time = 1000;
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));

        http429RequestManagerTimeMock.reportHttp429("eune.api.pvp.net", 0);

        http429RequestManagerTimeMock.time = 2000;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        http429RequestManagerTimeMock.time = 3000;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        http429RequestManagerTimeMock.time = 3999;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        http429RequestManagerTimeMock.time = 4000;
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));

        http429RequestManagerTimeMock.time = 5000;
        http429RequestManagerTimeMock.reportHttp429("eune.api.pvp.net", 5);

        http429RequestManagerTimeMock.time = 10999;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));

        http429RequestManagerTimeMock.time = 11000;
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
    }

    @Test
    public void testCanRunRequestParallel() {
        Http429RequestManagerTimeMock http429RequestManagerTimeMock = new Http429RequestManagerTimeMock(3000, 1000);
        http429RequestManagerTimeMock.time = 0;

        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));

        http429RequestManagerTimeMock.time = 1000;
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));

        http429RequestManagerTimeMock.reportHttp429("eune.api.pvp.net", 0);

        http429RequestManagerTimeMock.time = 2000;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));
        http429RequestManagerTimeMock.time = 3000;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));
        http429RequestManagerTimeMock.time = 3999;
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));
        http429RequestManagerTimeMock.time = 4000;
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));

        http429RequestManagerTimeMock.reportHttp429("euw.api.pvp.net", 0);
        Assert.assertTrue(http429RequestManagerTimeMock.canRunRequest("eune.api.pvp.net"));
        Assert.assertFalse(http429RequestManagerTimeMock.canRunRequest("euw.api.pvp.net"));
    }

    public static class Http429RequestManagerTimeMock extends Http429RequestManager {

        public long time = 0;

        public Http429RequestManagerTimeMock(int minWait, int retryAfterTimeIncrease) {
            super(minWait, retryAfterTimeIncrease);
        }

        @Override
        long getTime() {
            return time;
        }

    }

}
