# Makefile for Bus and Riders Synchronization Simulation
#
# Targets:
#   make or make compile – compile all Java sources into the bin directory
#   make run            – compile (if needed) and run the Main class
#   make clean          – remove compiled class files

SRC_DIR = src
BIN_DIR = bin
SOURCES = $(wildcard $(SRC_DIR)/*.java)

.PHONY: all compile run clean

# Default target: compile the sources
all: compile

# Compile all Java sources
compile:
	mkdir -p $(BIN_DIR)
	javac -d $(BIN_DIR) $(SOURCES)

# Run the simulation with default lab parameters. Accepts command-line args via ARGS.
# The default mode is dynamic buses, as per the lab specification.
run: compile
	java -cp $(BIN_DIR) Main --dynamicBuses $(ARGS)

# Runs a quick, deterministic simulation for CI/testing with a high volume of riders
test: compile
	java -cp $(BIN_DIR) Main --riders 100 --dynamicBuses --meanRiderMs 5 --meanBusMs 20 --seed 42

# Runs a minimal "smoke test" to ensure the program runs
smoke-test: compile
	java -cp $(BIN_DIR) Main --riders 2 --buses 1 --meanRiderMs 10 --meanBusMs 50 --seed 4

# Remove compiled classes and other generated files
clean:
	rm -rf $(BIN_DIR) output