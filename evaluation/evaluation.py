import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


def read_timings_quick(file_basename):
    """
    The below method takes a long time because of the joins, so this one
    aggregates mean communication time as it reads, allowing less flexibility
    in exchange for faster processing
    :param file_basename:
    :return:
    """
    # read the computation file
    compute_times_df = pd.read_csv(file_basename + "_computation.csv", sep=",", header=None)
    # read the communication file
    communication_times_df = pd.read_csv(file_basename + "_communication.csv", sep=",", header=None)

    header_names = ["phase", "phase_name", "type"] + ["{0}".format(i) for i in range(compute_times_df.shape[1] - 3)]
    compute_times_df.set_axis(header_names, axis=1, inplace=True)
    communication_times_df.set_axis(header_names, axis=1, inplace=True)

    # make into numpy floats
    for i in range(compute_times_df.shape[1] - 3):
        compute_times_df["{0}".format(i)] = pd.to_numeric(compute_times_df["{0}".format(i)])
        communication_times_df["{0}".format(i)] = pd.to_numeric(communication_times_df["{0}".format(i)])
    # make multiindex
    return (compute_times_df.reset_index().set_index(["phase", "phase_name", "type"]).stack().unstack([1, 2]),
            communication_times_df.reset_index().set_index(["phase", "phase_name", "type"]).stack().unstack([1, 2]))


def plot_performance_scaling(filenames):
    fig, axs = plt.subplots(ncols=2, figsize=(10, 5))

    xs = []
    ys = []
    err = []

    for i, file in enumerate(filenames):
        compute_df, communicate_df = read_timings_quick(file)
        # remove this sneaky row which appears for some reason
        compute_df.drop('index', level=1, inplace=True)
        communicate_df.drop('index', level=1, inplace=True)

        phase_mean = compute_df.groupby("phase").mean()
        phase_var = compute_df.groupby("phase").var()
        phase_communicate_max = (communicate_df["communication_before"] +
                                 communicate_df["communication_after"]).groupby("phase").max()

        compute_time = phase_mean.sum(axis=0).values[0]
        error_std = np.sqrt(phase_var.sum(axis=0).values[0])
        communicate_time = phase_communicate_max.sum(axis=0).values[0]

        # add to plot
        xs.append(np.sqrt(compute_df.loc[0].shape[0]))
        print("{0} err={1}".format(communicate_time + compute_time, error_std))
        print("ratio: {0}".format(compute_time / (communicate_time + compute_time)))
        ys.append(compute_time + communicate_time)
        err.append(error_std)

    axs[0].set_title("Total computation and communication time for various input sizes")
    axs[0].set_xlabel("Problem size")
    axs[0].set_ylabel("Execution time in nanoseconds")
    axs[0].errorbar(x=xs, y=ys, yerr=err, marker="D", markersize=6, capsize=5, elinewidth=2)

    plt.show()

def read_timings(file_basename):
    # read the computation file
    compute_times_df = pd.read_csv(file_basename + "_computation.csv", header=None)
    # read the communication file
    point_to_point_df = pd.read_csv(file_basename + "_communication.csv", sep=",", header=None)

    header_names = ["phase", "phase_name", "type"] + ["{0}".format(i) for i in range(compute_times_df.shape[1] - 3)]
    compute_times_df.set_axis(header_names, axis=1, inplace=True)
    point_to_point_df.set_axis(header_names, axis=1, inplace=True)
    # combine the two dataframes vertically
    new_df = pd.merge(compute_times_df, point_to_point_df,
                      left_on=header_names, right_on=header_names, how='outer').set_index(["phase"]).sort_index()
    # remove whitespace
    new_df["phase_name"] = new_df["phase_name"].str.strip()
    new_df["type"] = new_df["type"].str.strip()
    # make into numpy floats
    for i in range(compute_times_df.shape[1] - 3):
        new_df["{0}".format(i)] = pd.to_numeric(new_df["{0}".format(i)])
    # make multiindex
    return new_df.reset_index().set_index(["phase", "phase_name", "type"]).stack().unstack([1, 2])


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
    print("h")
    df = read_timings("timing-data/test-file")
    print("Computation to communication ratio is: {0}".format(find_overall_ratio(df).sum()))
    # plot_ratio(df)
