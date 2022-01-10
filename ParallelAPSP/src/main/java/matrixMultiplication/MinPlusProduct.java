package matrixMultiplication;

import memoryModel.MemoryController;
import memoryModel.PrivateMemory;
import work.Algorithm;

public abstract class MinPlusProduct extends Algorithm {
    public MinPlusProduct(int i, int j, int p, int n, int numPhases,
                          PrivateMemory privateMemory, MemoryController memoryController) {
        super(i, j, p, n, numPhases, privateMemory, memoryController);
    }
}
