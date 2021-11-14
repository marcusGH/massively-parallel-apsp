import matplotlib.pyplot as plt
import numpy as np
import csv

# read the recorded nanoseconds values
beanTimes = np.array([])
systemTimes = np.array([])
with open("beanTimeExecution.csv") as csvfile:
    for row in csv.reader(csvfile):
        beanTimes = np.array(row).astype(np.int)
with open("systemTimeExecution.csv") as csvfile:
    for row in csv.reader(csvfile):
        systemTimes = np.array(row).astype(np.int)

print(f"bean times: mu={np.mean(beanTimes)} sigma={np.std(beanTimes)}")
print(f"system times: mu={np.mean(systemTimes)} sigma={np.std(systemTimes)}")

fig, axs = plt.subplots(nrows=2,figsize=(7,14))
# beans
axs[0].hist(beanTimes,bins=100)
axs[0].title.set_text("ThreadMXBean times")
axs[0].set_xlabel("nanoseconds")
axs[0].tick_params(labelrotation=45)
axs[0].set_yscale('log')
# axs[0].set_xlim([0, np.mean(beanTimes)+2*np.std(beanTimes)])
# system
axs[1].hist(systemTimes,bins=100)
axs[1].set_title("System times")
axs[1].set_xlabel("nanoseconds")
axs[1].tick_params(labelrotation=45)
axs[1].set_yscale('log')
# axs[1].set_xlim([0, np.mean(beanTimes)+2*np.std(beanTimes)])

fig.suptitle("Exeuction times of 1000^2 threads for matrix multiplication")
plt.show()





