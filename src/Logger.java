/**
 * A simple logging utility to print timestamped messages from threads.
 * This helps in tracing the order of concurrent events during the simulation.
 */
public class Logger {
    /**
     * Prints a message to the console, prepended with the elapsed simulation time
     * and the name of the calling thread.
     *
     * @param message The message to log.
     */
    public static void log(String message) {
        // Calculate elapsed time in seconds since the simulation started.
        double s = (System.nanoTime() - Main.START_TIME) / 1_000_000_000.0;
        String thread = Thread.currentThread().getName();
        // System.out.printf is thread-safe.
        System.out.printf("[t=%.3fs][%s] %s%n", s, thread, message);
    }
}