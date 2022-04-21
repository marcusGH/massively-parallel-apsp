#!/usr/bin/env python3

import pandas as pd
import geopandas as gpd
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from shapely.geometry import Point, LineString

import sys, getopt

tex_fonts = {
        # Use LaTeX to write all text
        "text.usetex": True,
        "font.family": "serif",
        # Use 12pt font in plots, to match 12pt font in document
        "axes.labelsize": 12,
        "font.size": 12,
        # Make the legend/label fonts a little smaller
        "legend.fontsize": 8,
        # "legend.title_fontsize": 10,
        "xtick.labelsize": 10, # 8 at full scale
        "ytick.labelsize": 10
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


def parse_space_separated_file(filename):
    with open(filename) as f:
        for line in f:
            yield [x for x in re.split(r'\s+', line.strip())]

def plotGraph(ax, nodes_gdf, edges_gdf, alphaMult=1, extra_nodes=None, edge_col=None):
    if extra_nodes is None:
        # wacky heatmap attempt
        nodes_gdf.plot(ax=ax,alpha=0.005 * alphaMult, color="red", markersize=100)
    else:
        # plot first and last node
        mask = nodes_gdf.index.isin([extra_nodes[0], extra_nodes[-1]])
        nodes_gdf[mask].plot(ax=ax,alpha=1,color=edge_col,markersize=10,zorder=-5)
        # print(nodes_gdf.loc[extra_nodes])
        # mask = nodes_gdf.index.isin(extra_nodes)
        gpd.GeoSeries([LineString(nodes_gdf.loc[extra_nodes].geometry.tolist())]).plot(
                ax=ax,color=edge_col,alpha=1,zorder=-5,linewidth=1)
        # gpd.GeoSeries(nodes_gdf[mask].geometry[:50]).plot(ax=ax,color=edge_col)


    edges_gdf.plot(ax=ax,alpha=1 * alphaMult, color="black", linewidth=0.5,zorder=-6)
    # ax.set_title(f"Spatial plot of nodes and edges (|V|={len(nodes_gdf)}, |E|={len(edges_gdf)})")
    ax.set_xlabel("Longitude")
    ax.set_ylabel("Latitude")
    ax.set_rasterization_zorder(-3)

def main(argv):
    basefile = None
    output_file = None
    doPlot = False
    xloc, yloc = None, None
    dist = None
    pathFile = None
    try:
        opts, args = getopt.getopt(argv, 'hi:o:px:y:d:', ["ifile=", "ofile=", "plot", "xLoc=", "yLoc=", "dist=", "pathFile="])
    except getopt.GetoptError:
        print('graph-extractor.py -i <path-to-graph-base-file>')
        sys.exit(2)
    for opt, arg in opts:
        # input file
        if opt in ("-i", "--ifile"):
            basefile = arg
        # output file
        if opt in ("-o", "--ofile"):
            output_file = arg
        # plot
        if opt in ('-p', '--plot'):
            doPlot = True
        # x- and y-location
        if opt in ('-x', '--xLoc'):
            xloc = float(arg)
        if opt in ('-y', '--yLoc'):
            yloc = float(arg)
        if opt in ('-d', '--dist'):
            dist = float(arg)
        if opt in ("--pathFile"):
            pathFile = arg

    if basefile is None:
        print('graph-extractor.py -i <path-to-graph-base-file>')
        sys.exit(2)

    if xloc is not None or yloc is not None or dist is not None:
        if xloc is None or yloc is None or dist is None:
            print('graph-extractor.py -i <path-to-graph-base-file> -x <x> -y <y> -d <radius>')
            sys.exit(2)

    node_columns = ["node_id", "x", "y"]
    edge_columns = ["edge_id", "node1", "node2", "weight"]

    # read input
    try:
        nodes_df = pd.read_csv(f"{basefile}.cnode", sep=r'\s+', header=None, names=node_columns).set_index("node_id")
        edges_df = pd.read_csv(f"{basefile}.cedge", sep=r'\s+', header=None, names=edge_columns).set_index("edge_id")
    except IOError as e:
        print("I/O error({0}): {1}".format(e.errno, e.strerror))
        sys.exit(2)
    except: #handle other exceptions such as attribute errors
        print("Unexpected error:", sys.exc_info()[0])
        sys.exit(2)

    print(f"Found {len(nodes_df)} nodes and {len(edges_df)} edges.")

    path_nodes = None
    if pathFile is not None:
        path_nodes = []
        with open(pathFile) as f:
            for line in f:
                line = line.split(',')
                if line:
                    path_nodes.append([int(i) for i in line])

    # create the geodataframes
    nodes_gdf = gpd.GeoDataFrame(nodes_df, geometry=gpd.points_from_xy(nodes_df.x, nodes_df.y))

    # add geometry from the two nodes
    edges_df['node_id_1'] = edges_df['node1']
    edges_df['node_id_2'] = edges_df['node2']
    edges_merged_node1 = pd.merge(edges_df.set_index('node1'),
            nodes_gdf, left_index=True, right_index=True)
    edges_merged = pd.merge(edges_merged_node1.set_index('node2'),
            nodes_gdf, left_index=True, right_index=True)
    # create line geometry
    line_geometry = [LineString([a, b]) for (a,b) in zip(edges_merged.geometry_x, edges_merged.geometry_y)]
    # remove unwanted columns
    edges_gdf = gpd.GeoDataFrame(edges_merged, geometry=line_geometry)
    edges_gdf = edges_gdf.loc[:, edges_gdf.columns.intersection(['geometry', 'node_id_1', 'node_id_2', 'weight'])]


    if path_nodes is not None:
        fig, ax = plt.subplots(figsize=set_size(fraction=.5, ratio=1))
        cmap = plt.get_cmap("tab10")
        # cols = ["red", "blue", "green", "purple"]
        for i in range(4):
            print(f"The {i}th path has {len(path_nodes[i])} nodes")
            plotGraph(ax, nodes_gdf, edges_gdf, 0.1, path_nodes[i], cmap(i))

        custom_legends = [Line2D([0], [0], color=cmap(i), lw=1.5) for i in range(4)]
        # 1 -> 19000,
        # 10585 -> 9000,
        # 14000 -> 13520,
        # 60 -> 200
        ax.legend(custom_legends, [r"$1 \rightarrow^* 19000$", r"$10585 \rightarrow^* 9000$", r"$14000 \rightarrow^* 13520$", r"$60 \rightarrow^* 200$"])
        # ax.legend(custom_legends, ["a", "b", "c", "d"])
        fig.tight_layout()
        fig.savefig("test-figure-plot.pdf", format="pdf", dpi=800, bbox_inches='tight')


    elif doPlot and dist is None:
        fig, ax = plt.subplots(figsize=(13,13))
        plotGraph(ax, nodes_gdf, edges_gdf)
        plt.show()

    # filter out points
    elif dist is not None and doPlot:
        circle = Point(xloc, yloc).buffer(dist)
        fig, ax = plt.subplots(figsize=(13,13))
        plotGraph(ax, nodes_gdf, edges_gdf, 0.05)
        plotGraph(ax, nodes_gdf[nodes_gdf.within(circle)], edges_gdf[edges_gdf.within(circle)])
        plt.show()

    if output_file:
        if dist is None:
            print("graph-extractor.py -i <path-to-graph-base-file> -o <output-loc> -x <x> -y <y> -d <radius>")
            sys.exit(2)

        # filter
        circle = Point(xloc, yloc).buffer(dist)
        output_gdf = edges_gdf[edges_gdf.within(circle)].reset_index(drop=True)
        print(f"The remaining graph has {len(output_gdf)} edges")
        # save as file
        output_gdf.to_csv(output_file, columns=["node_id_1", "node_id_2", "weight"], sep=' ', header=False, float_format='%.7f')
        print(f"Successfully saved output to file {output_file}")

if __name__ == "__main__":
    main(sys.argv[1:])
