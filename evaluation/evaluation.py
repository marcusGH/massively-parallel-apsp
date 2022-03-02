import matplotlib.pyplot as plt
from pathlib import Path
import math
import numpy as np
import pandas as pd


def read_and_compute_errors(filename):
    """
    This method also calculates the error if there are multiple iterations of the timings.
    :param filename: If the file is called "something-n-X-p-Y.d.csv", the parameter should be
                     "something-n-X-p-Y" i.e. excluding the [.] and what comes after.
    :return: a tuple of a dict and an array of dataframe. All the key-value pairs in the dict that are
             related to the computation time, communication time and total time is **summed up over all
             processing elements**. The finish_time key gives the time at which the multiprocessor actually
             finishes its computation.
    """

    timing_dfs = []
    compute_times = []
    communicate_times = []
    finish_times = []
    for i in range(9999):
        f = f"{filename}.{i}.csv"
        if Path(f).is_file():
            df = stall_until_last_one_finished(pd.read_csv(f))
            timing_dfs.append(df)
            compute_times.append(np.sum(df['computation_time'].to_numpy()))
            communicate_times.append(np.sum(df['total_communication_time'].to_numpy()))
            finish_times.append(np.max(df['finish_time'].to_numpy()))
        else:
            break
    print(f"Found {len(timing_dfs)} timings for the specified file")
    if len(timing_dfs) > 0:
        print("Dropping first iteration because usually much higher than rest.")
        timing_dfs = timing_dfs[0:]
        compute_times = np.array(compute_times[0:])
        communicate_times = np.array(communicate_times[0:])
        finish_times = np.array(finish_times[0:])

    # all this is used to approximate the error for the ratio
    computation = np.mean(compute_times)
    communication = np.mean(communicate_times)
    computation_var = np.var(compute_times)
    communication_var = np.var(communicate_times)
    # Var(X + Y) = Var(X) + Var(Y) + 2Cov(X, Y) (see sum of correlated variables on wikipedia)
    total_time = computation + communication
    # total_time_var = computation_var + communication_var + 2 * np.cov(communicate_times, compute_times)[1][0]
    # Var(R / S) = (mu_R / mu_S)^2 ( Var(R)/mu_R^2 - 2 Cov(R, S) / (mu_R mu_S) + Var(S)/mu_S^2)
    ratio = computation / total_time
    # NOTE: the ratio is only an approximation (see https://www.stat.cmu.edu/~hseltman/files/ratio.pdf)
    #       becuase the ratio random variable X / Y is often Cauchy and thus has undefined variance
    # ratio_var = (computation / total_time) ** 2 \
    #              * ((computation_var / (computation ** 2))
    #                 - 2 * np.cov(compute_times, compute_times + communicate_times)[0][1] / (computation * total_time)
    #                 + (communication_var / (communication ** 2)))

    # alternatively, ...
    ratio_var = np.var(compute_times / (compute_times + communicate_times))
    total_time_var = np.var(compute_times + communicate_times)

    # pack the means and stds up into a dict and return them along with an array of the
    #   timing dataframes
    return {
        'computation_time': computation,
        'computation_err': np.sqrt(computation_var),
        'communication_time': communication,
        'communication_err': np.sqrt(communication_var),
        'total_time': total_time,
        'total_time_err': np.sqrt(total_time_var),
        'ratio': ratio,
        'ratio_err': np.sqrt(ratio_var),
        'finish_time': np.mean(finish_times),
        'finish_time_err': np.std(finish_times),
    }, timing_dfs


def stall_until_last_one_finished(df):
    """
    Returns a new dataframe with updated communication times, taking into account that all
    PEs must stall until the last one has finished its computation.
    :param df:
    :return: pandas Dataframe
    """
    df['finish_time'] = df["computation_time"] + df['total_communication_time']
    finish_time = max(df['finish_time'])
    # all the processing elements must stall until the last one has finished its computation
    df['total_communication_time'] = df['total_communication_time'] + (finish_time - df['finish_time'])

    df['finish_time'] = df['computation_time'] + df['total_communication_time']
    return df

def plot_scaling(base_path, ns, ps, y_func, y_err_func):
    """
    Utility function for plotting scaling with legend labels etc.
    :param base_path: Should be on the form "somewhere/subpath" where there are files on the form
                    "somewhere/subpath-n-X-p-Y-.d.csv"
    :param ns:
    :param ps:
    :param y_func:
    :param y_err_func:
    :return:
    """
    fig, ax = plt.subplots(figsize=(5, 7))
    for p in ps:
        ys = []
        err = []
        for n in ns:
            timings, _ = read_and_compute_errors(f"{base_path}-n-{n}-p-{p}")
            ys.append(y_func(timings))
            err.append(y_err_func(timings))
        # plot scaling with errorbars
        ax.errorbar(ns, ys, yerr=err, label=f"{p} x {p} cores", capsize=7.0, fmt='', ls='--')
        # plot true scaling
        # xspace = np.linspace(min(ns), max(ns), 1000)
        # yspace = (xspace ** 3 * np.log(xspace)) * (ys[0] / (ns[0] ** 3 * np.log(ns[0])))
        # ax.plot(xspace, yspace, label="n^3 log n")

        # ys = []
        # err = []
        # # ignore later TODO: remove this
        # for n in ns:
        #     timings, _ = read_and_compute_errors(f"{base_path}-5-repeats-n-{n}-p-{p}")
        #     ys.append(y_func(timings))
        #     err.append(y_err_func(timings))
        # # plot scaling with errorbars
        # ax.errorbar(ns, ys, yerr=err, label=f"{p} x {p} cores", capsize=7.0, fmt='', ls='--')
    # format plot
    ax.legend()
    return fig, ax

def plot_total_time_scaling(base_path, ns, ps):
    # nanoseconds to milliseconds
    fig, ax = plot_scaling(base_path, ns, ps, lambda t: t['finish_time'] * 1E-6, lambda t: t['finish_time_err'] * 1E-6)
    ax.set_yscale('log', nonpositive='clip')
    ax.set_xlabel("Problem size in number of graph vertices")
    ax.set_ylabel("Time (ms)")
    ax.set_title("Total execution time")
    plt.show()


def plot_ratio_scaling(base_path, ns, ps):
    fig, ax = plot_scaling(base_path, ns, ps, lambda t: t['ratio'], lambda t: t['ratio_err'])
    ax.set_xlabel("Problem size in number of graph vertices")
    ax.set_ylabel("Parallel efficiency")
    ax.set_ylim([0, 1.0])
    ax.set_title("Parallel efficiency")
    plt.show()


if __name__ == "__main__":
    # global df
    # df = read_timings("timing-data/cal-random-sandy-bridge-n-20-p-4")
    # plot_ratio(df)
    ns = list(range(10, 101, 10)) + list(range(100, 701, 50))
    plot_total_time_scaling("timing-data/cal-random-sandy-bridge", ns, [8])
    # plot_ratio_scaling("timing-data/cal-random-sandy-bridge", ns, [8, 16, 32])
