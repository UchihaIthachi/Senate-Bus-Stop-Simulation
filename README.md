# 🚌 Senate Bus Stop Synchronization Simulation (Java)

This project implements the **Senate Bus Problem** using Java **semaphores**. It models a bus stop where riders arrive continuously and buses serve them under strict synchronization rules — a classic exercise from _The Little Book of Semaphores_ (p. 211).

---

## 🧩 Problem Description

- **Rider arrivals:** Exponential inter-arrival time (mean **30 seconds**).
  Each rider is a thread that announces arrival to the shared `BusStop`, then blocks until a bus allows boarding.
- **Bus arrivals:** Exponential inter-arrival time (mean **20 minutes**).
  Each bus is a thread that signals arrival, lets eligible riders board (up to capacity), then departs.
- **Boarding rules:**

  - Only riders who were **already waiting** at bus arrival can board.
  - Riders who arrive **during** boarding must wait for the **next** bus.
  - If **no riders** are waiting, the bus departs immediately.

- **Capacity:** A bus boards at most **50** riders.

The synchronization must prevent race conditions, deadlocks, and missed wake-ups, while enforcing capacity and “no late boarding.”

---

## ⚙️ Solution Overview

We use a **granular locking** approach with **two binary semaphores** and **two counting semaphores** to ensure correctness and liveness:

- `busMutex` — **serializes buses** so only **one** bus is processed at a time (prevents bus–bus races).
- `mutex` — protects shared counters (e.g., `waitingRiderCount`) in **short** critical sections only (so arrivals aren’t blocked during boarding).
- `waitingRiders` — **gate** for riders. The bus releases exactly `boarding` permits (where `boarding = min(waitingRiderCount, 50)`).
- `allAboard` — **barrier** from riders to bus. Each boarding rider signals once; the bus waits for exactly `boarding` signals before departing.

> Patterns used: mutual exclusion, counting semaphores, barrier/rendezvous, and the “I’ll-do-it-for-you” batching style (the bus controls the batch size).

---

## 🚦 Execution Flow

### Rider thread

1. `riderArrives()`: acquire `mutex`, increment `waitingRiderCount`, log; release `mutex`.
2. Wait on `waitingRiders.acquire()`.
3. On wake: log “boarding”, then `allAboard.release()`.

### Bus thread (`depart()` in `BusStop`)

1. **Serialize:** acquire `busMutex` (one bus at a time).
2. **Snapshot:** briefly acquire `mutex`, compute
   `boarding = min(waitingRiderCount, 50)`, log; release `mutex`.
3. **Permit:** release exactly `boarding` permits on `waitingRiders`.
   (Late arrivals won’t receive a permit for this bus.)
4. **Wait:** optionally sleep to simulate boarding (no locks held), then wait for `boarding` signals on `allAboard`.
5. **Update & depart:** briefly acquire `mutex`, decrement `waitingRiderCount` by `boarding`, update metrics, log departure; release `mutex`.
6. **Release:** release `busMutex`.

> **Crucial:** `mutex` is **never** held while sleeping or waiting on `allAboard`, so **new riders can still arrive** during boarding.

---

## ✅ Correctness Guarantees

- **Capacity:** ≤ 50 riders per bus (`boarding = min(waiting, 50)`).
- **No late boarding:** Riders arriving during boarding **do not** get a permit for the current bus.
- **No early departure:** Bus waits for **exactly** `boarding` `allAboard` signals.
- **Deadlock-free:** `busMutex` serializes buses; `mutex` is short-held and never around blocking waits.
- **Race-free counters:** All accesses to `waitingRiderCount` (and metrics) are under `mutex`.
- **Robustness:** Semaphore acquires/releases are paired in `try/finally`.

---

## 🧪 Validation & What to Look For

- During a bus’s boarding phase, **new “Rider N arrives…” logs still appear** → arrivals aren’t blocked.
- For each bus:

  - Log shows: `arrives: waiting X, boarding K` → **K ≤ 50**.
  - Exactly **K** “Rider … boarding” lines.
  - Then one `departs: boarded K, waiting=…`.

- No negative `waitingRiderCount`. No riders board after the bus departs.

_(Optionally print a final summary: total riders boarded, max waiting.)_

---

## 📂 Project Structure

```
Senate-Bus-Stop-Simulation/
├── src/
│   ├── Main.java       # Simulation driver (spawns riders/buses with exponential arrivals)
│   ├── BusStop.java    # Core synchronization logic (depart(), semaphores, metrics)
│   ├── Rider.java      # Rider thread
│   ├── Bus.java        # Bus thread
│   └── Logger.java     # Timestamped logging helper (e.g., [t=...s][Thread-...])
├── README.md           # Conceptual overview (this file)
├── README.txt          # Build & run instructions (Makefile + inline javac/java)
└── Makefile            # compile / run / clean targets
```

---

## 💻 Running the Simulation

**Option 1 — Makefile (recommended)**

```bash
make
make run
```

**Option 2 — Manual commands**

```bash
javac -d bin src/*.java
java -cp bin Main
```

**Clean**

```bash
make clean
```

> See **README.txt** for both Makefile and inline `javac/java` instructions.

---

## ⚙️ Configuration (optional, for demos/tests)

Keep lab defaults in code (mean rider = **30 s**, mean bus = **20 min**).
For quick demos/CI, you can expose a _fast mode_ via environment variables (if you added it in `Main`):

```bash
# Example (optional) fast demo: MUCH shorter means + deterministic seed
CI_FAST=1 SEED_ON=1 MEAN_RIDER_MS=50 MEAN_BUS_MS=200 make run
```

Defaults must remain the lab’s parameters when not in fast mode.

---

## 🧠 Learning Outcomes (ties to the lab brief)

- Practical use of **mutexes and semaphores** to coordinate multiple threads.
- Designing **deadlock-free** algorithms with **barriers** and **counting semaphores**.
- Implementing **batching** (“I’ll do it for you”) to meet capacity and ordering constraints.
- Modeling **exponential inter-arrivals** for realistic asynchronous systems.

---

## 🔎 Sample Output (abridged)

```
[t=297.345s][Bus-1] Bus 1 arrives: waiting 79, boarding 50
[t=297.352s][Rider-12] Rider 12 boarding
...
[t=299.346s][Bus-1] Bus 1 departs: boarded 50, waiting=29
[main] Simulation complete.
```

---

## 📝 Notes for Viva / Grading

- Why two locks? **`busMutex`** prevents bus–bus overlap; **`mutex`** protects counters but is short-held (arrivals continue during boarding).
- Why late riders wait? The bus releases **exactly** `boarding` permits; late arrivals get no permit for that bus.
- Why no early leave? The bus waits for **`boarding` `allAboard`** signals (barrier).
- Where could deadlock happen originally? Holding a global lock across waiting/sleep led to stalls; we fixed it by narrowing critical sections and serializing buses.

---

**Acknowledgment**: Problem statement adapted from _The Little Book of Semaphores_, §7.4 “The Senate Bus problem” (p. 211).
