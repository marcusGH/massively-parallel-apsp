# Timetable

Current TODOs:
- [ ] Clean up datasets and make the part of test-datasets in the git so anyone
      can reproduce the tests
- [X] Make performance plots more tidy
- [X] Do the bin plot
- [ ] Evaluation: Make branch to speed up serial simulation, then do more measurements
- [ ] Evaluation: Finish `main::Evaluation` for California road network, and run for different `p`
- [ ] Dissertation: Setup latex file structure, prepare relevant packages (see imperial site)

Past TODOs:
* **Allow MIMD execution, communication stalls**:
  * All this can be done by modifying `CountingMemoryController`
  * We keep a matrix for each PE that counts the time in ns they are _currently at_
  * We also keep a reference to every single Worker in a Matrix
  * Whenever methods like `sendData` are called, we know that each worker must have finished
    their computation phase bc. of internals in Manager, so it's safe to look into the reference
    for that worker and call `getCurrentComputationTime`. We do that on the **recipient** of the
    data and potentially add a stalling time to the _currently at_ time. Additionally, we now
    compute the latency and bandwidth time requires by the send. After this, we need to somehow
    reset the computation time of the worker because we might have "communicationAfter" ->
    "communicationBefore" without compute between.
  * We can also pass some modification of `TimingAnalyser` which just contains hardware specs,
    is used when adding the times in the above methods
  * After this, there is not really any need for `TimingAnalyser` as the Counting mc can
    return three matrices, communication times for each PE, computation times for each PE etc.
  * To get error bars on this kind of scheme, we simply run the whole thing many times (e.g. 10)
    and compare the total times...
* **Generalize the implementation**:
  * This is what it says, and I think it's more important than the evaluation fix bit, so do this
    first!
* **Random graphs with networkx in python**

Documentation/refactoring cleanup:
- [ ] Do more clean-up on APSPSolver and graphReader and matrixMultiplication. Mostlyu done in memoryModel, util and work.

Implementation progress:
- [X] memoryModel
  - [X] Written all methods
  - [X] Written some documentation for them
  - [X] Written unit tests for them
- [o] graphReader:
  - [X] Written all methods
  - [ ] Written some docs
  - [X] Tested the code
  - [ ] Clean up code, and execute TODOs
- [o] driver:
  - [X] Planned methods
  - [X] Written methods
  - [ ] Written docs
  - [X] Tested code
  - [ ] Clean up code
- [o] parallelMatrixMultiplication:
  - [X] Written Fox-Otto
  - [ ] Docs
  - [X] Tested code
- [.] evaluation (rename):
  - [X] Planned and written evaluation package
  - [ ] Write test for timingAnalysis package
- [X] Correctness test
  - [X] Written Dijkstra package that can be used to test for correctness on the driver, on many
        graphs, including really big ones
- [ ] **generalised algorithm!**
- [ ] graphCompression:
  - [ ] ...
