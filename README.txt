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

Make sure you have a Java Development Kit (JDK) installed and available on your system path.