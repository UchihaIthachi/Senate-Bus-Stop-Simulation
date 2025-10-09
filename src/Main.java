import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * The main driver for the Senate Bus Problem simulation.
 *
 * This class is responsible for:
 * 1.  Parsing command-line arguments to configure simulation parameters such as
 *     the number of riders, buses, inter-arrival times, and RNG seed.
 * 2.  Initializing the shared {@link BusStop} object and other state variables.
 * 3.  Creating and starting two primary generator threads:
 *     - A Rider Generator: Spawns new {@link Rider} threads at random intervals
 *       following a Poisson process.
 *     - A Bus Generator: Spawns new {@link Bus} threads, also following a
 *       Poisson process. This generator can operate in two modes:
 *       a) Fixed Mode: A set number of buses are created.
 *       b) Dynamic Mode (default): Buses are generated indefinitely until all
 *          riders have been served, preventing rider starvation.
 * 4.  Waiting for all threads to complete their execution and then printing a
 *     final summary of the simulation results.
 */
public class Main {

    // Global start time for timestamped logging.
    public static final long START_TIME = System.nanoTime();

    public static void main(String[] args) throws InterruptedException {
        // --- Default Simulation Parameters ---
        int totalRiders = 100;
        int fixedBusCount = 0; // A value > 0 triggers fixed bus mode.
        boolean dynamicMode = true;
        long meanRiderMs = 30_000;  // 30 seconds
        long meanBusMs = 1_200_000; // 20 minutes
        Long seed = null; // No seed by default, for non-deterministic runs.

        // --- Simple Command-Line Argument Parsing ---
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--riders":
                        totalRiders = Integer.parseInt(args[++i]);
                        break;
                    case "--buses":
                        fixedBusCount = Integer.parseInt(args[++i]);
                        if (fixedBusCount > 0) {
                            dynamicMode = false; // Override default if a bus count is specified.
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
            System.err.println("Usage: java Main [--riders N] [--buses N] [--meanRiderMs M] [--meanBusMs M] [--seed S]");
            System.exit(1);
        }

        // --- Create final (or effectively final) copies for use in lambdas ---
        // Local variables used inside a lambda expression must be final or
        // effectively final. We create explicit final copies for clarity.
        final long finalMeanRiderMs = meanRiderMs;
        final long finalMeanBusMs = meanBusMs;
        final int finalFixedBuses = fixedBusCount;
        final boolean isDynamic = dynamicMode;
        final int targetRiders = totalRiders;

        // --- Initialize RNG and Sleep Functions for Poisson Process Simulation ---
        // If a seed is provided, use a single Random instance for deterministic behavior.
        // Otherwise, use the thread-safe ThreadLocalRandom for better performance.
        final Random rng = (seed != null) ? new Random(seed) : null;
        // Lambdas to generate sleep times based on an exponential distribution,
        // simulating a Poisson arrival process.
        LongSupplier riderSleep = () -> {
            double u = (rng != null ? rng.nextDouble() : ThreadLocalRandom.current().nextDouble());
            return (long) (-finalMeanRiderMs * Math.log(1 - u));
        };
        LongSupplier busSleep = () -> {
            double u = (rng != null ? rng.nextDouble() : ThreadLocalRandom.current().nextDouble());
            return (long) (-finalMeanBusMs * Math.log(1 - u));
        };

        // --- Shared State and Thread-Safe Collections ---
        final BusStop busStop = new BusStop();
        final AtomicBoolean ridersDoneSpawning = new AtomicBoolean(false);
        final AtomicInteger generatedBusCount = new AtomicInteger(0);
        final List<Thread> riderThreads = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> busThreads = Collections.synchronizedList(new ArrayList<>());

        // --- Rider Generator Thread ---
        // This thread creates and starts all the rider threads for the simulation.
        Thread riderGen = new Thread(() -> {
            for (int i = 0; i < targetRiders; i++) {
                try {
                    Thread.sleep(riderSleep.getAsLong());
                    Thread riderThread = new Thread(new Rider(busStop), "Rider-" + (i + 1));
                    riderThreads.add(riderThread);
                    riderThread.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Preserve interrupted status.
                    return;
                }
            }
            ridersDoneSpawning.set(true); // Signal that all riders have been created.
        }, "Rider-Generator");

        // --- Bus Generator Thread ---
        // This thread creates bus threads based on the selected mode.
        Thread busGen = new Thread(() -> {
            if (!isDynamic) {
                // Fixed mode: generate a specific number of buses and then stop.
                // This mode can result in rider starvation if not enough buses are provided.
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
                // Dynamic mode: keep sending buses until all riders have boarded.
                // This mode guarantees liveness for all riders.
                while (true) {
                    // The termination condition: all riders have been spawned AND have successfully boarded.
                    if (ridersDoneSpawning.get() && busStop.getTotalBoardedRiders() >= targetRiders) {
                        break;
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

        // --- Start Simulation ---
        String modeStr = isDynamic ? "Dynamic" : "Fixed (" + finalFixedBuses + " buses)";
        Logger.log("Starting simulation. Mode: " + modeStr + ", Riders: " + targetRiders);
        riderGen.start();
        busGen.start();

        // --- Wait for All Threads to Complete ---
        // First, wait for the generator threads to finish their work.
        riderGen.join();
        busGen.join();
        // Then, ensure all spawned rider and bus threads have also completed.
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