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

# Run the simulation in dynamic mode by default, as per the lab specification.
# To run in fixed mode, provide a bus count e.g., make run ARGS="--buses 5"
run: compile
	java -cp $(BIN_DIR) Main $(ARGS)

# Runs a quick, deterministic simulation in dynamic mode for CI/testing.
test: compile
	java -cp $(BIN_DIR) Main --riders 100 --meanRiderMs 5 --meanBusMs 20 --seed 42

# Runs a minimal "smoke test" to ensure the program runs
smoke-test: compile
	java -cp $(BIN_DIR) Main --riders 2 --buses 1 --meanRiderMs 10 --meanBusMs 50 --seed 4

# Remove compiled classes and other generated files
clean:
	rm -rf $(BIN_DIR) output