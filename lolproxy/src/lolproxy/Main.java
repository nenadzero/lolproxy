package lolproxy;

public class Main {

    public static void main(String args[]) {
        int port = 23830;

        LolProxy lolProxy = new LolProxy(port, 60 * 1000, 3000, 1000, 220, true, 100);
        lolProxy.start();
    }
}
