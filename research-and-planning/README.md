# Research and planning

[Dissertation writing tips](dissertation-tips.md)

## Resources

**Past projects:**

[vk313, 2017](https://www.cl.cam.ac.uk/teaching/projects/archive/2017/vk313-dissertation.pdf)

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

## Planning

[Multiprocessor simulation](multiprocessor-simulation.md)

...

## Research plan

% TODO: continue on from 8.4 in the pink book

- [x] Read _The Pink Book_, watch the "How to write a dissertation" lecture
      (and other material?) with the aim of answering the question: "What makes
      a good dissertation?"
- [o] Read all 4 of past projects, taking note of:
  * Their references/bibliography
  * What major items they put into the 5 chapters
  * Their method of evaluation
- [ ] Area of knowledge to further expand on/read about:
  - [ ] General knowledge about _parallel_ computing concepts
  - [ ] Evaluating parallel performance
  - [ ] General overview of the different parallel matrix multiplication techniques
  - [ ] How does message passing work in these multiprocessors?:
    * Are there packets being sent around, and they queue up at manager thread?
    * Does it communicate with manager thread through shared memory, requiring locking?

**Milestones:**
- [ ] Got hold of Modi's book from library
- [ ] Written some small fragments of Java code that uses concepts
  * _Remember: important part of preparation phase is getting practice in what you'll do in the main implementation phase_
- [ ] The _Work plan_ is fleshed out in a lot more detail

## Preparation plan

- [ ] Plan code structure and `make` file
- [ ] Plan specific evidence in evaluation (recollect ideas from notebook on this)
