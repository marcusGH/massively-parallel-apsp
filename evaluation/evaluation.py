import matplotlib
import matplotlib.pyplot as plt
from pathlib import Path
import math
import numpy as np
import pandas as pd

# Match dissertation parameters
from matplotlib.legend_handler import HandlerLine2D

tex_fonts = {
    # Use LaTeX to write all text
    "text.usetex": True,
    "font.family": "serif",
    # Use 12pt font in plots, to match 12pt font in document
    "axes.labelsize": 12,
    "font.size": 12,
    # Make the legend/label fonts a little smaller
    "legend.fontsize": 8,
    "legend.title_fontsize": 10,
    "xtick.labelsize": 8,
    "ytick.labelsize": 8
}
plt.rcParams.update(tex_fonts)


def set_size(width=455.24411, fraction=1.0, ratio=None):
    """Set figure dimensions to avoid scaling in LaTeX.

    This function is written by Jack Walton, fetched from his blogpost:
    https://jwalton.info/Embed-Publication-Matplotlib-Latex/

    Parameters
    ----------
    width: float
            Document textwidth or columnwidth in pts
    fraction: float, optional
            Fraction of the width which you wish the figure to occupy

    Returns
    -------
    fig_dim: tuple
            Dimensions of figure in inches
    """
    # Width of figure (in pts)
    fig_width_pt = width * fraction

    # Convert from pt to inches
    inches_per_pt = 1 / 72.27

    # Golden ratio to set aesthetic figure height
    # https://disq.us/p/2940ij3
    golden_ratio = (5 ** .5 - 1) / 2

    # Figure width in inches
    fig_width_in = fig_width_pt * inches_per_pt
    # Figure height in inches
    fig_height_in = fig_width_in * golden_ratio
    if ratio is not None:
        fig_height_in = fig_width_in * ratio

    fig_dim = (fig_width_in, fig_height_in)

    return fig_dim


class SymHandler(HandlerLine2D):
    """
    Class written by ImportanceOfBeingErnest.
    Used to align text baseline width line image, taken
    from: https://stackoverflow.com/questions/42103144/how-to-align-rows-in-matplotlib-legend-with-2-columns
    """

    def create_artists(self, legend, orig_handle, xdescent, ydescent, width, height, fontsize, trans):
        xx = 0.6 * height
        return super(SymHandler, self).create_artists(legend, orig_handle, xdescent, xx, width, height, fontsize, trans)

    @staticmethod
    def get_legend_func():
        return lambda ax: ax.legend(handler_map={matplotlib.lines.Line2D: SymHandler()},
                                    fontsize='6', title="Processing element layout", ncol=2, loc='upper left', handleheight=2, labelspacing=0.05, prop={'size':8})


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
    # print(f"Found {len(timing_dfs)} timings for the specified file")
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


def read_and_compute_error_serial(filename):
    """
    The filename must point to a timing data file for where p=1
    :param filename:
    :return:
    """
    compute_times = []
    for i in range(9999):
        f = f"{filename}.{i}.csv"
        if Path(f).is_file():
            df = pd.read_csv(f)
            compute_times.append(df['computation_time'])
        else:
            break
    compute_times = np.array(compute_times)
    assert len(compute_times) > 0
    return {
        'computation_time': np.mean(compute_times),
        'computation_err': np.std(compute_times),
        'ratio': 1.0,
        'ratio_err': 0.0,
        'finish_time': np.mean(compute_times),
        'finish_time_err': np.std(compute_times),
    }


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


def plot_with_errorbars(ax, ns, ys, err, label, plot_kwargs=None, use_caps=False):
    """
    Utility function for formatting the line segment plot
    :param ax: 
    :param ns: 
    :param ys: 
    :return: 
    """
    if plot_kwargs is None:
        plot_kwargs = {}
    err = np.array(err)
    ys = np.array(ys)
    if use_caps:
        _, caps, bars = ax.errorbar(ns, ys, yerr=err, label=label, elinewidth=2, capsize=3.0, ls=':', **plot_kwargs)
        # [bar.set_alpha(0.3) for bar in bars]
        [bar.set_zorder(-10) for bar in bars]
        # [cap.set_alpha(0.3) for cap in caps]
        [cap.set_zorder(-10) for cap in caps]
    else:
        ax.plot(ns, ys, 'D--', markersize=4, label=label, linewidth=1, **plot_kwargs)
        if 'c' in plot_kwargs:
            plot_kwargs['color'] = plot_kwargs['c']
            plot_kwargs.pop('c')
        ax.fill_between(ns, ys - err, ys + err, alpha=0.3, **plot_kwargs)

    SymHandler.get_legend_func()(ax)
    # ax.legend(loc='lower right', prop={'size': 4})


