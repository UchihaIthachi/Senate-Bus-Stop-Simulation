import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BusStop orchestrates the synchronization between Rider and Bus threads
 * using four semaphores to meet the problem's constraints.
 */
public class BusStop {
    private static final int BUS_CAPACITY = 50;

    // --- Semaphores for Synchronization ---

    // Binary semaphore to ensure only one bus can be at the stop at a time.
    private final Semaphore busMutex = new Semaphore(1, true);

    // Binary semaphore to protect access to the shared `waitingRiderCount`.
    // Held for very short periods to prevent blocking.
    private final Semaphore mutex = new Semaphore(1, true);

    // Counting semaphore for riders to wait on. The bus releases permits
    // to signal that waiting riders can start boarding.
    private final Semaphore waitingRiders = new Semaphore(0, true);

    // Counting semaphore for the bus to wait on. Riders release permits
    // after they have boarded, creating a barrier.
    private final Semaphore allAboard = new Semaphore(0, true);

    // --- Shared State ---
    private int waitingRiderCount = 0;
    private int maxWaitingRiders = 0; // For metrics
    private final AtomicInteger totalBoardedRiders = new AtomicInteger(0); // For metrics

    /**
     * A rider arrives at the stop. This method safely increments the waiting count.
     */
    public void riderArrives(int id) throws InterruptedException {
        // Acquire mutex to safely modify the shared waitingRiderCount.
        mutex.acquire();
        try {
            waitingRiderCount++;
            if (waitingRiderCount > maxWaitingRiders) {
                maxWaitingRiders = waitingRiderCount;
            }
            Logger.log("Rider " + id + " arrives, waiting=" + waitingRiderCount);
        } finally {
            mutex.release();
        }
    }

    /**
     * A rider attempts to board a bus.
     * This method blocks until the bus gives a signal, then signals back once boarded.
     */
    public void boardBus(int id) throws InterruptedException {
        // Wait for the bus to release a permit from `waitingRiders`.
        // This acts as a gate, ensuring only riders who were already waiting get on.
        waitingRiders.acquire();
        try {
            Logger.log("Rider " + id + " boarding");
        } finally {
            // Signal to the bus that this rider has finished boarding.
            // This contributes to the `allAboard` barrier.
            allAboard.release();
        }
    }

    /**
     * A bus arrives, manages the boarding process, and departs.
     * This method contains the core synchronization logic.
     */
    public void depart(int id) throws InterruptedException {
        // 1. A bus arrives and acquires the busMutex, ensuring it's the only active bus.
        busMutex.acquire();
        try {
            int boarding;

            // 2. Take a "snapshot" of the waiting riders count. This is a short
            // critical section to determine how many riders will board this bus.
            mutex.acquire();
            try {
                boarding = Math.min(waitingRiderCount, BUS_CAPACITY);
                if (boarding > 0) {
                    Logger.log("Bus " + id + " arrives: waiting " + waitingRiderCount + ", boarding " + boarding);
                } else {
                    // If no riders are waiting, the bus departs immediately.
                    Logger.log("Bus " + id + " arrives: no riders, departing immediately");
                }
            } finally {
                // Release mutex quickly so new riders aren't blocked while this bus boards.
                mutex.release();
            }

            // If no one is waiting, the bus's work is done.
            if (boarding == 0) {
                return;
            }

            // 3. "Open the gate" for the snapshot of waiting riders.
            // This releases 'boarding' number of permits.
            for (int i = 0; i < boarding; i++) {
                waitingRiders.release();
            }

            // 4. Wait for all 'boarding' riders to get on the bus.
            // This acts as a barrier, ensuring the bus doesn't leave early.
            for (int i = 0; i < boarding; i++) {
                allAboard.acquire();
            }

            // 5. Safely update the waiting count now that boarding is complete.
            mutex.acquire();
            try {
                waitingRiderCount -= boarding;
                totalBoardedRiders.addAndGet(boarding);
                Logger.log("Bus " + id + " departs: boarded " + boarding + ", waiting=" + waitingRiderCount);
            } finally {
                mutex.release();
            }
        } finally {
            // 6. The bus departs, releasing the busMutex for the next bus.
            busMutex.release();
        }
    }

    // --- Getter methods for simulation metrics ---
    public int getTotalBoardedRiders() { return totalBoardedRiders.get(); }
    public int getMaxWaitingRiders() { return maxWaitingRiders; }
}