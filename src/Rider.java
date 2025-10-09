/**
 * Represents a rider thread. Each rider arrives at the stop and waits to board a bus.
 */
public class Rider implements Runnable {
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    /**
     * Constructor for a Rider.
     * @param busStop The shared BusStop object.
     */
    public Rider(BusStop busStop) {
        this.busStop = busStop;
        // Atomically assign a unique ID to the rider.
        synchronized (Rider.class) {
            this.id = nextId++;
        }
    }

    /**
     * The rider's execution logic.
     * The rider arrives, then waits to board the bus.
     */
    @Override
    public void run() {
        try {
            // 1. Announce arrival at the bus stop.
            busStop.riderArrives(id);
            // 2. Wait for the bus to grant permission to board. This call blocks.
            busStop.boardBus(id);
        } catch (InterruptedException e) {
            // Preserve the interrupted status before terminating.
            Thread.currentThread().interrupt();
        }
    }
}