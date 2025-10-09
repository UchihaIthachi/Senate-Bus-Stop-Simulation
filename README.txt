# Bus and Riders Synchronization Simulation – How to Compile and Run

This program implements the Senate Bus synchronization problem in Java.
By default it runs in dynamic mode: buses keep arriving until all riders
are served (matches the lab note “buses and riders will continue to arrive
throughout the day”).

## Prerequisites

* Java JDK 11 or newer on your PATH (java, javac).
* Optional: `make` (for the Makefile targets).
* For Windows users: It is recommended to use a shell like Git Bash or WSL
  to ensure commands like `make` and `mkdir -p` work correctly.

## Quick Start (recommended)

1. Compile:
   $ make

2. Run (dynamic buses; finishes when all riders have boarded):
   $ make run

3. Take a screenshot of the terminal output showing arrivals, boarding,
   and departures (for submission).

## Fixed-Bus Demo (optional)

To demonstrate starvation with a fixed number of buses:
$ make run ARGS="--buses 5"

## Custom Runs (override defaults)

You can pass arguments through the Makefile via ARGS. Examples:

* Change rider count and seed:
  $ make run ARGS="--riders 200 --seed 42"

* Faster demo (short inter-arrivals):
  $ make run ARGS="--meanRiderMs 100 --meanBusMs 500 --seed 42"

## Arguments

--riders N           Total number of riders to generate (default: 100)
--buses N            Fixed-bus mode if N > 0. If 0 or omitted, dynamic mode.
--meanRiderMs ms     Mean rider inter-arrival in milliseconds (default: 30000)
--meanBusMs ms       Mean bus inter-arrival in milliseconds (default: 1200000)
--seed S             Random seed for reproducible runs (optional)

## Manual Compilation (no Makefile)

1. Create bin directory if needed:
   $ mkdir -p bin
   (On Windows CMD, you may need to use `mkdir bin` instead)

2. Compile all sources:
   $ javac -d bin src/*.java

3. Run (dynamic by default):
   $ java -cp bin Main

4. Run with custom arguments:
   $ java -cp bin Main --riders 150 --meanRiderMs 200 --seed 123

## What you should see

* Rider arrivals with the current waiting count, e.g.:
  Rider 12 arrives, waiting=13
* Bus arrival with snapshot and batch size (capacity 50):
  Bus 3 arrives: waiting 79, boarding 50
* Exactly “boarding” riders print “Rider X boarding”
* Bus departure after all permitted riders signal:
  Bus 3 departs: boarded 50, waiting=29

## Notes

* Dynamic mode prevents work starvation: buses keep arriving until all riders have boarded.
* Fixed-bus mode can leave riders waiting if all buses arrive too early (expected in that mode).
* Default means match the lab: rider mean 30 s, bus mean 20 min (use smaller values for demos).

## Files included

* src/BusStop.java, src/Bus.java, src/Rider.java, src/Main.java, src/Logger.java
* Makefile
* README.txt (this file)