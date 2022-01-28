import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

def read_timings(file_basename):
    # read the computation file
    compute_times_df = pd.read_csv(file_basename + "_computation.csv", header=None)
    # read the communication file
    point_to_point_df = pd.read_csv(file_basename + "_communication.csv", header=None)

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
    return new_df

if __name__ == "__main__":
    read_timings("timing-data/test-file")