import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entry point for the bus and rider simulation.
 * This class starts generator threads that create rider and bus threads
 * according to exponential inter-arrival times and waits for them to finish.
 */
public class Main {

    // Simulation parameters with default values from the spec
    private static int totalRiders = 100;
    private static int totalBuses = 3;
    private static double meanRiderInterval = 30000.0; // 30 seconds
    private static double meanBusInterval = 1200000.0; // 20 minutes
    private static Long rngSeed = null; // Use non-deterministic RNG by default

    // Shared state
    public static final long START_TIME = System.nanoTime();
    private static Random rng = null;

    /**
     * Parses command-line arguments to override simulation parameters.
     * Example: --riders 120 --buses 3 --meanRiderMs 30000 --meanBusMs 1200000 --seed 42
     */
    private static void parseArgs(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--riders":
                        totalRiders = Integer.parseInt(args[++i]);
                        break;
                    case "--buses":
                        totalBuses = Integer.parseInt(args[++i]);
                        break;
                    case "--meanRiderMs":
                        meanRiderInterval = Double.parseDouble(args[++i]);
                        break;
                    case "--meanBusMs":
                        meanBusInterval = Double.parseDouble(args[++i]);
                        break;
                    case "--seed":
                        rngSeed = Long.parseLong(args[++i]);
                        break;
                    default:
                        System.err.println("Unknown argument: " + args[i]);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Returns a random double. Uses a seeded RNG if a seed was provided,
     * otherwise uses the thread-local random number generator.
     */
    public static double nextDouble() {
        if (rng != null) {
            return rng.nextDouble();
        }
        return ThreadLocalRandom.current().nextDouble();
    }

    public static void main(String[] args) throws InterruptedException {
        parseArgs(args);

        // Initialize RNG if a seed was provided
        if (rngSeed != null) {
            rng = new Random(rngSeed);
        }

        BusStop busStop = new BusStop();
        List<Thread> riderThreads = Collections.synchronizedList(new ArrayList<>());
        List<Thread> busThreads = Collections.synchronizedList(new ArrayList<>());

        // Thread to generate riders
        Thread riderGen = new Thread(() -> {
            for (int i = 1; i <= totalRiders; i++) {
                try {
                    double u = nextDouble();
                    long sleepTime = (long) (-meanRiderInterval * Math.log(1 - u));
                    Thread.sleep(sleepTime);
                    Thread riderThread = new Thread(new Rider(busStop));
                    riderThreads.add(riderThread);
                    riderThread.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Thread to generate buses
        Thread busGen = new Thread(() -> {
            for (int i = 1; i <= totalBuses; i++) {
                try {
                    double u = nextDouble();
                    long sleepTime = (long) (-meanBusInterval * Math.log(1 - u));
                    Thread.sleep(sleepTime);
                    Thread busThread = new Thread(new Bus(busStop));
                    busThreads.add(busThread);
                    busThread.start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Start generator threads and wait for them to finish
        riderGen.start();
        busGen.start();
        riderGen.join();
        busGen.join();

        // Wait for all spawned rider and bus threads to complete
        for (Thread t : riderThreads) {
            t.join();
        }
        for (Thread t : busThreads) {
            t.join();
        }

        // Print final summary
        Logger.log("Simulation complete.");
        System.out.println("\n--- Simulation Summary ---");
        System.out.println("Buses simulated: " + totalBuses);
        System.out.println("Riders spawned: " + totalRiders);
        System.out.println("Riders boarded: " + busStop.getTotalBoardedRiders());
        System.out.println("Max waiting riders: " + busStop.getMaxWaitingRiders());
        System.out.println("--------------------------");
    }
}