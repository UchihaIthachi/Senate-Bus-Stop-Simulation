import java.util.concurrent.Semaphore;

/**
 * BusStop models a shared bus stop where riders wait and buses arrive.
 * Synchronization between riders and buses is managed using semaphores
 * and a mutex to protect shared state.
 */
public class BusStop {
    private static final int BUS_CAPACITY = 50;
    private int waitingRiderCount = 0;
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore waitingRiders = new Semaphore(0);
    private final Semaphore allAboard = new Semaphore(0);

    /**
     * Called by a rider when they arrive at the bus stop. Increments
     * the count of waiting riders under protection of a mutex.
     *
     * @param id identifier of the rider (for logging purposes)
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void riderArrives(int id) throws InterruptedException {
        mutex.acquire();
        waitingRiderCount++;
        System.out.println("Rider " + id + " arrives, waiting=" + waitingRiderCount);
        mutex.release();
    }

    /**
     * Called by a rider when boarding the bus. Riders call this only after
     * receiving a permit from waitingRiders.
     *
     * @param id identifier of the rider
     * @throws InterruptedException if the thread is interrupted
     */
    public void boardBus(int id) throws InterruptedException {
        waitingRiders.acquire();
        System.out.println("Rider " + id + " boarding");
        allAboard.release();
    }

    /**
     * Called by a bus when it arrives. Determines how many riders can board
     * and releases that many permits on waitingRiders.
     *
     * @param id identifier of the bus
     * @return number of riders that are allowed to board
     * @throws InterruptedException if interrupted while waiting
     */
    public int busArrives(int id) throws InterruptedException {
        mutex.acquire();
        int boarding = Math.min(waitingRiderCount, BUS_CAPACITY);
        if (boarding > 0) {
            System.out.println(
                    "Bus " + id + " arrives: waiting " + waitingRiderCount + ", boarding " + boarding);
            // release permits equal to number of riders boarding
            for (int i = 0; i < boarding; i++) {
                waitingRiders.release();
            }
        } else {
            System.out.println("Bus " + id + " arrives: no riders, departing");
        }
        mutex.release();
        return boarding;
    }

    /**
     * Called by a bus after riders have boarded. Waits for all riders to
     * signal that they have boarded, then decrements waiting rider count
     * accordingly and logs departure.
     *
     * @param id      identifier of the bus
     * @param boarded number of riders that boarded
     * @throws InterruptedException if interrupted while waiting
     */
    public void busDeparts(int id, int boarded) throws InterruptedException {
        // wait for all boarded riders to signal they are on board
        for (int i = 0; i < boarded; i++) {
            allAboard.acquire();
        }
        mutex.acquire();
        waitingRiderCount -= boarded;
        System.out.println(
                "Bus " + id + " departs: boarded " + boarded + ", waiting=" + waitingRiderCount);
        mutex.release();
    }
}