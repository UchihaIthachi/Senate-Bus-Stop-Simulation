/**
 * Rider represents a passenger arriving at the bus stop.
 * Each Rider has a unique id and interacts with the BusStop to
 * wait and board the bus when permitted.
 */
public class Rider implements Runnable {
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    /**
     * Constructs a Rider associated with a given BusStop. Assigns
     * a unique id to each Rider.
     *
     * @param busStop the shared BusStop instance
     */
    public Rider(BusStop busStop) {
        this.busStop = busStop;
        synchronized (Rider.class) {
            this.id = nextId++;
        }
    }

    @Override
    public void run() {
        try {
            busStop.riderArrives(id);
            busStop.boardBus(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}