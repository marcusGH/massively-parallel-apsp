# Timetable

_Meta: move whole timetable 1-2 weeks back?_
* For 2 weeks ahead, the dissertation is still submitted 3 weeks before
  deadline, and the progress report is sent in just in time
* So, maybe slack a bit (1 week) at starting research+planning, then get up to speed (1 week) in vacation?

What TODO now:
* Go through all of code so far, execute refactor TODOs and refactor complete Worker -> Algorithm, and write some more **class documentation**
* Write new implementation progress list, and the next items to add are: Add timing wrapper for Algorithm and Manager, Add SSSP Dijkstra (that finds all paths) and can be used for testing

Documentation/refactoring cleanup:
- [ ] Do more clean-up on APSPSolver and graphReader and matrixMultiplication. Mostlyu done in memoryModel, util and work.

Implemention TODOs:
* Write CountingMemoryController adapter, and new constructor taking a MemoryController
* Fix up Worker adapter by creating new constructor instead of all the getters
* Investigate if possible to nicely move all timing functionality to a different package
* Refactor memoryTopology passing out of memorycontroller base class
* Write a _test for correctness_ on timing manager which works the same as the FoxOtto test (just copy the expected matrices and check if they're reproduced after running algo twice using the same manger).

Implementation progress:
- [o] memoryModel
  - [X] Written all methods
  - [X] Written some documentation for them
  - [o] Written unit tests for them
- [ ] graphReader:
  - [ ] Written all methods
  - [ ] Written some docs
  - [ ] Tested the code
- [ ] driver:
  - [ ] Planned methods
  - [ ] Written methods
  - [ ] Written docs
  - [ ] Tested code
- [ ] parallelMatrixMultiplication:
  - [ ] Written Fox-Otto
  - [ ] Docs
  - [ ] Tested code
- [ ] evaluation (rename):
  - [ ] Planned and written evaluation package
  - [ ] ...
- [ ] generalised algorithm
- [ ] graphCompression:
  - [ ] ...

Research progress:
- [X] Worked through formalism on choosing predecessor to use:
  - [X] Read online resources found on it
  - [X] Sketch a proof?


TODO list:
* Create Factories
* Write tests in conjunction with writing docs for all
* Refactor memoryModel on a package level
-> After all this, start working through formalism of APSP,
   then start implementing the APSP stuff
