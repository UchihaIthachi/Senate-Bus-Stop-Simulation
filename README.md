# Bus and Riders Synchronization Problem

This project implements the classic **Senate Bus** synchronization problem using Java. The scenario models a bus stop where riders arrive and wait for buses. When a bus arrives it can carry up to 50 riders; any additional riders must wait for the next bus. Riders arriving during the boarding process must also wait for the next bus. If a bus arrives when there are no waiting riders it departs immediately.

## Problem Description

- **Rider Arrival:** Riders arrive randomly at a bus stop (with an exponential inter‑arrival time, mean 30 seconds) and wait for a bus. Each rider is represented by a thread that calls the shared `BusStop` to indicate its arrival and later board when permitted.
- **Bus Arrival:** Buses arrive with an exponential inter‑arrival time (mean 20 minutes). Each bus can board at most 50 waiting riders. Buses are represented by threads that interact with the `BusStop` to signal arrival, allow riders to board, and depart when ready.
- **Boarding Logic:** When a bus arrives it releases up to 50 permits for waiting riders. Riders arriving while boarding is in progress do **not** board that bus and instead wait for the next one. After boarding, the bus departs. If no riders are waiting the bus departs immediately.

This concurrency problem requires carefully coordinating multiple threads to avoid race conditions and ensure that riders board the bus correctly.

## Synchronization Approach

The solution uses a combination of a mutex and semaphores to coordinate access to shared state:

- A binary **mutex** protects the `waitingRiderCount` so that riders and buses can safely update it.
- A **waitingRiders** semaphore grants boarding permits to riders. When a bus arrives it releases up to 50 permits so that exactly that many riders can call `boardBus()`.
- An **allAboard** semaphore acts as a barrier. Each rider signals this semaphore when they finish boarding, and the bus waits on it as many times as riders boarded. This ensures the bus does not depart until all boarded riders have taken their seats.

**Rider Thread Logic:**

1. **Arrive at Stop:** Acquire `mutex`, increment the count of waiting riders, and print a log message.
2. **Wait to Board:** Call `waitingRiders.acquire()`. This blocks until a bus has arrived and released a permit.
3. **Board Bus:** Once a permit is acquired, print a boarding message and signal `allAboard` to indicate boarding completion.

**Bus Thread Logic:**

1. **Arrive at Stop:** Acquire `mutex` and compute `count = min(waitingRiderCount, BUS_CAPACITY)`. Log the arrival status.
2. **Allow Boarding:** Release `waitingRiders` permits `count` times so that exactly that many riders can board. Release `mutex` so that riders arriving while boarding cannot board the current bus.
3. **Wait for Riders:** Wait on `allAboard` `count` times to ensure that all boarding riders have signaled their presence.
4. **Depart:** Acquire `mutex`, decrement `waitingRiderCount` by `count`, log departure, and release `mutex`.

This design ensures that no more than 50 riders board a bus, that late riders wait for the next bus, and that a bus does not leave early.

## Project Structure

```
CS4532Lab2/
├── src/
│   ├── Main.java     – Simulation entry point
│   ├── BusStop.java – Shared bus stop synchronization logic
│   ├── Rider.java    – Rider thread implementation
│   └── Bus.java      – Bus thread implementation
├── README.md        – Problem description and solution overview
├── README.txt       – Compile and run instructions
└── Makefile         – Convenient commands to build and run the simulation
```

## Usage

You can either use the provided **Makefile** to compile and run the simulation or run the Java compiler and runtime directly. For details, see `README.txt`.