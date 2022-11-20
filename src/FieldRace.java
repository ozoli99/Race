import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FieldRace {
    private static final int PLAYER_COUNT = 10;
    private static final int CHECKPOINT_COUNT = 10;

    private static AtomicBoolean isOn = new AtomicBoolean(true);
    private static ConcurrentHashMap<Integer, Integer> scores = new ConcurrentHashMap<>(PLAYER_COUNT);
    private static List<AtomicInteger> checkpointScores = new ArrayList<>(PLAYER_COUNT);
    private static List<BlockingQueue<AtomicInteger>> checkpointQueues = Collections.synchronizedList(new ArrayList<>(CHECKPOINT_COUNT));

    public static class Field extends Thread {
        private static BlockingQueue<AtomicInteger> _blockingQueue = new ArrayBlockingQueue<AtomicInteger>(CHECKPOINT_COUNT);
        private int _index;

        public Field(int index, int score) { _index = index; }

        @Override
        public void run() {
            while (isOn.get()) {
                _blockingQueue = checkpointQueues.get(_index);
            }
        }
    }

    public static class Player extends Thread {
        @Override
        public void run() {

        }
    }

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(PLAYER_COUNT + CHECKPOINT_COUNT + 1);

        for (int i = 0; i < PLAYER_COUNT; ++i) {
            executorService.submit(() -> {

            });
        }
        for (int i = 0; i < CHECKPOINT_COUNT; ++i) {
            executorService.submit(() -> {

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
    }
}