/**
 * Represents a bus thread. Each bus arrives at the stop and manages the boarding process.
 */
public class Bus implements Runnable {
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    /**
     * Constructor for a Bus.
     * @param busStop The shared BusStop object.
     */
    public Bus(BusStop busStop) {
        this.busStop = busStop;
        // Atomically assign a unique ID to the bus.
        synchronized (Bus.class) {
            this.id = nextId++;
        }
    }

    /**
     * The bus's execution logic.
     * The bus arrives and handles the entire boarding and departure sequence.
     */
    @Override
    public void run() {
        try {
            // Delegate the entire arrival-to-departure logic to the BusStop.
            busStop.depart(id);
        } catch (InterruptedException e) {
            // Preserve the interrupted status before terminating.
            Thread.currentThread().interrupt();
        }
    }
}