package lolproxy;

public class Main {

    public static void main(String args[]) {

        int port = findCommandLineValue(args, "port", 23830);
        int proxyTimeout = findCommandLineValue(args, "proxyTimeout", 60 * 1000);
        int minWaitAfter429 = findCommandLineValue(args, "minWaitAfter429", 3000);
        int retryAfterTimeIncreaseAfter429 = findCommandLineValue(args, "retryAfterTimeIncreaseAfter429", 1000);
        int maxRequestsPerSecond = findCommandLineValue(args, "maxRequestsPerSecond", 220);
        int threadsCount = findCommandLineValue(args, "threadsCount", 100);

        LolProxy lolProxy = new LolProxy(port, proxyTimeout, minWaitAfter429, retryAfterTimeIncreaseAfter429, maxRequestsPerSecond, true, threadsCount);
        lolProxy.start();
    }

    private static int findCommandLineValue(String args[], String argName, int defaultValue) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (("-" + argName).equalsIgnoreCase(args[i])) {
                    if (i < args.length - 1) {
                        return Integer.parseInt(args[i + 1]);
                    } else {
                        usage();
                        return -1;
                    }
                }
            }

            return defaultValue;
        } catch (NumberFormatException ex) {
            usage();
            return -1;
        }
    }

    private static void usage() {
        System.err.println("Usage: java -jar lolproxy.jar [-port <Port>] [-proxyTimeout <ProxyTimeout>] [-minWaitAfter429 <MinWaitAfter429>] [-retryAfterTimeIncreaseAfter429 <RetryAfterTimeIncreaseAfter429>] [-maxRequestsPerSecond <MaxRequestsPerSecond>] [-threadsCount <ThreadsCount>]");
        System.exit(1);
    }
}
