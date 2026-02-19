package tools;

import java.time.Duration;
import java.util.concurrent.*;

public class Utilities {
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignored) {}
    }

    public static boolean waitUntil(Callable<Boolean> task) {
        return waitUntil(task, Duration.ofMinutes(1));
    }

    public static boolean waitUntil(Callable<Boolean> task, Duration duration) {
        if(task == null) return true;

        long deadline = System.currentTimeMillis() + duration.toMillis();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            while (System.currentTimeMillis() < deadline) {
                Future<Boolean> future = executor.submit(task);
                try {
                    long remainingTime = deadline - System.currentTimeMillis();
                    if(remainingTime <= 0) break;

                    boolean status = future.get(remainingTime, TimeUnit.MILLISECONDS);
                    if(status) return true;

                    Utilities.sleep(Duration.ofMillis(250));
                } catch (TimeoutException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Exception while waiting for task: " + e.getMessage());
                    break;
                }
            }
        } finally {
            executor.shutdown();
        }
        return false;
    }
}
