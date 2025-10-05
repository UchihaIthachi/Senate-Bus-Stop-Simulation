public class Bus implements Runnable {
    private static int nextId = 1;
    private final int id;
    private final BusStop busStop;

    public Bus(BusStop busStop) {
        this.busStop = busStop;
        synchronized (Bus.class) { this.id = nextId++; }
    }

    @Override
    public void run() {
        try {
            busStop.depart(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}