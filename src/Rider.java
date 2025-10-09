/**
 * Represents a single rider (passenger) in the simulation.
 *
 * Each Rider object is designed to be run on its own thread. Its primary role
 * is to interact with the shared {@link BusStop} to simulate the process of
 * arriving at the stop, waiting for a bus, and then boarding it. The core
 * synchronization logic is delegated entirely to the {@code BusStop}.
 */
public class Rider implements Runnable {
    // A static counter to ensure each rider gets a unique ID.
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    /**
     * Constructs a new Rider.
     *
     * A unique ID is assigned to the rider upon creation using a synchronized
     * block to ensure thread-safe instantiation if riders were ever created
     * by multiple threads simultaneously.
     *
     * @param busStop The shared {@link BusStop} instance that this rider will
     *                interact with.
     */
    public Rider(BusStop busStop) {
        this.busStop = busStop;
        // Synchronize on the class object to ensure atomic ID assignment.
        synchronized (Rider.class) {
            this.id = nextId++;
        }
    }

    /**
     * The main execution logic for a rider thread.
     *
     * The rider performs two main actions:
     * 1.  Calls {@link BusStop#riderArrives(int)} to announce their arrival and
     *     increment the waiting count.
     * 2.  Calls {@link BusStop#boardBus(int)}, which blocks until a bus permits
     *     the rider to board.
     *
     * Any {@link InterruptedException} is handled by re-interrupting the thread
     * to ensure a clean shutdown.
     */
    @Override
    public void run() {
        try {
            // Announce arrival and wait for the bus.
            busStop.riderArrives(id);
            // This call will block until the bus releases a permit for this rider.
            busStop.boardBus(id);
        } catch (InterruptedException e) {
            // Restore the interrupted status and allow the thread to terminate.
            Thread.currentThread().interrupt();
        }
    }
}