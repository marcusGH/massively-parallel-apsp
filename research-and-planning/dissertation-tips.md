# Dissertation writing tips

[The Pink Book](https://www.cl.cam.ac.uk/teaching/projects/pinkbook.pdf)

[Marking scheme](https://www.cst.cam.ac.uk/teaching/part-ii/projects/assessment)

## High-level stuff

* Plan evaluation and evidence (of successful project completion) early
* Write in notebook why **design decisions** were made
* Always remember to do "fine-grained" version control, especially when you're the most stressed out

## Content

### Introduction

Contains:
* principal motivation of the project
* how does work fit into area of CompSci?
* survey of related work

### Preparation

_describes work undertaken before code was written_ -> 26% of marks

Work items
* how was proposal further refined and clarified?
* describe algorithms/theories that required understanding
Important aspects
* motiviation: implementation stage go as smooth as possible
* demonstrate **professional approach** through use of SwEngSec techniques like:
  * Requirements analysis
  * High-level design planning
  * _Planning for extensions when creating modules e.g. Topology creator_
  * % TODO: review SwSecEng briefly to get survey of such techniques
  * ...

_Consider how well the candidate understood the task_
_and analysed it. Give credit for a good introduction to the technical background, a_
_coherent discussion of the problems and sensible planning._

### Implementation

_describes software produced_ -> 40% of marks

Good stuff:
* Design strategies that looked ahead to testing stage:
  * _Self: e.g. modular Java code, test-driven developement, interface/API predefined_

_Seek evidence of skill, clear thinking and common sense. Consider_
_how much work was carried out and take into account how challenging this was._

### Evaluation

_presents evidence of thorough and systematic evaluation_ + _conclusions_ -> 20% of marks

Discusses:
* What goals were achieved?
* What evidence can be used to back this?
* Did program work?

Relevant testing for me:
* Engineering performance through quantitative experiments:
  * Would be nice to have comparison with some baselines/existing solutions
* Functional performance, through systematic and reproducible testing procedures

Good stuff:
* Discuss limits of your evaluations

### Conclusions

_very short summary_

Good stuff:
* discuss how planned project if starting again with hindsight

## Notes from Ian lecture

Tips
* Nice to write early because then can let brew and have a look after
  a couple of weeks to tell if it makes sense or not
* Do the project justice! The examiner will just read the document,
  so if something is a big part of the project, make it appear as such
* If present result, also add **discussion** of what the significance
  of the result is. Don't just dump it there if there is no
  intellectual value in it.
* The examiner will read it quickly, so respect their time and present
  only the **key** parts of your project, and do it succinctly without
  much _waff_.
* Tell things up front, what did you do and why? Don't hide interesting stuff
* USE DIAGRAMS! Diagrams are underused and can be very valuable and can refer to them at times
* Use **signposts**: Where are you going? Why are you going there? And how are you going to get there?
* Say everything three times:
  * _Introduction_, main, _summarises_ (and not copy paste)
  * Chapter 1 overview, chapter 2-4 say it, chapter 5 summarises
  * Can also apply recursively in each chapter, but not extreme in each
    paragraph probably
* One valuable action is to have something written, let it sit for a week,
  then look at it again, so you can spot things

What are the _key points_ of a dissertation?
* What set out to do?
* What actually do?
* How did you do it?
* What are the results?
* How good are the results?
  * Put results into context and discuss significance of it

Audience:
* Assume know Computer Science knowledge, but not detailed knowledge
  in area of project (just finished part 1B)
* Demonstrate however that _you_ know detail in the area

Number of words per section (aim):
* Introduction & prep: ~500 + ~2500 words
  * The examiners should know what the project is about at a glance
    after reading this
  * ... see slide?
* Implementation: ~4500 words
  * _find right level of detail_ (maybe read past dissertation to gauge this?)
* Evaluation & Conclusion: ~2000 + ~500
  * See slides
  * _Is it possible to compare results with any existing methods/work done?_ [e.g. something like this](https://escholarship.org/content/qt4wv354s6/qt4wv354s6_noSplash_7e2fde6b8f65e4cccaa9c46dfc2c0954.pdf)
  * The diagrams don't need to be super fancy, but clearly convey some point you are trying to get across (thinking of implementaiton diagrams here)
  * Conclusion, mention further work, hindsight ideas etc.
* Professional practice:
  * Usually part of implementation, and marks for _demonstrating_
    that used suitable techniques and tools for **managing** the
    project well

Examiners are not required to read the appendices, so if you ever
_rely_ on it to get a point across, that's very bad because, again,
they don't have to read it.

## Notes from asking Ian about implementation focus

How much emphasis put on simulation part?
* The need to describe the design decisions yes. The point of the dissertation is
  to lead up onto whatever the result and significance is, so include what's relevant
  in the implementation chapter that does that. E.g. if you explain some aspect of 
  the functionality of the simulation, which is used to simulate e.g. certain timing
  behaviour, explain the design decision on why it become like that.
