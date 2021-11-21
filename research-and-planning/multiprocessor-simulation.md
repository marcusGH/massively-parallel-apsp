# Code structure plan

## Components and their requirements

### Data preparation

* **Java InputGraphReader**:
  * Takes text file of list of edges and their weights
  * Return `Matrix` that is the adjacency matrix
  * Optional methods for removing edges and nodes not in the largest connected
    component
  * Statistics methods for finding highest degree and some measure of sparsity
* **Python? graphPreperator**:
  * Takes text of file of edges and nodes' position in (long, lati)
  * Creates a circle in geographical coordinates, and extracts subgraph
  * Produces text file `InputGraphReader` can read

### Multiprocessor simulation

* _General requirements_:
  * When initialising, can configure:
    * Processor interconnect topology
    * Size $p^2$ grid of PEs
    * Problem size $n$
    * Implementation of `Algorithm(i, j, l)` interface
      specifying what to do at PE(i,j) at iteration `l`
    * Data to allocate to the PEs
  * After running, get resulting data
  * After running, get computation and communication time at each phase

* The **Matrix** class:
  * Used to store objects in square grids
  * _Does the unboxing and boxing for the user_
  * Basic methods...

* **The Worker interface**:
  * Constructor needs variables `i`, `j`, `p`, `n`, `privateMemory`,
    `memoryController`, `syncBarrier1`, `syncBarrier2`
  * _The privateMemory needs to be package so that MemoryController_
    has control over it
  * Methods to override `computation(l)`:
    * What does PE(i,j) do at iteration `l` having access to `this.privateMemory`?
  * Communication stuff to override `communicaitonBefore(l)` and `communicationAfter(l)`:
    * Uses calls to below methods
  * Pre-defined default methods not to be overridden:
    * `broadcastRow(double) : void`
    * `broadcastCol(int, double)` - puts the provided value at appropriate index in `rows` at memory controller
    * `awaitRow(i, j, label) : void` (wait for data from broadcast and put in priv. memory)
    * `send(int, int, double) : void` (send data to PE at (i', j'))
    * `receive(i, j, label) : void` (receive from `shift` and put in private memory)
  * Maintains two references `synchronisationBarrier1` and `synchronisationBarrier2`
  * Implements `Runnable` and has a `run` method with the main loop:

        for (int l = 0 to p) do
          algo.communicationBefore(l);
          synchronisationBarrier1.wait();
          algo.computation(l);
          algo.communicationAfter(l);
          synchronisationBarrier2.wait();
        end

  * Maintains a timer that is started before `computation` and ended after
  * Implements a method `getComputationTimes` which gives list of all computation time phases

* The **MemoryController**:
  * Constructor needs `p` and `Matrix<PrivateMemory>` of the worker's private memory
  * _We only have one object of this class, and copy is attached to algorithm interface, is a_ **MONITOR**
    * Doesn't need to be monitor, but can do finer-grained control by `syncrhonize`ing on the Queues and row/cols
  * _Introduces new exception type of e.g. congested broadcast channel_
  * Maintains $p$ by $p$ array of `privateMemory`s belonging to each PE(i,j), `currentPrivateMemory`
  * Maintains a $p$ by $p$ array of FIFO queues for shifted data
  * Maintains a $p$ by $p$ array of FIFO queues with `receive` arguments
  * Maintains length $p$ array of rows, initialised to `None`s    \_ used for broadcasting
  * Maintains length $p$ array of columns, initialised to `None`s /
  * Implements the following methods (which are to be used by the `Algorithm` interface's default implementation):
    * `broadcastRow(int, double)` - puts the provided value at appropriate index in `rows` and raises exception if congested
    * `broadcastCol(int, double)` - puts the provided value at appropriate index in `cols` and raises exception if congested
    * `send(int, int, double)` - puts the provided data in the queue at (i, j)
    * `receive(int, int, int, int, label)` - puts the arguments into the queue at (i, j) with args (k, l, label)
    * `awaitRow(int, int, int, int, label)` - puts argument into broadcast queue at (i, j) with args (k, l, label)
    * `flush()` - iterates the broadcast highways and mathces the values up with `await`s and runs `currentPrivateMemory.set`s
                and also does similar stuff with all the `send` and `receive` queues, raising exception if there are
                any length mismatches.

* **The PrivateMemory**:
  * Constructor needs `k` size of internal `Matrix` memory grid (basic/default one has `k=1`)
  * `get(label)` and `set(label)` (and which are shorthand for generalised versions)
  * _Has timers in its `get` and `set` methods, which can be subtracted when doing summary of times?_
  * TODO: consider if worth to specify MemoryController-controlled labels, and restrict access to `set`ing these?

* **The Manager**:
  * Constructor needs `n` and `p`, and map of `label`s to `Matrix`es.
  * _There is only one object of this class, and it creates two_ `synchronisationBarrier`s
  * Creates initial `PrivateMemory`s with content in map of labels, and distributes them
    to the workers (easy to generalise)
  * Creates a `memoryController`, passing it this `Matrix` of `PrivateMemory`s, which
    it can use to modify them
  * Creates all the `Worker`s, passing them these memory objects
  * Has the following methods:
    * `runAlgorithm` - In first iteration, does `run` on all workers. It then has the main loop:

          for (int l = 0 to p) do
            // Before the flushes, need to make sure all threads are finished.
            // Consider using [ReentrantLock](https://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/locks/ReentrantLock.html#getQueueLength()
            // and research if can still notifyAll() the way we do!
            memController.flush();
            syncBarr.notifyAll();
            // do same consideration here
            memController.flush();
            synBarr2.notifyAll();
          end

  * `getComputationTimes` - returns some big data structure, which then handed
                          over to Analyse class or something
  * `getResult(label) : Matrix` - combines the sub`Matrix`es of all the workers
                                to get one big matrix, extracting data stored with `label`

_Design decisions_:
* We pick up all sent data in queues through monitor, and we then dump it into memories on one sweep
  * This seems more efficient and simpler to reason/think about, idk?
* We have a `Worker` interface with many default methods, so to implement an algorithm, simply
  override `run` and use provided `broadcast`, `send`, `receive` methods!
* Package together Worker, PrivateMemorty and MemoryController to hide the `set` and `get`
  of privateMemory entirely in the package, leaving constructor open to the Manager

### Driver

* **APSP solver**:
  * TODO

## Comments on the evaluation framework

* If we are going with MIMD, there are important factors to the timing of
  computation time that affects accuracy, so certain questions need to be
  answered through testing Java code:
  * If a thread is paused while timer within it is running, does the timer stop?
  * How different are the time measured ($\sigma$)? Do batch testing!
  * Does actual cache effects play a role? Should we have interface to make sure
    stuff is in memory before starting the timer?
  * _If we count the number of flops, and use time based on that, we basically do SIMD_
  * Since my clock speed is higher, should the communication bandwidth/latency be adjusted?
    * Can just say: _my processor, but we have interconnect technology from supercomputer X_
    *
