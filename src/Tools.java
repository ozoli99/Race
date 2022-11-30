import java.util.concurrent.ThreadLocalRandom;

public class Tools {
    public static int getRandom(int min, int max) { return ThreadLocalRandom.current().nextInt(min, max); }

    public static void doNothing(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ex) { ex.printStackTrace(); }
    }
}
