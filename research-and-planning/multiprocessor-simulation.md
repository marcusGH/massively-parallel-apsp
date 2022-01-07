# Code structure plan

## Components and their requirements

### Data preparation

* **Java InputGraphReader**:
  * Takes text file of list of edges and their weights
  * Return `Matrix` that is the adjacency matrix
  * Optional methods for removing edges and nodes not in the largest connected
    component
  * Statistics methods for finding highest degree and some measure of sparsity
  * Method to return adjacency `List`
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
    `memoryController`, `cycBarrier`
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
  * Implements `Runnable` and has a `run` method with the main loop:

        for (int l = 0 to p) do
          algo.communicationBefore(l);
          cycBarrier.await();
          algo.computation(l);
          algo.communicationAfter(l);
          cycBarrier.await();
        end

  * Maintains a timer that is started before `computation` and ended after
  * Implements a method `getComputationTimes` which gives list of all computation time phases

* The **MemoryController**:
  * Constructor needs `p` and `Matrix<PrivateMemory>` of the worker's private memory
  * Edit: We also need to pass a `Topology` object to the controller, so that it can calculate number of hops
    done whenever we send something and store this in its counter. We also need methods to extract statistics
    for memory usage, and would be sensible to include these here.
  * _We only have one object of this class, and copy is attached to each worker, is a sort of_ **MONITOR**
    * Doesn't need to be monitor, but can do finer-grained control by `syncrhonize`ing on the Queues and row/cols
  * _Introduces new exception type of e.g. congested broadcast channel_
  * Maintains $p$ by $p$ array of `privateMemory`s belonging to each PE(i,j), `currentPrivateMemory`
  * Maintains a $p$ by $p$ array of FIFO queues for shifted data
  * Maintains a $p$ by $p$ array of FIFO queues with `receive` arguments
  * Maintains length $p$ array of rows, initialised to `None`s    \_ used for broadcasting
  * Maintains length $p$ array of columns, initialised to `None`s /
  * Implements the following methods (which are to be used by the `Worker` interface's default implementation):
    * `broadcastRow(int, double)` - puts the provided value at appropriate index in `rows` and raises exception if congested
    * `broadcastCol(int, double)` - puts the provided value at appropriate index in `cols` and raises exception if congested
    * `send(int, int,, int, int, double)` - puts the provided data in the queue at (i, j), receiving from (i', j')
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

* **The Manager<Worker>**:
  * Constructor needs `n` and `p`, and map of `label`s to `Matrix`es.
  * _There is only one object of this class, and it creates one_ `CyclicBarrier`
    * This `CyclicBarrier` is given a `Runnable` object that does `memController.flush()`
  * Creates initial `PrivateMemory`s with content in map of labels, and distributes them
    to the workers (easy to generalise)
  * Creates a `memoryController`, passing it this `Matrix` of `PrivateMemory`s, which
    it can use to modify them
  * Creates all the `Worker`s, passing them these memory objects
  * Has the following methods:
    * `runAlgorithm` - In first iteration, does `run` on all workers. It then has the main loop:

          // implicit loop through cyclic barrier
          for (int l = 0 to p) do
            // wait for all workers to finish, then this happens
            memController.flush();
            // then all threads started again
            syncBarr.notifyAll();
            // same wait here
            memController.flush();
            // then they start up again
            synBarr2.notifyAll();
          end
          // Do .join() on all threads

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
* We recreate all the threads each time we do matrix multiplication. We need to distribute new
  private memory anyways, so easier to just start from scratch

### Driver

* **APSP solver**:
  * Constructor needs `n`, `p` and adjacency `Matrix`
  * Constructs `TimingAnalsysi` object
  * Has method `solve`? which does:
    * Create a manager with the `FoxOtto` class implementation
    * Does squaring $\log n$ times
    * Extracts computation and communication times and passes them to
      `TimingAnalysis` object
    * Returns result

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

## Some design decisions explanations


Design decisions:
* Explanation of approach: Start by designing programming interface. In the
  simulated systems we have a `n x n` grid and the code executed at each PE
  should be symmetrical except might reference different variables depending on
  its `i` and `j` coordinate. Therefore, when programming such grid algorithms,
  it would be nice to just specify what _computation_ and what _communication_
  all the PEs should in general perform, depending on their coordinates.
  Therefore, methods specifying this is the only thing that varies between code
  executed on this kind of system, so the other functionality (how this code runs,
  how the communications are implemented, etc.) should be implemented with default
  methods that don't need to be specified when implementing an algorithm.
* **Memory phase between global synchronisation**: We have a single memory controller
  monitor that all memory change request go to, and the memory changes happen when the
  PEs sleep. In a real system, would have many communication channels and would
  have fine-grained interactions on locking, listening and sending (?? read-up on
  this??) at each channel. This is complicated to simulate, and semantics the same with
  just one controller. Additionally, this approach makes it easier to keep statistics on
  the communication part since it's seperate. We also plan to do quasi-estimate
  of communicaiton time, so don't want to have it interefere with timed
  computation cost.
  The seperation also makes it easier to add timers around the computation phase, which
  is useful for our evaluation statistics gathering.
* The `awaitRow` and `receive` takes arguments to `Matrix::set`: Not very nice interface,
  but all the memory manipulations happens in a seperate place, so only option? Another
  option would be to infer this based on the order of sends and receive, but more safe
  for programmer to explicitly state it. Could also override with inferred version later
  down the line?  These functions also return nothing. This is because all
  communication happens in seperate phases than when computation is done. So in
  computation phase, the programmer only needs to think about what is in the
  _private memory_ and this memory manipulated while in communication phase.
* PrivateMemory holds a matrix object. This is looking ahead to generalising the algorithm
  to where each PE handles a submatrix. It will thus hold a submatrix of each input `A`
  and `B` so natural for this memory to be laid out in a matrix, for the programmer to
  interact with. In the general case, we also specify what each PE should do, so the
  whole submatrix must be taken into acount. An alternative approahc would be to default
  execution to doing each element in a loop, but may implement optimisations to algorithms
  where something clever done in each PE, so need flexibility to do other things as well
  (e.g. take into account sparsity, so model memory with something else).
* The communication is handled in separate object where coordinates of both
  input and output PE is passed to it. This allows doing computation on
  topology to determine shortest number of jumps required for each message
  pass. 
* Another reason for sperating the communication simulation is our evaluation, communication
  costs are estimated while only computation costs are actually timed.

Alternative approaches:
* Could `monitor`ed each PE and when sending something, manipulate it's private memory.
  This would complicate synchronisations?
* Another approach would be to have an object for each communication channel and lock it
  whenever want to send something. This would be more realistic and closer to actual HW,
  but makes planned evaluation difficult as would not happen simultaneously and would need
  to have trackers at each communication channel. It will also be more difficult to
  reason about when programming because  (not really). This just feels more difficult and
  more cumbersome than single memory controller approach, but I can't explain _why_.
  Anyways, benefit is less overhead in `Queue`s as it would be more fine-grained

Some more design decisions:
* Arguments to PrivateMemory::set is hard-coded into MemoryController::receive methods:
  * a Triple object is passed around and used in MemoryController. This makes implementation
    of `MemoryController` dependent on `PrivateMemory`, which is not that good. An alternative
    would be to create new classes related to `PrivateMemory` such as `PrivateMemorySetArgument`
    and `PrivateMemoryGetArgument` to abstract away the triples passed around, and make the
    getter and setter methods take one such object. However, this would be very unpractical
    to code with and becomes unwieldy. It's a better option to pass data around in `Triple`s
    and suffer the consequences of a possible refactor of both `MemoryController` and `PrivateMemory`
    in case we change the internal memory representation. However, the current design is quite flexible:
    We already generalised it to a matrix of any type, which should work for generalised case. We can
    also create `1 x n` matrices (lists) in case we want to use compact row/col representations when
    doing sparse matrix multiplications. So a refactor is unlikely to happen.
* When `receive`ing some data, the sending node is inferred instead of specified:
  * If we have two separate PEs sending data to the same PE, the order in which they are
    received will be non-deterministic since we don't know in which order the values are
    added to the queue, so we have "undefined behaviour". As a result, we can't have such
    memory transfers (TODO: add check for this?) However, if we want to explicitly state this,
    we need to pass around more data in cumbersome "quintuples". Additionally, we aim to design
    algorithms that minimise data movements, and if we have multiple different nodes sending data to
    the same nodes across the same channels, we have would in real situation have queueing and dependencies
    and ordering-constaints between the individual memory transfers, which would slow down and complicate things.
    In ideal algorithms, this is avoided, so by removing functionality, we shouldn't need it anyways.
* Worker and MemoryController in the same package because don't want to expose receiveBroadcasts methods etc. because
  only used by the worker
* **PrivateMemory is not generic**: Because of our class hierarchy, the generic
  type T that would be associated with PrivateMemory would need to ripple
  upwards to the MemoryController, Manager and Worker because they hold
  instances of it. This would in turn mean that when subclassing Worker to
  implement e.g. FoxOtto, we would still be constrained to implementing an
  algorithm for a generic type T. An appropriate solution is to use `T extends
  Number` such that our FoxOtto implementation can use numeric operations and
  we can later decide whether to use Integer or Double precision. I will now
  compare this solution with hard-coding integers and doubles (for example)
  into private memory:
  - The generic typing `T` ripples up in our class hierarchy, causing us to
    need to specify it for other classes like `Worker` and `MemoryController`
    as well
  - When implementing the `FoxOtto` class or similar worker classes, we must
    use boxed number variables, which is both cumbersome and inefficient. Stack
    variables would be more efficient and easier to work with.
  + We are more flexible in the type of variables we use, and don't need to change
    much code to use different  private memory types. However, to start with the only
    realistic types to use are `int`s and `double`s, and this is just two different types
    which is easy to hard-code. An extension may involve doing sparse matrix multiplication
    later, in which case we would store a list instead of a matrix with each processing
    element. It would be nice to just do `PrivateMemory<List>` in that case, but we can still
    remove generic and just subclass `PrivateMemory` to create a version that uses `List`s,
    we would then refactor `Manager` slightly to use a `Supplier<PrivateMemory>` instead
    of creating it itself, allowing use the new version easily and still get the benefits
    above.
* Factory interface for Worker and need to implement this for e.g. `FoxOtto` and other
  implementations. Alternative is using reflection to get the super constructor, which
  might just be possible with the non-generic version because we can't get the
  `Class` objects for generic classes like `PrivateMemory<T>`...
* Adapter pattern for timing analysis. Make a class TimingWorker where constructor takes a Worker object :))


TODO next: Done with refactoring generic-removal, so now get on with making the WorkerFactory and getting the WorkerCommunicationTest done
