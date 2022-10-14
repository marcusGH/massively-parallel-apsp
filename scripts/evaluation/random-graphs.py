from fa2 import ForceAtlas2
from functools import partial

import matplotlib.pyplot as plt

import networkx as nx
import random


def create_connected_graph(n, p):
    g = nx.erdos_renyi_graph(n, p, directed=False)
    # add edges so the graph is fully connected (make a circle)
    for i in range(1, n):
        g.add_edge(i - 1, i)
    g.add_edge(n - 1, 0)
    # we then assign uniform weights
    for u, v in g.edges:
        g[u][v]['weight'] = random.uniform(0, 1)
    return g


def print_graph(g):
    for u, v, data in g.edges.data():
        print(f"{u} - {v} (w: {data['weight']:.5})")


def visualise_graph(g):
    forceatlas2 = ForceAtlas2(
        scalingRatio=100.0,
        gravity=0.8
    )
    pos = forceatlas2.forceatlas2_networkx_layout(g, pos=None, iterations=1000)
    nx.draw_networkx_nodes(g, pos, node_size=20, node_color="blue", alpha=1)
    nx.draw_networkx_edges(g, pos, edge_color="lightblue", alpha=1)
    plt.axis('off')
    plt.savefig('plots/example-graph.eps', format='eps')
    plt.show()


def create_graph_from_degree(n, avg_degree):
    p = max(0, avg_degree - 2) / (n - 3)
    return create_connected_graph(n, p)


def save_graph(g, filename):
    with open(filename, "w") as f:
        for i, (u, v, data) in enumerate(g.edges.data()):
            line = f"{i} {u} {v} {data['weight']:.5}\n"
            f.write(line)

def create_and_save_graphs(graph_f, N_values, base_filename):
    for n in N_values:
        save_graph(graph_f(n), f"{base_filename}/{n}.cedge")

if __name__ == '__main__':
    cal_compressed_avg_degree = 2.9450549450549453
    global calGraph
    calGraph = partial(create_graph_from_degree, avg_degree=cal_compressed_avg_degree)

