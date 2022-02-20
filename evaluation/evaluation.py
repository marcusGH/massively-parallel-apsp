import matplotlib.pyplot as plt
import math
import numpy as np
import pandas as pd

def plot_performance_scaling(filenames):
    fig, axs = plt.subplots(ncols=2, figsize=(10, 5))

    xs = []
    ys = []
    err = []

    for i, file in enumerate(filenames):
        compute_df, communicate_df, p = read_timings(file)

        compute_time = compute_df.sum()
        error_std = compute_df.std()
        communicate_time = (communicate_df["point_to_point"] + communicate_df["row_broadcast"] + communicate_df["col_broadcast"]) * 100  # TODO: extract data from files

        # add to plot
        xs.append(p)
        print("{0} err={1}".format(communicate_time + compute_time, error_std))
        print("ratio: {0}".format(compute_time / (communicate_time + compute_time)))
        ys.append(np.log(compute_time + communicate_time))
        err.append(np.log(error_std))

    axs[0].set_title("Total computation and communication time for various input sizes")
    axs[0].set_xlabel("Problem size")
    axs[0].set_ylabel("Execution time in nanoseconds")
    axs[0].errorbar(x=xs, y=ys, yerr=err, marker="D", markersize=6, capsize=5, elinewidth=2)

    plt.show()


def read_timings(filename):
    return pd.read_csv(filename)

def find_compute_ratio(df):
    df['finish_time'] = df["computation_time"] + df['total_communication_time']
    finish_time = max(df['finish_time'])
    print(finish_time)
    df['total_communication_time'] = df['total_communication_time'] + (finish_time - df['finish_time'])

    communication = sum(df['total_communication_time'])
    compute = sum(df['computation_time'])
    return compute / (compute + communication)


def plot_ratio_scaling(filenames):
    fig, ax = plt.subplots(figsize=(5, 7))
    ax.set_ylim([0, 1])

    xs = []
    ys = []
    for f in filenames:
        df = read_timings(f)
        xs.append(math.sqrt(df['n'].iloc[0]))
        ys.append(find_compute_ratio(df))
        print(xs, ys)

    ax.plot(xs, ys)
    plt.show()

def plot_ratio(timing_df):
    compute_df = timing_df["computation", "computation"]
    # merge the before and after data
    communicate_df = timing_df["communication_before"] + timing_df["communication_after"]
    # mean computation time by phase
    compute_times = compute_df.groupby("phase").mean()
    compute_err = compute_df.groupby("phase").std()
    # mean communication time by phase
    communicate_times = communicate_df.groupby("phase").mean().sum(axis=1)

    stats_df = pd.concat([compute_times, communicate_times, compute_err], axis=1)
    stats_df.columns = ["computation", "communication", "error"]

    stats_df["computation_ratio"] = stats_df["computation"] / (stats_df["computation"] + stats_df["communication"])
    stats_df["error_ratio"] = stats_df["error"] / (stats_df["computation"] + stats_df["communication"])

    fig, ax = plt.subplots(figsize=(5, 4))
    ax.set_title("Computation to communication ratio in different phases")
    ax.set_xlabel("Phase")
    ax.set_ylabel("Computation to communication ratio")
    ax.set_ylim([0, 1])
    ax.errorbar(x=stats_df["computation_ratio"].index,
                y=stats_df["computation_ratio"], yerr=stats_df["error_ratio"],
                marker="D", markersize=6, capsize=5, elinewidth=2)
    # annotate the values
    for i, v in enumerate(stats_df["computation_ratio"]):
        ax.text(i, v - 0.05, "%.2f" % v, ha="left")
    plt.show()

    return stats_df


def find_overall_ratio(timing_df):
    # all the phases happen in parallel
    phase_agg_df = timing_df.groupby("phase").mean()
    # find the time to compute everything
    computation_time = phase_agg_df["computation"].sum()
    # and total communication time
    communication_time = phase_agg_df["communication_before"].sum() + phase_agg_df["communication_after"].sum()
    return computation_time / (computation_time + communication_time.sum())


if __name__ == "__main__":
    global df
    df = read_timings("timing-data/cal-50.csv")
    # plot_ratio(df)