def plot_scaling(base_path, ns, ps, y_func, y_err_func, use_caps=False):
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
    fig, ax = plt.subplots(figsize=set_size(fraction=1.0))
    for p in reversed(ps):
        ys = []
        err = []
        for n in ns:
            timings, _ = read_and_compute_errors(f"{base_path}-n-{n}-p-{p}")
            ys.append(y_func(timings))
            err.append(y_err_func(timings))
        # plot scaling with errorbars
        plot_with_errorbars(ax, ns, ys, err, label=fr"${p} \times {p}$", use_caps=use_caps)

    # then plot the serial version
    ys = []
    err = []
    for n in ns:
        # temp override for sunway, because serial is the same for both
        if base_path == "timing-data/cal-random-sunway-light/cal-random-sunway-light-5-repeats":
            base_path = "timing-data/cal-random-sandy-bridge/cal-random-sandy-bridge-5-repeats"
        timings = read_and_compute_error_serial(f"{base_path}-n-{n}-p-1")
        ys.append(y_func(timings))
        err.append(y_err_func(timings))
    plot_with_errorbars(ax, ns, ys, err, label="Sequential", plot_kwargs={'c': 'k'}, use_caps=use_caps)
    return fig, ax


def plot_total_time_scaling(base_path, ns, ps):
    # nanoseconds to milliseconds
    fig, ax = plot_scaling(base_path, ns, ps, lambda t: t['finish_time'] * 1E-6, lambda t: t['finish_time_err'] * 1E-6, use_caps=True)
    ax.set_yscale('log', nonpositive='clip')
    ax.set_xscale('log',  nonpositive='clip')
    ax.set_xlabel(r"Problem size (number of vertices, $| V |$): $n$", fontsize=10)
    ax.set_ylabel("Time (ms)", fontsize=10)
    # ax.set_title("Total execution time by MatSquare", fontsize=10)
    ax.set_xticks([10,20,30,40,50,60,70,80,90] + list(range(100,701,100)))
    ax.tick_params(axis='x', labelrotation=45)
    ax.get_xaxis().set_major_formatter(matplotlib.ticker.ScalarFormatter())
    fig.tight_layout(pad=0.8)
    #fig.set_constrained_layout_pads(w_pad=2, h_pad=2)
    #fig.savefig('plots/total-time-scaling-taihu-full-width.pdf', format='pdf', bbox_inches='tight')
    plt.show()


def plot_ratio_scaling(base_path, ns, ps):
    fig, ax = plot_scaling(base_path, ns, ps, lambda t: t['ratio'], lambda t: t['ratio_err'])
    ax.set_xlabel("Problem size", fontsize=8)
    ax.set_ylabel("Parallel efficiency", fontsize=8)
    ax.set_ylim([0.0, 1.0])
    ax.set_xlim([0, 710])
    ax.set_xticks(list(range(0,701,100)))
    ax.set_title("Parallel efficiency", fontsize=12)
    plt.tight_layout()
    #fig.savefig('plots/example4.pdf', format='pdf', bbox_inches='tight')
    plt.show()


def plot_ratio_bucket(base_path, ns, ps):
    classes = []
    for p in ps:
        classes.append({
            'p': p,
            'xs': [],
            'ys': [],
            'col': [],
        })
    # the maximum number of submatrix size per processing element
    xs = list(range(1, 1 + math.ceil(max(ns) / min(ps))))
    means = [[] for _ in range(len(xs) + 1)]

    # read all the files
    for n in ns:
        for class_id, p in enumerate(ps):
            for i in range(9999):
                # skip the ones we did again for p=8
                # if p == 8 and i != 1:
                #     continue
                f = f"{base_path}-n-{n}-p-{p}.{i}.csv"
                if Path(f).is_file():
                    df = stall_until_last_one_finished(pd.read_csv(f))
                    compute = np.sum(df['computation_time'].to_numpy())
                    communicate = np.sum(df['total_communication_time'].to_numpy())
                    ratio = compute / (communicate + compute)
                    # the number of elements per PE
                    classes[class_id]['xs'].append(math.ceil(n / p))
                    # the parallel efficiency
                    classes[class_id]['ys'].append(ratio)
                    means[math.ceil(n / p)].append(ratio)
                    # colour
                    col_id = int(np.log2(p) - 2)
                    classes[class_id]['col'].append(f"C{col_id}")
                else:
                    break
    fig, ax = plt.subplots(figsize=set_size())
    # setup the plot
    ax.set_xscale('log', nonpositive='clip')
    ax.set_xlabel("submatrix size per procesing element")
    ax.set_ylabel("Parallel efficinecy")
    ax.set_title("Parallel efficiency for different submatrix sizes")
    # scatter for the different multiprocessor classes
    for mpc in classes:
        ax.scatter(mpc['xs'], mpc['ys'], c=mpc['col'], s=8, marker='x',
                   label=f"{mpc['p']} x {mpc['p']}",
                   alpha=0.5, edgecolors='none')
    # produce a legend
    leg = ax.legend(loc="lower right", title="Multiprocessor size")
    for lh in leg.legendHandles:
        lh.set_alpha(1)
        lh._sizes = [14]
    # overlay a mean across the points
    xs2 = []
    ys2 = []
    err = []
    for x, y in zip(xs, means[1:]):
        if len(y) > 0:
            xs2.append(x)
            ys2.append(np.mean(y))
            err.append(np.std(y))
    ys2 = np.array(ys2)
    err = np.array(err)
    ax.plot(xs2, ys2, zorder=-8)
    # error behind scatter
    ax.fill_between(xs2, ys2 - err, ys2 + err, zorder=-10, alpha=0.3)
    #fig.savefig('plots/example1.pdf', format='pdf', bbox_inches='tight')
    plt.show()
    
