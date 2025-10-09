/**
 * Represents a single bus in the simulation.
 *
 * Each Bus object is designed to be run on its own thread. Its purpose is to
 * arrive at the shared {@link BusStop} and manage the boarding process for a
 * batch of waiting riders. All complex synchronization logic is handled by the
 * {@link BusStop#depart(int)} method, which this class calls.
 */
public class Bus implements Runnable {
    // A static counter to ensure each bus gets a unique ID.
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    /**
     * Constructs a new Bus.
     *
     * A unique ID is assigned to the bus upon creation. The assignment is
     * done within a synchronized block to ensure thread-safety, which is crucial
     * as the main simulation driver may spawn bus threads from a generator thread.
     *
     * @param busStop The shared {@link BusStop} instance that this bus will
     *                interact with.
     */
    public Bus(BusStop busStop) {
        this.busStop = busStop;
        // Synchronize on the class object to ensure atomic ID assignment.
        synchronized (Bus.class) {
            this.id = nextId++;
        }
    }

    /**
     * The main execution logic for a bus thread.
     *
     * The thread's only action is to call the {@link BusStop#depart(int)} method,
     * which encapsulates the entire bus lifecycle at the stop: arriving,
     * signaling riders to board, waiting for them, and departing.
     *
     * If the thread is interrupted during its operation (e.g., while waiting),
     * it restores the interrupted status and terminates cleanly.
     */
    @Override
    public void run() {
        try {
            // Delegate the entire arrival, boarding, and departure process.
            busStop.depart(id);
        } catch (InterruptedException e) {
            // Restore the interrupted status and allow the thread to terminate.
            Thread.currentThread().interrupt();
        }
    }
}