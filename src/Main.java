import java.util.concurrent.ThreadLocalRandom;

/**
 * Entry point for the bus and rider simulation.
 * This class starts generator threads that create rider and bus threads
 * according to exponential inter-arrival times and waits for them to finish.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        BusStop busStop = new BusStop();

        // Parameters: total riders and buses to simulate
        final int totalRiders = 100;
        final int totalBuses = 3;

        // Mean arrival times (in milliseconds)
        final double meanRiderInterval = 30000.0; // 30 seconds
        final double meanBusInterval = 1200000.0; // 20 minutes

        // Thread to generate riders
        Thread riderGen = new Thread(() -> {
            for (int i = 1; i <= totalRiders; i++) {
                try {
                    // Exponential sleep for rider inter-arrival
                    double u = ThreadLocalRandom.current().nextDouble();
                    long sleepTime = (long) (-meanRiderInterval * Math.log(1 - u));
                    Thread.sleep(sleepTime);
                    new Thread(new Rider(busStop)).start();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Thread to generate buses
        Thread busGen = new Thread(() -> {
            for (int i = 1; i <= totalBuses; i++) {
                try {
                    // Exponential sleep for bus inter-arrival
                    double u = ThreadLocalRandom.current().nextDouble();
                    long sleepTime = (long) (-meanBusInterval * Math.log(1 - u));
                    Thread.sleep(sleepTime);
                    new Thread(new Bus(busStop)).start();
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

        // Wait for a short period to let active threads finish
        Thread.sleep(5000);
        System.out.println("Simulation complete.");
    }
}