def plot_cal():
    path = "timing-data/cal-real-sandy-bridge/cal-real-sandy-bridge" #-p-4.0.csv"
    y_func, y_err_func = lambda t: t['finish_time'] * 1E-6, lambda t: t['finish_time_err'] * 1E-6
    fig, ax = plt.subplots(figsize=(7,7))
    ys = []
    err = []
    ps = [4,8,16,32,64,128,256]
    for p in ps:
        timings, _ = read_and_compute_errors(f"{path}-p-{p}")
        ys.append(y_func(timings))
        err.append(y_err_func(timings))
    # plot scaling with errorbars
    plot_with_errorbars(ax, ps, ys, err, label="Time")
    ax.set_yscale('log', nonpositive='clip')
    ax.set_xscale('log', nonpositive='clip')
    plt.show()

def plot_cal2():
    ps = [4,8,16,32,64,128,256]
    path = "timing-data/cal-real-sandy-bridge/cal-real-sandy-bridge" #-p-4.0.csv"
    classes = []
    for p in ps:
        classes.append({
            'p': p,
            'xs': [],
            'ys': [],
            'col': [],
        })
    # the maximum number of submatrix size per processing element
    xs = list(range(1, 1 + math.ceil(1362 / min(ps))))
    means = [[] for _ in range(len(xs) + 1)]

    # read all the files
    for class_id, p in enumerate(ps):
        for i in range(9999):
            f = f"{path}-p-{p}.{i}.csv"
            if Path(f).is_file():
                df = stall_until_last_one_finished(pd.read_csv(f))
                compute = np.sum(df['computation_time'].to_numpy())
                communicate = np.sum(df['total_communication_time'].to_numpy())
                ratio = compute / (communicate + compute)
                # the number of elements per PE
                classes[class_id]['xs'].append(math.ceil(1362 / p))
                # the parallel efficiency
                classes[class_id]['ys'].append(ratio)
                means[math.ceil(1362 / p)].append(ratio)
                # colour
                col_id = int(np.log2(p) - 2)
                classes[class_id]['col'].append(f"C{col_id}")
            else:
                break
    fig, ax = plt.subplots(figsize=set_size())
    # setup the plot
    ax.set_xscale('log', nonpositive='clip')
    ax.set_ylim([0, 1])
    ax.set_xlabel("submatrix size per procesing element")
    ax.set_ylabel("Parallel efficinecy")
    ax.set_title("Parallel efficiency for different submatrix sizes")
    # scatter for the different multiprocessor classes
    for mpc in classes:
        ax.scatter(mpc['xs'], mpc['ys'], c=mpc['col'], s=8, marker='x',
                   label=f"{mpc['p']} x {mpc['p']}",
                   alpha=0.5, edgecolors='none')
    # produce a legend
    leg = ax.legend(loc="lower right", title="Multiprocessor size")
    for lh in leg.legendHandles:
        lh.set_alpha(1)
        lh._sizes = [14]
    # overlay a mean across the points
    xs2 = []
    ys2 = []
    err = []
    for x, y in zip(xs, means[1:]):
        if len(y) > 0:
            xs2.append(x)
            ys2.append(np.mean(y))
            err.append(np.std(y))
    ys2 = np.array(ys2)
    err = np.array(err)
    ax.plot(xs2, ys2, zorder=-8)
    # error behind scatter
    ax.fill_between(xs2, ys2 - err, ys2 + err, zorder=-10, alpha=0.3)
    plt.show()

if __name__ == "__main__":
    # plot_cal()
    # global df
    # df = read_timings("timing-data/cal-random-sandy-bridge-n-20-p-4")
    # plot_ratio(df)
    ns = list(range(10, 101, 10)) + list(range(100, 701, 50))
    plot_total_time_scaling("timing-data/cal-random-sunway-light/cal-random-sunway-light-5-repeats", ns, [4, 8, 16, 32, 64, 128])
    #plot_ratio_scaling("timing-data/cal-random-sandy-bridge/cal-random-sandy-bridge-5-repeats", ns, [4, 8, 16, 32, 64, 128])
    #plot_ratio_bucket("timing-data/cal-random-sandy-bridge/cal-random-sandy-bridge-5-repeats", ns, [4, 8, 16, 32, 64, 128])
