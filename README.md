# 🚌 Senate Bus Stop Synchronization Simulation (Java)

This project implements the **Senate Bus Problem** with Java **semaphores**. It models a bus stop where riders and buses arrive continuously and are coordinated under strict synchronization rules — a classic exercise from _The Little Book of Semaphores_ (§7.4, p. 211).

By default, the simulation runs in **dynamic bus mode**, which prevents work starvation by continuing to generate buses until **all riders** are served.

---

## 🧩 Problem Description

- **Rider arrivals:** Poisson process (exponential inter-arrival), mean **30 seconds**. Each rider thread announces arrival and waits to board.
- **Bus arrivals:** Poisson process, mean **20 minutes**. Per the lab note “buses continue to arrive throughout the day,” the default driver keeps sending buses until everyone is served.
- **Boarding rules:**

  - Only riders **already waiting** at bus arrival may board.
  - Riders arriving **during** boarding wait for the **next** bus.
  - If **no riders** are waiting, the bus departs immediately.

- **Capacity:** At most **50** riders per bus.

The solution must avoid race conditions, deadlocks, and missed wake-ups, while guaranteeing liveness and enforcing capacity and no-late-boarding.

---

## ⚙️ Solution Overview

We use **granular locking** with two binary semaphores and two counting semaphores:

- `busMutex` — **serializes buses** (only one bus is processed at the stop at a time).
- `mutex` — protects shared counters (e.g., `waitingRiderCount`) in **very short** critical sections, so new arrivals are never blocked by a bus that’s sleeping or waiting.
- `waitingRiders` — **gate** for riders; the bus releases exactly `boarding = min(waitingRiderCount, 50)` permits.
- `allAboard` — **barrier** from riders to bus; each boarding rider signals once; the bus waits for exactly `boarding` signals before departure.

> Patterns: mutual exclusion, counting semaphores, rendezvous/barrier, and the “I’ll-do-it-for-you” batching style (the bus controls batch size).

**Dynamic vs Fixed mode**

- **Dynamic (default):** buses continue to arrive until `totalBoarded == totalRiders` → **no starvation** by construction.
- **Fixed:** a fixed number of buses are scheduled. This mode can **starve** (by design) if buses cluster early and later riders have no bus.

---

## 🚦 Execution Flow (high level)

1. **Rider**: `riderArrives()` increments `waitingRiderCount` under `mutex`, then waits on `waitingRiders`.
2. **Bus** (`depart()`):

   - Acquire `busMutex` (one bus at a time).
   - Take a **snapshot** of `waitingRiderCount` under `mutex`, compute `boarding = min(waiting, 50)`.
   - Release exactly `boarding` permits on `waitingRiders`.
   - Wait for exactly `boarding` `allAboard` signals (no `mutex` held here).
   - Under `mutex`, decrement `waitingRiderCount` by `boarding`, log and depart; release `busMutex`.

**Crucial:** `mutex` is **never** held while sleeping or waiting on other semaphores → prevents deadlock and lets arrivals proceed concurrently.

---

## ✅ Correctness Guarantees

- **Liveness (Dynamic Mode):** buses keep arriving until **all riders** are boarded → **no starvation**.
- **Capacity:** `boarding = min(waiting, 50)` ensures at most 50 board.
- **No late boarding:** permits are released **once** for the snapshot; late arrivals don’t receive a permit for the current bus.
- **No early departure:** bus waits for **exactly** `boarding` `allAboard` signals.
- **Deadlock-free:** buses are serialized (`busMutex`); `mutex` is short-held and never while blocking on other semaphores.
- **Race-free counters:** shared metrics are updated under `mutex` only.

---

## 💻 Running the Simulation

**Using Makefile (recommended)**

```bash
make            # compile
make run        # run with defaults (dynamic mode)
make test       # quick deterministic demo (short means, fixed seed, dynamic)
make clean      # remove bin/
```

**Manual commands**

```bash
javac -d bin src/*.java
java -cp bin Main                 # dynamic by default
java -cp bin Main --buses 5       # fixed mode (may starve by design)
```

**Custom configuration (via Makefile ARGS)**

```bash
make run ARGS="--riders 200 --meanRiderMs 100 --meanBusMs 500 --seed 42"
# Flags:
#   --riders <N>         total riders (default 100)
#   --buses <N>          fixed-bus mode if N>0; dynamic if 0 or omitted
#   --meanRiderMs <ms>   mean rider inter-arrival (default 30000)
#   --meanBusMs <ms>     mean bus inter-arrival (default 1200000)
#   --seed <S>           RNG seed for reproducibility
```

---

## 🧪 Sanity Checks (what graders should see)

- While a bus is boarding, **new “Rider … arrives”** logs still appear → arrivals not blocked.
- For each bus:

  - `arrives: waiting X, boarding K` with **K ≤ 50**.
  - Exactly **K** “Rider … boarding” lines.
  - One `departs: boarded K, waiting=R`.

- `waitingRiderCount` never negative.
- **Dynamic mode**: program finishes with `Riders boarded = Riders spawned`.

---

## 📂 Project Structure

```
.
├── src/
│   ├── Main.java       # Simulation driver (dynamic by default; parses args)
│   ├── BusStop.java    # Core synchronization (semaphores, depart(), metrics)
│   ├── Rider.java      # Rider thread
│   ├── Bus.java        # Bus thread
│   └── Logger.java     # Timestamped logs: [t=...s][Thread-...]
├── Makefile            # compile / run / test / clean
├── README.md           # overview + theory + usage
└── README.txt          # concise run instructions for submission
```

---

**Acknowledgment:** Problem adapted from _The Little Book of Semaphores_, §7.4 “The Senate Bus problem” (p. 211).

---
