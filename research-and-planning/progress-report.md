# Progress report

Deadline: **February 4th**

Written report: 300 - 500 words
Oral presentation: about 5 minutes (no more than4 slides)

Contents (excluding formalities):
* An indication of what work has been completed and how this relates to the
  timetable and work plan in the original proposal. The progress report
  should answer the following questions:
  * Is the project on schedule and if not, how many weeks behind or ahead?
  * What unexpected difficulties have arisen?
  * If the project is behind, what actions have been taken to address this and when will progress be back on track?
  * Briefly, what has been accomplished?

_It should be possible to understand the progress report independently of the_
_original proposal, thus 'I have completed implementing the wombat module'_
_rather than 'I have completed points 1 and 3 in the proposal but not point 2'._

## Planning

According to timetable, by then I should be finished with the following
main items of work:
* Data set extractor, preparer modules
* Multiprocessor simulation
* APSP driver algorithm (also reconstructs paths)
* FoxOtto implementation
* **Evaluated performance**
* (tested for correctness)

What I thus definitely need to do before report due is the **evaluation constants** and
done some more code on extracting all the timings, making a module for aggregating these
into files (`.csv`s) that can be plotted...

To be ahead of schedule, I need to have started the generalising part etc.


Questions:
* Who is the target? Should I assume audience know proposal? Or introduce project?

## The report itself

Introduction (content: the project is on schedule, no unexpected difficulties,
brief summary of main paragraph)

The project is currently on schedule, and work items have been completed as they
were described on the timetable. There has not been any unexpected difficulties with
implementation. Additionally, all the milestones of the work items leading up
to, but not including, evaluation have been met. I am currently doing evaluation,
which is in line with the timetable has the period 20 Jan to 2 Feb is set of
for evaluation.

I have implemented a python script and Java class to, respectively, download
and transform various graph datasets into the appropriate format to feed into
the all-pairs shortest paths (APSP) algorithm.  I have also finished the
multiprocessor simulation; It now provides the programmer with an interface
where they can program what a general processing element (PE) $PE(i, j)$ should do
during its computation and communication phases, having access to methods such
as `send(other_i, other_j, data)` and `broadcastRow(data)`. This description
can then be passed onto a `Manager` which instantiates workers according to the
description, loads the initial memory content, and runs them until they have
completed a specified number of phases. It also handles their communication
using a `MemoryController` that I have implemented.  I have also implemented
the `FoxOtto` algorithm, which runs on the multiprocessor simulator.
Additionally, I have finished implementing the main APSP algorithm, which uses
FoxOtto min-plus matrix multiplication as a subroutine.  I have also thoroughly
tested for correctness by manually creating small example graphs and verifying
that the shortest paths found are correct, and I have also run it on a large
graph with 250 nodes and compared the results with the output of a serial
Dijkstra algorithm I am certain is correct.

_Note on above: for the slides, add a slide on example code using multiprocessor interface_

Currently, I am working on the evaluation of the algorithm. I have implemented timing and
communication-counting wrappers, and used these to estimate the computation and communication
time required by the algorithm. I have also been researching how the algorithm implemented would
map onto real multiprocessor hardware, and looked into what the memory latency
and bandwidth are on such hardware. This will be used to create a more realistic estimate
of the communication time. What remains to be
done as part of evaluation is to do more measurements for graphs of various sizes to get measures
for how the algorithm's performance scales. The current progress is thus in line with
the timetable as the period 20 Jan -- 2 Feb is set of for evaluation.

Going forward, I will finish evaluation, then aim to complete two of my extensions, one of
which I'm more than halfway done with: Graph compression through removal of 2-degree nodes.
The other extension is generalizing the implementation to also handle the case where each PE handles
a sub-matrix of size greater than 1 x 1.
