import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the synchronization of buses and riders at a single bus stop.
 *
 * This class uses a combination of four semaphores to orchestrate the complex
 * interactions required by the Senate Bus Problem. The design prioritizes
 * liveness and deadlock-avoidance by using a two-level locking strategy and
 * minimizing the scope of critical sections.
 *
 * The core components of the synchronization strategy are:
 * 1.  `busMutex`: A binary semaphore that ensures only one bus can be at the
 *     "boarding" stage at any time. This serializes bus arrivals and prevents
 *     multiple buses from competing for the same group of waiting riders.
 *
 * 2.  `mutex`: A binary semaphore that provides exclusive access to the shared
 *     `waitingRiderCount`. This lock is held for very short durations, allowing
 *     new riders to arrive and update the count even while a bus is present at
 *     the stop (but not actively modifying the count).
 *
 * 3.  `waitingRiders`: A counting semaphore used as a gate for riders. Riders
 *     wait on this semaphore after arriving. The bus releases a specific number
 *     of permits (up to 50) to allow a batch of waiting riders to proceed with
 *     boarding. This ensures no new riders can board once the bus has arrived.
 *
 * 4.  `allAboard`: A counting semaphore that acts as a barrier. The bus waits
 *     on this semaphore until every rider who was permitted to board has
 *     signaled their boarding. This ensures the bus does not depart prematurely.
 */
public class BusStop {
    private static final int BUS_CAPACITY = 50;

    // Ensures only ONE bus is actively being served at the stop at a time.
    // This semaphore is held for the entire duration of a bus's stop, from arrival
    // to departure. It does NOT block new riders from arriving at the stop.
    private final Semaphore busMutex = new Semaphore(1, true);

    // Protects short critical sections involving shared counters (`waitingRiderCount`).
    // This lock is acquired and released quickly to prevent blocking rider/bus threads
    // for extended periods, thus avoiding deadlocks.
    private final Semaphore mutex = new Semaphore(1, true);

    // A "gate" for waiting riders. Riders must acquire a permit to board.
    // The bus releases a batch of permits to let a fixed group of riders through.
    private final Semaphore waitingRiders = new Semaphore(0, true);

    // A "barrier" for the bus. The bus waits until all boarding riders have
    // released a permit, ensuring everyone is on board before it departs.
    private final Semaphore allAboard = new Semaphore(0, true);

    // --- Shared State & Metrics ---

    // The number of riders currently waiting at the bus stop.
    private int waitingRiderCount = 0;
    // Tracks the peak number of waiting riders for statistical purposes.
    private int maxWaitingRiders = 0;
    // A thread-safe counter for the total number of riders who have boarded across all buses.
    private final AtomicInteger totalBoardedRiders = new AtomicInteger(0);

    /**
     * A rider arrives at the bus stop.
     *
     * This method increments the shared `waitingRiderCount` within a critical
     * section protected by `mutex`. The lock is released immediately after,
     * allowing other riders to arrive concurrently without being blocked.
     *
     * @param id The unique ID of the arriving rider.
     * @throws InterruptedException if the thread is interrupted.
     */
    public void riderArrives(int id) throws InterruptedException {
        mutex.acquire();
        try {
            waitingRiderCount++;
            if (waitingRiderCount > maxWaitingRiders) {
                maxWaitingRiders = waitingRiderCount; // Update peak metric
            }
            Logger.log("Rider " + id + " arrives, waiting=" + waitingRiderCount);
        } finally {
            mutex.release();
        }
    }

    /**
     * A rider waits to board the bus and then signals that they are on board.
     *
     * The rider first waits to acquire a permit from the `waitingRiders` semaphore.
     * A permit is only released by a bus for a specific batch of riders. Once the
     * permit is acquired, the rider is considered "boarding" and immediately
     * releases a permit on the `allAboard` semaphore to signal to the bus that
     * they are on board. This is a classic rendezvous pattern.
     *
     * @param id The unique ID of the boarding rider.
     * @throws InterruptedException if the thread is interrupted.
     */
    public void boardBus(int id) throws InterruptedException {
        // 1. Wait for the bus to open the gate (release a permit).
        waitingRiders.acquire();
        try {
            // 2. Announce boarding.
            Logger.log("Rider " + id + " boarding");
        } finally {
            // 3. Signal to the bus that this rider is on board.
            allAboard.release();
        }
    }

    /**
     * Handles the entire lifecycle of a bus at the stop: arrival, boarding, and departure.
     *
     * This method orchestrates the synchronization:
     * 1.  Acquires `busMutex` to ensure it is the only bus at the stop.
     * 2.  Acquires `mutex` to take a "snapshot" of `waitingRiderCount`. This determines
     *     the number of riders to board (`boarding`), which is capped at `BUS_CAPACITY`.
     *     `mutex` is released immediately after, allowing new riders to arrive without delay.
     * 3.  If `boarding > 0`, it releases exactly `boarding` permits on `waitingRiders`,
     *     opening the gate for the waiting riders.
     * 4.  It then waits to acquire `boarding` permits from `allAboard`, ensuring it does
     *     not depart until every permitted rider has boarded.
     * 5.  Finally, it acquires `mutex` again to update the `waitingRiderCount` and
     *     `totalBoardedRiders`, then releases `busMutex` to allow the next bus to arrive.
     *
     * @param id The unique ID of the bus.
     * @throws InterruptedException if the thread is interrupted.
     */
    public void depart(int id) throws InterruptedException {
        // 1. Acquire exclusive access to the bus stop. Only one bus at a time.
        busMutex.acquire();
        try {
            int boarding;

            // 2. Take a snapshot of the number of waiting riders. This is a short
            // critical section to ensure atomicity of the read.
            mutex.acquire();
            try {
                boarding = Math.min(waitingRiderCount, BUS_CAPACITY);
                if (boarding > 0) {
                    Logger.log("Bus " + id + " arrives: waiting " + waitingRiderCount + ", boarding " + boarding);
                } else {
                    Logger.log("Bus " + id + " arrives: no riders, departing immediately");
                }
            } finally {
                mutex.release(); // Allow new riders to arrive while this bus boards.
            }

            // If no one is waiting, the bus leaves immediately.
            if (boarding == 0) {
                return;
            }

            // 3. Open the gate for the exact number of riders determined in the snapshot.
            // This prevents riders who arrive *after* the bus from boarding.
            for (int i = 0; i < boarding; i++) {
                waitingRiders.release();
            }

            // 4. Wait for all permitted riders to get on board. This is the barrier.
            for (int i = 0; i < boarding; i++) {
                allAboard.acquire();
            }

            // 5. Update the shared state now that boarding is complete.
            mutex.acquire();
            try {
                waitingRiderCount -= boarding;
                totalBoardedRiders.addAndGet(boarding);
                Logger.log("Bus " + id + " departs: boarded " + boarding + ", waiting=" + waitingRiderCount);
            } finally {
                mutex.release();
            }
        } finally {
            // 6. Release the bus stop for the next bus.
            busMutex.release();
        }
    }

    // --- Getter methods for simulation metrics ---

    public int getTotalBoardedRiders() { return totalBoardedRiders.get(); }
    public int getMaxWaitingRiders() { return maxWaitingRiders; }
}