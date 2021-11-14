# Multiprocessor simulation framework

...

## On the evaluation framework

* If we are going with MIMD, there are important factors to the timing of
  computation time that affects accuracy, so certain questions need to be
  answered through testing Java code:
  * If a thread is paused while timer within it is running, does the timer stop?
  * How different are the time measured ($\sigma$)? Do batch testing!
  * Does actual cache effects play a role? Should we have interface to make sure
    stuff is in memory before starting the timer?
  * _If we count the number of flops, and use time based on that, we basically do SIMD_
  * Since my clock speed is higher, should the communication bandwidth/latency be adjusted?
    * Can just say: _my processor, but we have interconnect technology from supercomputer X_
