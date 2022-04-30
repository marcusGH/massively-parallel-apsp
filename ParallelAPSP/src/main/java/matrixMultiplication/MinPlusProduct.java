package matrixMultiplication;

import memoryModel.CommunicationManager;
import memoryModel.PrivateMemory;
import work.Worker;

public abstract class MinPlusProduct extends Worker {
    public MinPlusProduct(int i, int j, int p, int n, int numPhases,
                          PrivateMemory privateMemory, CommunicationManager communicationManager) {
        super(i, j, p, n, numPhases, privateMemory, communicationManager);
    }
}
