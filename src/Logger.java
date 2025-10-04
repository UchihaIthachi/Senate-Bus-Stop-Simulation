/**
 * A simple utility for timestamped logging.
 */
public class Logger {
    /**
     * Prints a message to the console, prepended with a timestamp
     * indicating the elapsed time since the simulation started.
     *
     * @param message The message to log.
     */
    public static void log(String message) {
        double s = (System.nanoTime() - Main.START_TIME) / 1_000_000_000.0;
        String thread = Thread.currentThread().getName();
        System.out.printf("[t=%.3fs][%s] %s%n", s, thread, message);
    }
}