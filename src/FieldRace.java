import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FieldRace {
    private static final int PLAYER_COUNT = 10;
    private static final int CHECKPOINT_COUNT = 5;

    private static AtomicBoolean isOn = new AtomicBoolean(true);

    private static ConcurrentHashMap<Integer, Integer> scores = new ConcurrentHashMap<>();
    static {
        for (int i = 0; i < PLAYER_COUNT; i++) { scores.put(i, 0); }
    }
    private static List<AtomicInteger> checkpointScores = new ArrayList<>(PLAYER_COUNT);
    private static List<BlockingQueue<AtomicInteger>> checkpointQueues = Collections.synchronizedList(new ArrayList<>(CHECKPOINT_COUNT));
    static {
        for (int i = 0; i < CHECKPOINT_COUNT; i++) { checkpointQueues.add(new ArrayBlockingQueue<>(2)); }
    }

    public static class Field extends Thread {
        private int index;

        public Field(int index) { this.index = index; }

        @Override
        public void run() {
            while (isOn.get()) {
                try {
                    System.out.println("Field" + index + ": Waits for player to reach the field");
                    AtomicInteger checkpointScore = checkpointQueues.get(index).poll(2, TimeUnit.SECONDS);
                    System.out.println("Field" + index + ": Player reached the field");
                    synchronized (checkpointScore) {
                        checkpointScore.set(Tools.getRandom(10, 100));
                        System.out.println("Field" + index + ": Set player's points to " + checkpointScore.get());
                        checkpointScore.notify();
                    }
                } catch (InterruptedException ex) { ex.printStackTrace(); }
            }
        }
    }

    public static class Player extends Thread {
        private int id;

        public Player(int id) { this.id = id; }

        @Override
        public void run() {
            while (isOn.get()) {
                int fieldIdx = Tools.getRandom(0, CHECKPOINT_COUNT);
                System.out.println("(Player " + id + " chose checkpoint " + fieldIdx + " and goes there...)");
                Tools.doNothing(Tools.getRandom(500, 2000));
                AtomicInteger checkpointScore = checkpointScores.get(id);
                synchronized (checkpointScore) {
                    try {
                        System.out.println("(Player " + id + " starts competing at checkpoint " + fieldIdx + ")");
                        checkpointQueues.get(fieldIdx).put(checkpointScore);
                        while (checkpointScore.get() == 0) { checkpointScore.wait(3000); }
                        System.out.println("(Player " + id + " got " + checkpointScore.get() + " points at checkpoint " + fieldIdx + ")");
                        scores.replace(id, scores.get(id) + checkpointScore.get());
                        checkpointScore.set(0);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(PLAYER_COUNT + CHECKPOINT_COUNT + 1);

        for (int i = 0; i < PLAYER_COUNT; ++i) {
            int finalI = i;
            executorService.submit(() -> {
                new Player(finalI).run();
            });
        }
        for (int i = 0; i < CHECKPOINT_COUNT; ++i) {
            int finalI = i;
            executorService.submit(() -> {
                new Field(finalI).run();
            });
        }
        executorService.submit(() -> {
            String playersScores = "Scores: [";

            Iterator<ConcurrentHashMap.Entry<Integer, Integer> > itr = scores.entrySet().iterator();
            while (itr.hasNext()) {
                ConcurrentHashMap.Entry<Integer, Integer> entry = itr.next();
                playersScores += entry.getKey() + "=" + entry.getValue() + ", ";
            }

            playersScores += "]";
            System.out.println(playersScores);
        });

        isOn.set(false);

        executorService.shutdown();
        try {
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) { ex.printStackTrace(); }
        executorService.shutdownNow();
    }
}