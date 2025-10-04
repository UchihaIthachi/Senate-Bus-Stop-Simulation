# Senate Bus Stop Simulation

This project implements the classic **Senate Bus** synchronization problem in Java. The system models a bus stop where riders arrive and wait. When a bus arrives, it may board up to 50 riders; any additional riders must wait for the next bus. Riders who arrive **during** an ongoing boarding phase also wait for the next bus. If a bus finds no riders, it departs immediately.

## Problem Description

* **Rider arrival:** Riders arrive randomly with an exponential inter-arrival time (mean **30 seconds**). Each rider is a thread that announces arrival to the shared `BusStop` and later boards when permitted.
* **Bus arrival:** Buses arrive with an exponential inter-arrival time (mean **20 minutes**). Each bus is a thread that signals arrival, allows boarding (up to capacity), and departs when finished.
* **Boarding rules:** On arrival, a bus releases at most **50** boarding permits for the riders that were already waiting. Riders that arrive after permits are released do **not** board this bus. After all permitted riders board, the bus departs. If no riders are waiting, it departs immediately.

This is a concurrency problem requiring careful coordination to avoid races and to enforce capacity and ordering constraints.

## Synchronization Strategy

We combine a mutex with semaphores to coordinate shared state and timing:

* **`mutex` (binary):** Protects `waitingRiderCount` so buses and riders update it safely.
* **`waitingRiders` (counting):** Grants boarding permits. Each arriving bus releases up to 50 permits so exactly that many riders can proceed to `boardBus()`.
* **`allAboard` (counting):** Barrier from riders to the bus. Each boarding rider signals once; the bus waits the same number of times to ensure everyone permitted has boarded.

### Why this works

* Capacity is enforced by the number of permits released.
* Late arrivals can update `waitingRiderCount` (after the bus releases `mutex`), but they receive **no permits** for the current bus.
* The bus waits on `allAboard` to prevent early departure.

## Thread Behavior

**Rider thread**

1. **Arrive:** Acquire `mutex`, increment `waitingRiderCount`, log, release `mutex`.
2. **Wait to board:** `waitingRiders.acquire()` blocks until a bus issues a permit.
3. **Board:** Log boarding and `allAboard.release()` to signal completion.

**Bus thread**

1. **Arrive:** Acquire `mutex`, compute `count = min(waitingRiderCount, BUS_CAPACITY)`, log status.
2. **Permit boarding:** Release `waitingRiders` exactly `count` times. Release `mutex` (new arrivals can wait for the next bus).
3. **Wait for completion:** Acquire `allAboard` `count` times to ensure all permitted riders have boarded.
4. **Depart:** Re-acquire `mutex`, decrement `waitingRiderCount` by `count`, log departure, release `mutex`.

**Guarantees**

* No more than 50 riders board a bus.
* Riders arriving during boarding wait for the next bus.
* The bus never departs before all permitted riders have boarded.
* Shared counters are updated without races.

## Project Structure

```
Senate-Bus-Stop-Simulation/
├── src/
│   ├── Main.java      – Simulation entry point
│   ├── BusStop.java   – Shared synchronization logic
│   ├── Rider.java     – Rider thread
│   └── Bus.java       – Bus thread
├── README.md          – Problem and solution overview
├── README.txt         – Build and run instructions
└── Makefile           – Compile/run/clean shortcuts
```

## Usage

You can use the **Makefile** or plain Java commands. See `README.txt` for both options (compile, run, and clean).
