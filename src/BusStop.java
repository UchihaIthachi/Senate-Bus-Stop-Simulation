import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class BusStop {
    private static final int BUS_CAPACITY = 50;

    // Ensure ONE bus is serviced at a time (does not block rider arrivals)
    private final Semaphore busMutex = new Semaphore(1, true);

    // Protects waitingRiderCount + maxWaitingRiders
    private final Semaphore mutex = new Semaphore(1, true);

    // Boarding / barrier semaphores
    private final Semaphore waitingRiders = new Semaphore(0, true);
    private final Semaphore allAboard = new Semaphore(0, true);

    private int waitingRiderCount = 0;
    private int maxWaitingRiders = 0;
    private final AtomicInteger totalBoardedRiders = new AtomicInteger(0);

    public void riderArrives(int id) throws InterruptedException {
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

    public void boardBus(int id) throws InterruptedException {
        // Only riders that receive a permit can proceed
        waitingRiders.acquire();
        try {
            Logger.log("Rider " + id + " boarding");
        } finally {
            // Signal the bus that one rider has boarded
            allAboard.release();
        }
    }

    /**
     * One-stop bus handler: arrival -> allow up to 50 riders -> wait -> depart.
     * Critical section is minimized so rider arrivals aren't blocked.
     */
    public void depart(int id) throws InterruptedException {
        busMutex.acquire(); // ensure only one bus is being served
        try {
            int boarding;

            // Snapshot how many can board now (freeze the batch size)
            mutex.acquire();
            try {
                boarding = Math.min(waitingRiderCount, BUS_CAPACITY);
                if (boarding > 0) {
                    Logger.log("Bus " + id + " arrives: waiting " + waitingRiderCount + ", boarding " + boarding);
                } else {
                    Logger.log("Bus " + id + " arrives: no riders, departing immediately");
                }
            } finally {
                mutex.release();
            }

            if (boarding == 0) {
                return; // nothing to do
            }

            // Allow exactly 'boarding' riders to proceed (permits)
            for (int i = 0; i < boarding; i++) {
                waitingRiders.release();
            }

            // Wait until all those riders have boarded
            for (int i = 0; i < boarding; i++) {
                allAboard.acquire();
            }

            // Now deduct those riders from the queue and depart
            mutex.acquire();
            try {
                waitingRiderCount -= boarding;
                totalBoardedRiders.addAndGet(boarding);
                Logger.log("Bus " + id + " departs: boarded " + boarding + ", waiting=" + waitingRiderCount);
            } finally {
                mutex.release();
            }
        } finally {
            busMutex.release();
        }
    }

    public int getTotalBoardedRiders() { return totalBoardedRiders.get(); }
    public int getMaxWaitingRiders() { return maxWaitingRiders; }
}