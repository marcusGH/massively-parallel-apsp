package timingAnalysis;

import util.Matrix;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TimingAnalysisResult {

    private final TimedCommunicationManager memoryController;
    private final int problemSize;


    public TimingAnalysisResult(TimedCommunicationManager memoryController, int problemSize) {
        this.memoryController = memoryController;
        this.problemSize = problemSize;
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

    public void saveResult(String filename) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileWriter(filename));

        // write the headers
        printWriter.write("n,computation_time,send_time,stall_time,total_communication_time\n");

        // collect the data we want to save
        List<Double> computation = getComputationTimes().toList();
        List<Double> send = getSendTimes().toList();
        List<Double> stall = getStallTimes().toList();
        List<Double> communication = getTotalCommunicationTimes().toList();

        // then save it with one row for each PE
        for (int i = 0; i < computation.size(); i++) {
            String line = String.format("%d,%f,%f,%f,%f\n", this.problemSize,
                    computation.get(i), send.get(i), stall.get(i), communication.get(i));
            printWriter.write(line);
        }

        printWriter.close();
    }
}
