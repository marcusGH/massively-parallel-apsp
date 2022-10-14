import matplotlib.pyplot as plt
import numpy as np
import csv

def read_csv(filename):
    times = np.array([])
    with open(filename) as csvfile:
        for row in csv.reader(csvfile):
            times = np.array(row).astype(np.int)
    return times

xs = np.array([50,100,200,300,400,500,600,700,800,900,1000])
types = ["system", "bean"]

mus = [[], []]
sigmas = [[], []]
for i, t in enumerate(types):
    y = int(i)
    for x in xs:
        times = read_csv(f"execution-time-{t}-time-{x}.csv")
        mus[i].append(np.mean(times))
        sigmas[i].append(np.std(times))

fig, axs = plt.subplots(ncols=2,figsize=(16,8))

(_, caps, _) = axs[0].errorbar(xs, mus[0], yerr=sigmas[0], fmt='D', markersize=5, capsize=10)
for cap in caps:
    cap.set_markeredgewidth(2)
(_, caps, _) = axs[1].errorbar(xs, mus[1], yerr=sigmas[1], fmt='D', markersize=5, capsize=10)
for cap in caps:
    cap.set_markeredgewidth(2)
axs[0].plot(xs,mus[0],'-')
axs[1].plot(xs,mus[1],'-')
axs[0].set_xlabel("x")
axs[1].set_xlabel("x")
axs[0].title.set_text("System.nanotime()")
axs[1].title.set_text("ThreadMXBean")
axs[0].set_ylabel("nanoseconds")
axs[1].set_ylabel("nanoseconds")
fig.suptitle("Computation time for matrix multiplication of x by x matrix")
plt.show()

# print(f"bean times: mu={np.mean(beanTimes)} sigma={np.std(beanTimes)}")
# print(f"system times: mu={np.mean(systemTimes)} sigma={np.std(systemTimes)}")
#
# fig, axs = plt.subplots(nrows=2,figsize=(7,14))
# # beans
# axs[0].hist(beanTimes,bins=100)
# axs[0].title.set_text("ThreadMXBean times")
# axs[0].set_xlabel("nanoseconds")
# axs[0].tick_params(labelrotation=45)
# axs[0].set_yscale('log')
# # axs[0].set_xlim([0, np.mean(beanTimes)+2*np.std(beanTimes)])
# # system
# axs[1].hist(systemTimes,bins=100)
# axs[1].set_title("System times")
# axs[1].set_xlabel("nanoseconds")
# axs[1].tick_params(labelrotation=45)
# axs[1].set_yscale('log')
# # axs[1].set_xlim([0, np.mean(beanTimes)+2*np.std(beanTimes)])
#
# fig.suptitle("Exeuction times of 1000^2 threads for matrix multiplication")
# plt.show()
#
#
#
#
#
