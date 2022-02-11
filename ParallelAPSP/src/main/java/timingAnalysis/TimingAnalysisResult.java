package timingAnalysis;

import util.Matrix;

public class TimingAnalysisResult {

    private final TimingAnalysisMemoryController memoryController;

    public TimingAnalysisResult(TimingAnalysisMemoryController memoryController) {
        this.memoryController = memoryController;
    }

    public Matrix<Double> getComputationTimes() {
        return this.memoryController.getWorkerComputationTimes();
    }

    public Matrix<Double> getTotalCommunicationTimes() {
        Matrix<Double> communicationTimes = new Matrix<>(this.memoryController.getWorkerCommunicationTimes());
        // add time the workers stall
        for (int i = 0; i < this.memoryController.getProcessingElementGridSize(); i++) {
            for (int j = 0; j < this.memoryController.getProcessingElementGridSize(); j++) {
                communicationTimes.set(i, j, communicationTimes.get(i, j) +
                        this.memoryController.getWorkerStallTimes().get(i, j));
            }
        }
        return communicationTimes;
    }

    public Matrix<Double> getStallTimes() {
        return this.memoryController.getWorkerStallTimes();
    }

    public Matrix<Double> getSendTimes() {
        return this.memoryController.getWorkerCommunicationTimes();
    }

    public Matrix<Double> getTotalExecutionTimes() {
        return this.memoryController.getTotalWorkerTimes();
    }
}
