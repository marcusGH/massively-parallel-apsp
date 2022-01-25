# Research and planning

[Dissertation writing tips](dissertation-tips.md)

[Progress Report](progress-report)

## Expanded timetable

[Timetable](timetable.md)

## Resources

**Past projects:**

[vk313, 2017](https://www.cl.cam.ac.uk/teaching/projects/archive/2017/vk313-dissertation.pdf)

Interesting points:
* Used a SIMD-based model for the computation, because inherent similar instructions
  when doing matrix multiplication.
  * Comment: Is about justifying a design decision. In my case I will use timers in the
    threads to estimate cost, so would be having many SISD's running together, so MIMD
* Not very interesting/thorough evaluation (lacking imho)
* Like yl431, said that simulation in Java did not prove to be very accurate
  * Comment: This is a big challenge, so do research/plan how this can be made as
    realistic as possible.
* Motivated parallelism through references to (stop? in) Dennard scaling and
  how reached limit on how much we can improve serial processors, so next
  natural direction to get a lot of benefit is employing many different
  processors working in parallel as -- if done appropriately with the right
  algorithms -- gives a lot of speedup compared to cost, comparing the system
  to a serial system.


[yl431, 2013](https://www.cl.cam.ac.uk/teaching/projects/archive/2013/yl431-dissertation.pdf)

Interesting points:
* Requirements analysis based on success criteria
* Flow chart of project components and how they relate
* Discusses surrounding topics to justify design decisions e.g. MIMD?, distributed memory model
* In addition to dynamic metrics like speedup, there are static ones analysable with flow graphs
* _Generally, preparation phase explained all the techniques/algorithms researched_
* To handle communication, each PE runs two threads and each sending
  (broadcasting or otherwise) is on hop-by-hop basis where the communicator
  thread in each PE handles it and delivers it to worker-thread or "puts it on
  next interface". The justification (I think) is that Java doesn't support
  modelling multicast, so a hop-by-hop simulation is used (con: doesn't capture
  benefit of hardware broadcasting). Could be achieved with `MPI_Bcast` and
  `MPI_Sendrev_replace`? Also, wouldn't this cause linear broadcasting costs `n`?
  That will not scale very well, so think about how this broadcasting should be
  simulated in the framework!

[hdc21, 2011](https://www.cl.cam.ac.uk/teaching/projects/archive/2011/hdc21-dissertation.pdf)

Not accessible yet

[nc344, 2011](https://www.cl.cam.ac.uk/teaching/projects/archive/2011/nc344-dissertation.pdf)

Not accessible yet


**Relevant papers:**

Cremonesia,P. and Rostib,E. and Serazzia, P. and Smirnid,E. Performance evaluation of parallel systems 1999.

Akpan, Okon H. Efficient Parallel Implementation of the Fox Algorithm 2003

(maybe?) Xiao, W. and Parhami, B. Structural Properties of Cayley Digraphs with Applications to Mesh and Pruned Torus Interconnection Networks 2007

Modi J. Parallel Algorithms and Matrix Computation. 1988.

% TODO: the text below might be better moved to separate page

* More rigorous classification of parallel systems (c.f. Flynn's taxonomy) is
  made by Gurd 1988
* Main difference SIMD-MIMD is the inclusion of separate ALU and memory unit within
  the processor, also allowing the processors to run asynchronously. Also common for
  SIMD systems to more easily have more processors, so MIMD MMP might be more unrealistic
* Regarding connectivity topology used in the parallel systems, usually the choice is
  determined by the nature of the algorithm (e.g. circular connectivity when sorting
  numbers), and lattice topology for matrix algorithms

[Minimizing Communication in APSP](https://www.researchgate.net/publication/235784373_Minimizing_Communication_in_All-Pairs_Shortest_Paths)
* The approach in my project looks like approach [24], but using
  Fox-Otto/Cannon's instead of the SUMMA-like algorithm [2].
* One original contribution might be the investigation of the advantage
  as the problem size increases while num. PEs is the same
* Also interesting to reference the proof [4] that matmul semiring
  multiplicaiton has same asymptotal complexity as other APSP solutions
  (and how that is what the work is based on).

[Survey on all APSP](http://www.acta.sapientia.ro/acta-info/C8-1/info81-02.pdf)

### Knowledge on matrix multiplication techniques

[Parallel methods for matrix multiplication](http://www.hpcc.unn.ru/mskurs/ENG/DOC/pp08.pdf)

I have read this (details in notebook)

[Overview, and then better approach](http://ilpubs.stanford.edu:8090/59/1/1994-25.pdf)

Read half, more details in notebook

[Berntsen's algorithm](https://reader.elsevier.com/reader/sd/pii/0167819189900914?token=7DEA6AD6448CA99933B09C9CE7E16BC284B2B325CCF1EF50536455BC35E7F123C179A35A7F02DA4270137F105B3C6177&originRegion=eu-west-1&originCreation=20211024104716)

Not worth reading now, unless want to make hypercube algorithm

[very detailed on communication](https://www.researchgate.net/publication/220327777_Optimum_Broadcasting_and_Personalized_Communication_in_Hypercubes)

Not read yet

Extra: [sparse parallel matMul](https://arxiv.org/pdf/1006.2183.pdf)
Or was it [this one](https://www.researchgate.net/publication/220486666_Highly_Parallel_Sparse_Matrix-Matrix_Multiplication)?

Cannon's and Fox-Otto are built for _dense_ matrices, so might be worth a look
at as a further optimisation of the algorithm.

* [Scalability of parallel algorithms for APSP, kumar](https://www.sciencedirect.com/science/article/pii/074373159190083L?via%3Dihub)
  * See notes
* [sunway supercomputer simple report](http://www.netlib.org/utk/people/JackDongarra/PAPERS/sunway-report-2016.pdf)
  * Good summary of constants associated with the Sunway 26010 chip
* [intel's 8*10 core chip](https://www.researchgate.net/publication/2983733_An_80-tile_sub-100-w_teraflops_processor_in_65-nm_CMOS)
  * Latency is a lot lower, but doesn't use network since relatively small chip
* [NoC multicast support](https://arxiv.org/pdf/1610.00751.pdf)
  * TODO: read this!
* More reading material on APSP predeccessor matrix: [link](http://theory.stanford.edu/~virgi/cs367/oldlecs/lecture7.pdf)

* [Zwick's algorithm](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=743464)
  * This does approximation to calculate product of matrices, witness notation from here
* Planar graphs, allows other methods very efficient, and some in later paper are parallelizable :
  * https://arxiv.org/pdf/2111.07360.pdf
  * https://www.sciencedirect.com/science/article/pii/S0022000097914938
  * https://dl.acm.org/doi/10.1145/165231.165240

## Planning

[Multiprocessor simulation](multiprocessor-simulation.md)

...

## Research phase plan

- [x] Read _The Pink Book_, watch the "How to write a dissertation" lecture
      (and other material?) with the aim of answering the question: "What makes
      a good dissertation?"
- [o] Read all 4 of past projects, taking note of:
  * Their references/bibliography
  * What major items they put into the 5 chapters
  * Their method of evaluation
- [ ] Area of knowledge to further expand on/read about:
  - [x] General knowledge about _parallel_ computing concepts
    * Realised from reading Modi that these are fairly OK. List:
      * Distributed vs. shared memory
      * Flynn's taxonomy
      * Processor topology
      * Broadcasting vs. only nearest neighbour communication
  - [o] Evaluating parallel performance
    * The main metrics are $speedup$ and $efficiency$
    * Could also look at $isoefficinecy$
  - [x] General overview of the different parallel matrix multiplication techniques
    * Fox-Otto: row broadcasting (only one P_ij needs to broadcast at a time),
                each P_ij sends their data upwards along column
    * Cannon:   No broadcasting, but each P_ij sends data to "far-away" node in the
                row (which is sped up if embedded tree)
    * Sparsity: Techniques to speed up sub computation for sparse matrices, but mainly
                used when p << n^2, and not that effective as get close to goal because
                then is less sparse
                TODO: could read a bit more into exactly how that sparse method worked...
    * Warshall: Both column and row broadcasting used, but only one processor per column
                and one per row at a time -> Requirements: need to have option between both
                point to point communication, or broadcasting horizontally, vertically, or both
  - [ ] How does message passing work in these multiprocessors?:
    * Are there packets being sent around, and they queue up at manager thread?
    * Does it communicate with manager thread through shared memory, requiring locking?
  - [X] Arrange supervision to discuss the parallel matrix multiplication algorithm
        to find shortest paths

- [ ] Write summary of references read in research phase
  * This phase is described in the dissertation, so write some notes that will make this
    easier to write when you start your write-up. Also make it expandable as
    some research (on e.g. extensions) may be done at later point, but still
    put into this dissertation chapter

**Milestones:**
- [x] Got hold of Modi's book from library
- [x] Written some small fragments of Java code that uses concepts
  * _Remember: important part of preparation phase is getting practice in what you'll do in the main implementation phase_
- [ ] The _Work plan_ is fleshed out in a lot more detail

## Preparation and planning phase plan

- [ ] Create list of requirements for the three main components of the
      implementation:
  - [ ] The APSP driver
  - [ ] The multiprocessor simulation framework (think about **evaluation**
        and **Fox-Otto/FW** and **generalisation to n>p** while doing this)
  - [ ] The data preparation helper class
- [ ] Do data preparation (and possibly look into OSM data with python)
- [ ] Get some experience with the **tools** I'll be using:
  - [X] Try to parallelise matrix operation described on Modi book p.47
  - [X] Play around with multithread timing in Java
    * Figure out: _When simulating multiprocessor compute element, the timer is_
      _not paused because of JVM stuff??_
    * Findings: There is very little difference in `ThreadMxBean` API and
      `System.nanotime()` call, with the exception that the latter has
      some massive outliers (probably due to garbage collection).
      Doing about 1000 x 1000 processing elements caused each PE
      to take about 5-6 microseconds (Sunway memory latency was 1 microsecond,
      but execution time may be scaled up due to clock speed difference).
      TODO: implement a serial version with similar timing mechanisms, and check
      if the computed 5-6 microseconds time is accurate or not. If so, the way of
      placing timers in the worker threads should be good to go!

## The evaluation

    _The evaluation of the algorithm demonstrates that parallel_
    _computation gives a high parallel efficiency for solving APSP_

**Quasi-analytical evaluation:**
* Use realistic constants for memory latency and interconnect bandwidth:
  * Use the constants from the Sunway supercomputer, and can safely assume
    the constants are the same for row and column broadcasting
* Assume SIMD model, and do average of each computation phase
* Add computation + communication time to get total parallel running time
* Con: Need to implement serial algorithm
* Pro: Backed in research

**Quantitative complexity analysis:**
- [ ] Has this ever been done in the past literature?
* Get total time on many runs of the algorithm for different input sizes
* Fit the data to a model (e.g. log n * n^k) that is based on analytical analysis
* Compare this complexity function to theoretical serial complexities
* Pro: Don't need to implement a serial algorithm
* Con: No backed research on how viable this is. E.g. problematic because need
  to run analysis for many many pairs of (n, p) where we for each fixed p do
  many n runs and do statistical fitting

**Just look at Communication/Computation ratio**:
* If $b / (a+b) < k$, then $\epsilon > 1-k$

**Analytical complexity analysis:**
* Must of course be done to complement either of the above...

**Using the results:**
* _Both of the above give a function for efficiency:_
  * Former gives data items for each choice of (n,p)
  * Latter gives function of n for each choice of p?
- [ ] Think about this some more...
- [ ] Which of the above two options is most suitable?
- [ ] What specific **evidence** would I want?

## "Shower thoughts"
* To make private memory access consistent, add some interface for it, that
  loads the variable into cache and pauses timer. The variable should also be
  _alive_ so the compiler doesn't optimize it away.
* If making pipelined approach with that computation graph thing to evaluate,
  you would need to build a framework able to handle processor sending data
  before another processor requests that data.
* Planning phase should involve **requirements** of the like:
  * The multiprocessor simulation supports both point-to-point sending and
    simulated broadcasting

## Trying to determining latency constants

Resources:
* Intel's [skylake](https://en.wikichip.org/wiki/intel/microarchitectures/skylake_(server)#Memory_Hierarchy) processors have mest interconnet microarhictecture
* Some analysis on mesh interconnect, with cycle counts in [paper](https://dl.acm.org/doi/pdf/10.1145/3316781.3317808)
* article with concrete nanosecond latency numbers [here](https://www.anandtech.com/show/16594/intel-3rd-gen-xeon-scalable-review/4)
* Intel Xeon also seems to be such a processor, e.g. xeon gold
* Neoverse chips also use mesh [here](https://fuse.wikichip.org/news/5006/the-mesh-network-for-next-generation-neoverse-chips/)
* AMD Opteron is also a processor, with hyper-transport thing (sub 10ns latency?). See [this](https://www.sciencedirect.com/topics/earth-and-planetary-sciences/distributed-memory)
* SO thread on inter-core stuff [here](https://stackoverflow.com/questions/57670764/different-inter-core-latency-measured-on-two-identical-skylake-xeon-gold-6154-sy)
* More intel xeon stuff:
  * [article 1](https://www.eteknix.com/intel-xeon-gold-6146-12-core24-thread-processor-review/9/)
  * [paper](https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.740.3541&rep=rep1&type=pdf)
* [Another paper](https://arxiv.org/pdf/2012.02973.pdf)



Dear Professor R. D. Mullins,

For my dissertation, I am implementing a parallel algorithm on a simulation of a multiprocessor with a distributed memory model and a mesh interconnect architecture. To evaluate the performance of this algorithm, my plan is to estimate the communication and computation time it would take to execute on the simulated multiprocessor. I want to find such an estimate my using realistic values for memory latency and bandwidth, but I am having trouble finding such values. In particular, I am looking for the latency associated with one core sending some amount of data to an adjacent core in the mesh interconnect. Do you know of any good sources of realistic values for such latency?

Trying to look up these values, for example on the Intel skylake server microarchitecture [1], I am unable to find values for this type of "mesh interconnect" latency. I am wondering if I am missing something fundamental with this interconnect architecture. Do cores communicate by writing to the closest shared cache, instead of using some inter-core message passing API? Or is passing data to adjacent cores simply an instruction provided by the "multiprocessor ISA", in which case it would only take a few clock cycles?

Best wishes,

Marcus



<!-- vim: set nospell: -->
