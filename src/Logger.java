/**
 * A simple, centralized logging utility for the simulation.
 *
 * This class provides a static method to print timestamped messages to the
 * console. It ensures that all log output is consistent in format, showing
 * the elapsed time since the simulation began and the name of the thread
 * that generated the message. This helps in debugging and understanding the
 * sequence of concurrent events.
 */
public class Logger {
    /**
     * Prints a formatted message to standard output.
     *
     * The log entry includes:
     * 1.  A timestamp in seconds (e.g., "[t=1.234s]") representing the
     *     elapsed time since {@link Main#START_TIME}.
     * 2.  The name of the current thread (e.g., "[Rider-1]").
     * 3.  The log message itself.
     *
     * This method is thread-safe because {@code System.out.printf} is
     * inherently synchronized.
     *
     * @param message The message to be logged.
     */
    public static void log(String message) {
        // Calculate elapsed time in seconds with millisecond precision.
        double s = (System.nanoTime() - Main.START_TIME) / 1_000_000_000.0;
        // Get the name of the currently executing thread.
        String thread = Thread.currentThread().getName();
        // Print the formatted string to the console.
        System.out.printf("[t=%.3fs][%s] %s%n", s, thread, message);
    }
}