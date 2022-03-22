# Plan of dissertation structure and content

## Preparation

Subsections (~2500 words):
* Parallel computing (distributed memory model, communication, SIMD, MIMD, evaluating a parallel algorithm, ratio)
* APSP algorithm:
  * Dijkstra not well suited for parallelism, distribute work reference above, matrix multiplication highly parallel,
  * FoxOtto and Canon's algorithm are examples
* Requirements analysis (referencing theory above when writing this)
* Choice of tools
  * Professional approach, version control, correctness testing, CI etc.
* Starting point
* Implementation approach (??)
* Software engineering
* Conclusion

Items to write about....
* Requirements for the multiprocessor simulation:
  * Referencing goal of measuring parallel efficiency
* Choice of programming language:
  * Benefits of modularity etc.
* More items? .....

###    Requirements analysis

TODO: ...

## Implementation

Subsections (~4500 words):
* Graph datasets:
  * Real-world datasets
  * The input with `graphReader` package
  * Random graphs, parameter choice ref. to California
* Multiprocessor simulation:
  * The inter-core communication (`memoryModel` package)
  * The manager (`work` package)
  * The timing analysis (`timingAnalysis` package)
* Main algorithm:
  * Fox-Otto (`matrixMultiplication` package)
  * Repeated matrix squaring (`APSPSolver` package)
* Graph compression
  *  `graphReader` package additional class

_TODO: clean up below subsubsections, to be updated with new ToC_

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

  Want to demonstrate the following are met:
  • Implemented an algorithm based on matrix multiplication that can find the length of the
    shortest path between all pairs of nodes in a graph, and it is able to give the list of nodes
    that make up such paths.
  • Parallelised the matrix multiplication routine of the algorithm to run on a simulated mas-
    sively parallel processor, where each processing element can send data to each other through
    simulated interconnects.
  • The parallel matrix multiplication routine is optimised to minimise the amount of data move-
    ment between processing elements, which is done by using techniques such as Fox-Otto’s
    algorithm.
  • The evaluation of the algorithm demonstrates that parallel computation gives a high parallel
    efficiency for solving APSP

Subsections (~2000 words):
* Overall results (reference extensions done and not done?)
* Testing (unit tests. etc.)
  * Covers the correctness and criteria (1) above
  * Unit tests also cover point (2)
* Timing analysis (rename)?:
  * Various plots
  * This should show point (3) above
  * Also shows (4), by refering to how efficiency and ratio relate (or look at timing of serial algorithm ... and see multiplicative increase in performance as increase p)

The components (~2000 words):
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

