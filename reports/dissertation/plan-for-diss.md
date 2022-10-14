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

###    Requirements analysis

Multiprocessor simulation:
* Can configure processing element grid size $p$, and the interconnect topology
* (Can pass `Matrix` of input data, and automatically distributed over PEs)
* Simple interface with sufficient expressive power; Can run arbitrary "work"
  description on the form $(i, j, l)$, express all required algorithms on this
  form
  * Prepeation, read on parallel algortihm like FW, cannon, foxOtto, on the special form, so suitable interface
  * Very neat interface for specifying work
  * Allows multiple different algorithms to easily be tested when framework finished
* Simulates key characteristics of multiprocessor
  * Distributed memory model, no shared memory
  * Message passing
  * MIMD, where each PE runs independently of others, so stalling may happen when sending messages (extension?)
* Can measure computation time and estimated communication time, based on above characteristics
  * Used to find communication-computation ratio which can be used for evaluation
* Efficient; Uses parallelism in simulation as well

Input data:
* Can obtain graphs with similar properties, but of different sizes in terms of nodes
  * For evaluating the performance scaling

APSP algorithm:
* Computes distance and predecessor matrices by describing "work" using above interface
* Can use these matrices to create a list of nodes for any shortest path
* Can compute the matrices when using fewer processing elements than problem size (extension)

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

