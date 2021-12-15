#!/usr/bin/env python3

import pandas as pd
import geopandas as gpd
import matplotlib.pyplot as plt
from shapely.geometry import Point, LineString

import sys, getopt

def parse_space_separated_file(filename):
    with open(filename) as f:
        for line in f:
            yield [x for x in re.split(r'\s+', line.strip())]

def plotGraph(ax, nodes_gdf, edges_gdf, alphaMult=1):
    # wacky heatmap attempt
    nodes_gdf.plot(ax=ax,alpha=0.005 * alphaMult, color="red", markersize=100)
    edges_gdf.plot(ax=ax,alpha=1 * alphaMult, color="blue", zorder=2)
    ax.set_title(f"Spatial plot of nodes and edges (|V|={len(nodes_gdf)}, |E|={len(edges_gdf)})")
    ax.set_xlabel("x-axis")
    ax.set_ylabel("y-axis")

def main(argv):
    basefile = None
    output_file = None
    doPlot = False
    xloc, yloc = None, None
    dist = None
    try:
        opts, args = getopt.getopt(argv, 'hi:o:px:y:d:', ["ifile=", "ofile=", "plot", "xLoc=", "yLoc=", "dist="])
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

    if doPlot and dist is None:
        fig, ax = plt.subplots(figsize=(13,13))
        plotGraph(ax, nodes_gdf, edges_gdf)
        plt.show()

    # filter out points
    if dist is not None and doPlot:
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
