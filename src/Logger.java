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
        double elapsedSeconds = (System.nanoTime() - Main.START_TIME) / 1_000_000_000.0;
        System.out.printf("[%t=%.3fs] %s%n", elapsedSeconds, message);
    }
}