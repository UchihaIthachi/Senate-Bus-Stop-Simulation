import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Main class to drive the Senate Bus simulation.
 * It parses arguments, initializes threads for riders and buses with
 * exponential inter-arrival times, and runs the simulation.
 */
public class Main {

    public static final long START_TIME = System.nanoTime();

    public static void main(String[] args) throws InterruptedException {
        // --- Simulation Parameters (with defaults) ---
        int totalRiders = 100;
        int fixedBusCount = 0; // If > 0, triggers fixed mode.
        boolean dynamicMode = true;
        long meanRiderMs = 30_000;  // Corresponds to 30 sec mean inter-arrival
        long meanBusMs = 1_200_000; // Corresponds to 20 min mean inter-arrival
        Long seed = null;

        // --- Parse Command-Line Arguments ---
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--riders":
                        totalRiders = Integer.parseInt(args[++i]);
                        break;
                    case "--buses":
                        fixedBusCount = Integer.parseInt(args[++i]);
                        if (fixedBusCount > 0) {
                            dynamicMode = false; // Fixed bus count overrides dynamic mode.
                        }
                        break;
                    case "--meanRiderMs":
                        meanRiderMs = Long.parseLong(args[++i]);
                        break;
                    case "--meanBusMs":
                        meanBusMs = Long.parseLong(args[++i]);
                        break;
                    case "--seed":
                        seed = Long.parseLong(args[++i]);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            System.exit(1);
        }

        // --- Final copies for use in lambdas ---
        final long finalMeanRiderMs = meanRiderMs;
        final long finalMeanBusMs = meanBusMs;
        final int finalFixedBuses = fixedBusCount;
        final boolean isDynamic = dynamicMode;
        final int targetRiders = totalRiders;

        // --- RNG and Arrival Time Simulation ---
        // Lambdas to generate sleep times based on an exponential distribution,
        // simulating a Poisson arrival process as required.
        Random rng = (seed != null) ? new Random(seed) : null;
        LongSupplier riderSleep = () -> {
            double u = (rng != null ? rng.nextDouble() : ThreadLocalRandom.current().nextDouble());
            return (long) (-finalMeanRiderMs * Math.log(1 - u));
        };
        LongSupplier busSleep = () -> {
            double u = (rng != null ? rng.nextDouble() : ThreadLocalRandom.current().nextDouble());
            return (long) (-finalMeanBusMs * Math.log(1 - u));
        };

        // --- Shared State ---
        BusStop busStop = new BusStop();
        final AtomicBoolean ridersDoneSpawning = new AtomicBoolean(false);
        final AtomicInteger generatedBusCount = new AtomicInteger(0);
        final List<Thread> riderThreads = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> busThreads = Collections.synchronizedList(new ArrayList<>());

        // --- Rider Generator Thread ---
        // Creates a fixed number of rider threads with simulated arrival times.
        Thread riderGen = new Thread(() -> {
            for (int i = 0; i < targetRiders; i++) {
                try {
                    Thread.sleep(riderSleep.getAsLong());
                    Thread riderThread = new Thread(new Rider(busStop), "Rider-" + (i + 1));
                    riderThreads.add(riderThread);
                    riderThread.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            ridersDoneSpawning.set(true);
        }, "Rider-Generator");

        // --- Bus Generator Thread ---
        // Creates bus threads according to the selected mode (fixed or dynamic).
        Thread busGen = new Thread(() -> {
            if (!isDynamic) {
                // Fixed mode: generate a specific number of buses.
                for (int i = 0; i < finalFixedBuses; i++) {
                    try {
                        Thread.sleep(busSleep.getAsLong());
                        Thread busThread = new Thread(new Bus(busStop), "Bus-" + (generatedBusCount.incrementAndGet()));
                        busThreads.add(busThread);
                        busThread.start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } else {
                // Dynamic mode: keep sending buses until all riders are boarded.
                // This prevents rider starvation as per the lab notes.
                while (true) {
                    if (ridersDoneSpawning.get() && busStop.getTotalBoardedRiders() >= targetRiders) {
                        break; // Exit condition for dynamic mode.
                    }
                    try {
                        Thread.sleep(busSleep.getAsLong());
                        Thread busThread = new Thread(new Bus(busStop), "Bus-" + (generatedBusCount.incrementAndGet()));
                        busThreads.add(busThread);
                        busThread.start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "Bus-Generator");

        // --- Start and Wait for Simulation to Complete ---
        String modeStr = isDynamic ? "Dynamic" : "Fixed (" + finalFixedBuses + " buses)";
        Logger.log("Starting simulation. Mode: " + modeStr);
        riderGen.start();
        busGen.start();

        // Wait for all threads to finish before printing the summary.
        riderGen.join();
        busGen.join();
        for (Thread t : riderThreads) t.join();
        for (Thread t : busThreads) t.join();

        // --- Print Final Summary ---
        Logger.log("Simulation complete.");
        System.out.println("\n--- Simulation Summary ---");
        System.out.println("Mode: " + modeStr);
        System.out.println("Buses simulated: " + generatedBusCount.get());
        System.out.println("Riders spawned: " + targetRiders);
        System.out.println("Riders boarded: " + busStop.getTotalBoardedRiders());
        System.out.println("Max waiting riders: " + busStop.getMaxWaitingRiders());
        System.out.println("--------------------------");
    }
}