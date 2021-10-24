# Research and planning

[Dissertation writing tips](dissertation-tips.md)

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

### Knowledge on matrix multiplication techniques

[Parallel methods for matrix multiplication](http://www.hpcc.unn.ru/mskurs/ENG/DOC/pp08.pdf)

[Overview, and then better approach](http://ilpubs.stanford.edu:8090/59/1/1994-25.pdf)

[Berntsen's algorithm](https://reader.elsevier.com/reader/sd/pii/0167819189900914?token=7DEA6AD6448CA99933B09C9CE7E16BC284B2B325CCF1EF50536455BC35E7F123C179A35A7F02DA4270137F105B3C6177&originRegion=eu-west-1&originCreation=20211024104716)


## Planning

[Multiprocessor simulation](multiprocessor-simulation.md)

...

## Research plan

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
  - [ ] Evaluating parallel performance
  - [ ] General overview of the different parallel matrix multiplication techniques
  - [ ] How does message passing work in these multiprocessors?:
    * Are there packets being sent around, and they queue up at manager thread?
    * Does it communicate with manager thread through shared memory, requiring locking?

**Milestones:**
- [x] Got hold of Modi's book from library
- [ ] Written some small fragments of Java code that uses concepts
  * _Remember: important part of preparation phase is getting practice in what you'll do in the main implementation phase_
- [ ] The _Work plan_ is fleshed out in a lot more detail

## Preparation plan

- [ ] Plan code structure and `make` file
- [ ] Plan specific evidence in evaluation (recollect ideas from notebook on this)
