# Plan of dissertation structure and content

## Preparation

_Clear motivation, justifying potential benefits of success._
_Good or excellent requirements analysis; justified and documented selection of_
  _suitable tools; good engineering approach._
_Clear presentation of challenging background material covering a range of_
  _computer science topics beyond Part IB._

The idea with the preparation is honing in **what** is it you finally set out on doing,
and why you ended up doing that (refining proposal). This also includes
**requirements analysis**: What should the multiprocessor simulation be able to do etc.

The reader will not like pulling things out of thin air, so in this chapter, I should
explain the _whys_, such as: I used the following model for communication and this
is suitable because X.

Items to write about:
* The real-world datasets
* Random graph generation, and parameter choice
* Requirements for the multiprocessor simulation:
  * Referencing goal of measuring parallel efficiency
* Choice of programming language:
  * Benefits of modularity etc.
* More items? .....

## Implementation

Do top-down description of the components:
* The main packages of program, and how interact
* Description of how they each work, and why and how

The components:
* Professional approach, version control, correctness testing, CI etc.
* The inter-core communication (`memoryModel` package)
* The manager (`work` package)
* The matrix multiplication (`matrixMultiplication` package)
* The APSP driver (`APSPSolver` package)
* The input (`graphReader` package)
* The timing analysis (`timingAnalysis` package)
* The graph compression algorithm (also `graphReader` package)

### Prof. Approach
### Inter-core communication
### Work management
Using executor service and why, diagram of how threads are used

### Matrix multiplication

All the details on FoxOtto here, with diagrams for the memory movement, and also
pseudo code on basic and(?) generalized version. Also predecessor matrix and edge case.

### APSP driver

Idea behind repeated matrix squaring, why $O(n^3 \log n)$

### Input
### Timing analysis

Explain wrapper, how done timing, repetition of computation, possible because of work
management, which gives good error bars

### Graph compression

The algorithm for this, and explain all the edge cases.

Also a section for the expected asymptotic speed-up referencing random graph generation


## Evaluations

The components:
* Plots:
  * Parallel efficiency
  * Sensitivity analysis on communication time
  * Speedup on california road network
  * Bin plot
  * _Also include discussion on significance of all the plots_
* Concrete evidence of all the success criteria:
  * Tested all pairs of nodes in large graph and paths reconstructed are correct
  * This also works when using parallel FoxOtto, so second criteria good
  * Argue for FoxOtto minimise memory cost
  * Reference plots on parallel efficinecy ratio, adn derivation from communication ratio

