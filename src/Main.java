import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Entry point for the bus and rider simulation.
 * This class starts generator threads that create rider and bus threads
 * according to exponential inter-arrival times and waits for them to finish.
 */
public class Main {

    public static final long START_TIME = System.nanoTime();

    public static void main(String[] args) throws InterruptedException {
        // --- Simulation Parameters ---
        int totalRiders = 100;
        int fixedBuses = 0; // Default to 0, implying dynamic mode unless specified
        boolean dynamicBuses = false;
        long meanRiderMs = 30_000;
        long meanBusMs = 1_200_000;
        Long seed = null;

        // --- Parse Command-Line Arguments ---
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--riders":
                        totalRiders = Integer.parseInt(args[++i]);
                        break;
                    case "--buses":
                        fixedBuses = Integer.parseInt(args[++i]);
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
                    case "--dynamicBuses":
                        dynamicBuses = true;
                        break;
                    default:
                        System.err.println("Unknown argument: " + args[i]);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            System.exit(1);
        }

        // If --buses is not specified or is 0, enable dynamic mode
        if (fixedBuses == 0) {
            dynamicBuses = true;
        }

        // Create final copies for use in lambdas
        final long finalMeanRiderMs = meanRiderMs;
        final long finalMeanBusMs = meanBusMs;
        final int finalFixedBuses = fixedBuses;
        final boolean isDynamic = dynamicBuses;
        final int targetRiders = totalRiders;

        // --- Initialize RNG and Sleep Functions ---
        Random rng = (seed != null) ? new Random(seed) : null;
        LongSupplier riderSleep = () -> {
            double u = (rng != null ? rng.nextDouble() : ThreadLocalRandom.current().nextDouble());
            return (long) (-finalMeanRiderMs * Math.log(1 - u));
        };
        LongSupplier busSleep = () -> {
            double u = (rng != null ? rng.nextDouble() : ThreadLocalRandom.current().nextDouble());
            return (long) (-finalMeanBusMs * Math.log(1 - u));
        };

        // --- Shared State and Generators ---
        BusStop busStop = new BusStop();
        final AtomicBoolean ridersDoneSpawning = new AtomicBoolean(false);
        final AtomicInteger busCount = new AtomicInteger(0);
        final List<Thread> riderThreads = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> busThreads = Collections.synchronizedList(new ArrayList<>());

        // Rider generator thread
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
            Logger.log("All " + targetRiders + " riders have been generated.");
        }, "Rider-Generator");

        // Bus generator thread
        Thread busGen = new Thread(() -> {
            if (!isDynamic) {
                // Legacy/demo mode: generate a fixed number of buses
                for (int i = 0; i < finalFixedBuses; i++) {
                    try {
                        Thread.sleep(busSleep.getAsLong());
                        Thread busThread = new Thread(new Bus(busStop), "Bus-" + (busCount.incrementAndGet()));
                        busThreads.add(busThread);
                        busThread.start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                return;
            }

            // Dynamic mode: keep sending buses until all riders are boarded
            while (true) {
                if (ridersDoneSpawning.get() && busStop.getTotalBoardedRiders() >= targetRiders) {
                    Logger.log("All riders boarded. Stopping bus generation.");
                    break;
                }
                try {
                    Thread.sleep(busSleep.getAsLong());
                    Thread busThread = new Thread(new Bus(busStop), "Bus-" + (busCount.incrementAndGet()));
                    busThreads.add(busThread);
                    busThread.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Bus-Generator");

        // --- Start Simulation ---
        String mode = isDynamic ? "Dynamic" : "Fixed (" + finalFixedBuses + " buses)";
        Logger.log("Starting simulation. Mode: " + mode);
        riderGen.start();
        busGen.start();

        // --- Wait for Generators to Finish ---
        riderGen.join();
        busGen.join();

        // --- Wait for all active threads to complete ---
        for (Thread t : riderThreads) {
            t.join();
        }
        for (Thread t : busThreads) {
            t.join();
        }

        // --- Print Final Summary ---
        Logger.log("Simulation complete.");
        System.out.println("\n--- Simulation Summary ---");
        System.out.println("Mode: " + mode);
        System.out.println("Buses simulated: " + busCount.get());
        System.out.println("Riders spawned: " + targetRiders);
        System.out.println("Riders boarded: " + busStop.getTotalBoardedRiders());
        System.out.println("Max waiting riders: " + busStop.getMaxWaitingRiders());
        System.out.println("--------------------------");
    }
}