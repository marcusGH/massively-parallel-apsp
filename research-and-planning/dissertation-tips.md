# Dissertation writing tips

[The Pink Book](https://www.cl.cam.ac.uk/teaching/projects/pinkbook.pdf)

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
