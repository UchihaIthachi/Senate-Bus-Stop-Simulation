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

# Run the simulation
run: compile
	java -cp $(BIN_DIR) Main

# Runs a quick simulation for testing purposes
test: compile
	java -cp $(BIN_DIR) Main --riders 20 --buses 5 --meanRiderMs 100 --meanBusMs 500 --seed 42

# Remove compiled classes
clean:
	rm -rf $(BIN_DIR)