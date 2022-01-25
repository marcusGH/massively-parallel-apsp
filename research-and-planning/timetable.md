# Timetable

What TODO now:
* Go through all of code so far, execute refactor TODOs and refactor complete Worker -> Algorithm, and write some more **class documentation**
* Write new implementation progress list, and the next items to add are: Add timing wrapper for Algorithm and Manager, Add SSSP Dijkstra (that finds all paths) and can be used for testing

Documentation/refactoring cleanup:
- [ ] Do more clean-up on APSPSolver and graphReader and matrixMultiplication. Mostlyu done in memoryModel, util and work.

Implemention TODOs:
* Done: Write CountingMemoryController adapter, and new constructor taking a MemoryController
* Done: Fix up Worker adapter by creating new constructor instead of all the getters
* Done: Investigate if possible to nicely move all timing functionality to a different package
* Done?: Refactor memoryTopology passing out of memorycontroller base class
* Done: Write a _test for correctness_ on timing manager which works the same as the FoxOtto test (just copy the expected matrices and check if they're reproduced after running algo twice using the same manger).
* TODO next: Clean up driver code, refactor out topology in manager constructor (do this in different branch :))

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
- [ ] Correctness test
  - [ ] Written Dijkstra package that can be used to test for correctness on the driver, on many
        graphs, including really big ones
- [ ] **generalised algorithm!**
- [ ] graphCompression:
  - [ ] ...
