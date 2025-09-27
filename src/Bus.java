/**
 * Bus represents a bus arriving at the bus stop.
 * Each Bus has a unique id and interacts with the BusStop to
 * allow riders to board and depart when ready.
 */
public class Bus implements Runnable {
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    /**
     * Constructs a Bus associated with a BusStop. Assigns
     * a unique id to each Bus.
     *
     * @param busStop the shared BusStop instance
     */
    public Bus(BusStop busStop) {
        this.busStop = busStop;
        synchronized (Bus.class) {
            this.id = nextId++;
        }
    }

    @Override
    public void run() {
        try {
            // Bus arrives and allows riders to board
            int boarded = busStop.busArrives(id);
            // Simulate boarding time
            if (boarded > 0) {
                Thread.sleep(2000); // 2 seconds
            }
            busStop.busDeparts(id, boarded);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}