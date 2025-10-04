Bus and Riders Synchronization Simulation – Compile and Run Instructions

This project contains Java source files implementing the Senate Bus synchronization problem. You can build and run the simulation using either the provided **Makefile** or manual Java commands.

## Using the Makefile (recommended)

The Makefile offers simple targets to compile, run, and clean the project. Ensure you have `make` and a JDK installed.

1. **Compile the project**

   ```
   make
   ```

   or explicitly:

   ```
   make compile
   ```

   This command creates the `bin/` directory (if it does not exist) and compiles all source files from `src/` into it.

2. **Run the simulation**

   ```
   make run
   ```

   This command first compiles the project (if necessary) and then runs the `Main` class from the `bin` directory.

3. **Clean compiled classes**

   ```
   make clean
   ```

   This removes the `bin/` directory and all compiled `.class` files.

## Using inline Java commands

If you prefer not to use the Makefile, you can compile and run the program directly with the Java compiler and runtime. Navigate to the project root (where `src/` resides) and run:

1. **Compile**

   ```
   javac -d bin src/*.java
   ```

   This command compiles all `.java` files in the `src/` directory and places the resulting `.class` files into a `bin/` directory. Create the `bin` directory first if it does not exist.

2. **Run**

   ```
   java -cp bin Main
   ```

   This runs the `Main` class, which starts the simulation.

## Command-Line Arguments

You can override the default simulation parameters by passing command-line arguments when running `Main.java`.

- `--riders <N>`: Sets the total number of riders to generate (default: 100).
- `--buses <N>`: Sets the total number of buses to generate (default: 3).
- `--meanRiderMs <ms>`: Sets the mean rider inter-arrival time in milliseconds (default: 30000).
- `--meanBusMs <ms>`: Sets the mean bus inter-arrival time in milliseconds (default: 1200000).
- `--seed <S>`: Sets the seed for the random number generator for a deterministic run.

Example of a quick test run:
```
java -cp bin Main --riders 20 --buses 5 --meanRiderMs 100 --meanBusMs 500 --seed 42
```

Make sure you have a Java Development Kit (JDK) installed and available on your system path.

## Windows Note

- **Makefile:** The `make` command is not available on Windows by default. To use the Makefile, run the commands from a terminal that provides Unix-like tools, such as **Git Bash** or the **Windows Subsystem for Linux (WSL)**.
- **Manual Compilation:** If using the Windows Command Prompt (`cmd.exe`), the `mkdir -p` command will not work. Create the `bin` directory with `mkdir bin` before compiling. The `javac` and `java` commands will work as described.