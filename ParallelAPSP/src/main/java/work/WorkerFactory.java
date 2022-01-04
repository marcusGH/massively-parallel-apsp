package work;

import memoryModel.MemoryController;
import memoryModel.PrivateMemory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CyclicBarrier;

public class WorkerFactory {

    protected MemoryController memoryController;
    protected CyclicBarrier cyclicBarrier;
    protected Runnable runExceptionHandler;
    private final Constructor<?> workerConstructor;

    private static final Class[] workerConstructorParameterTypes = {
        Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, PrivateMemory.class,
        MemoryController.class, CyclicBarrier.class, Runnable.class
    };

    WorkerFactory(Class<? extends Worker> workerClass) throws WorkerInstantiationException {
        try {
            this.workerConstructor = workerClass.getConstructor(WorkerFactory.workerConstructorParameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new WorkerInstantiationException("Could not find appropriate constructor in provided Worker class.");
        }
    }

    void init(MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        this.memoryController = memoryController;
        this.cyclicBarrier = cyclicBarrier;
        this.runExceptionHandler = runExceptionHandler;
    }


    public Worker createWorker(int i, int j, int p, int numPhases, PrivateMemory privateMemory) throws WorkerInstantiationException {
        try {
            return (Worker) this.workerConstructor.newInstance(i, j, p, numPhases, privateMemory, this.memoryController, this.cyclicBarrier, this.runExceptionHandler);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new WorkerInstantiationException(String.format("Failed to instantiate Worker(%d, %d).", i, j));
        }
    }
}
