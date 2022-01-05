package work;

import memoryModel.MemoryController;
import memoryModel.PrivateMemory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerFactory {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private MemoryController memoryController;
    private CyclicBarrier cyclicBarrier;
    private Runnable runExceptionHandler;

    private final Constructor<?> workerConstructor;

    // The list of arguments to the abstract Worker class constructor
    private static final Class[] workerConstructorParameterTypes = {
        Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, PrivateMemory.class,
        MemoryController.class, CyclicBarrier.class, Runnable.class
    };

    /**
     * Creates a WorkerFactory that can produce object of subtypes of Workers on demand.
     * The subtype class provided must have a constructor taking the same arguments as the Worker
     * constructor. Otherwise, an exception is thrown.
     *
     * @param workerClass a Class object of a subtype of Worker. The subtype MUST HAVE A PUBLIC CONSTRUCTOR.
     * @throws WorkerInstantiationException if the subclass provided does not have the appropriate constructor
     */
    public WorkerFactory(Class<? extends Worker> workerClass) throws WorkerInstantiationException {
        try {
            if (workerClass.getConstructors().length == 0) {
                LOGGER.log(Level.SEVERE, "No constructors were found from reflection of the worker subclass {0}." +
                        "Try to make its constructor public.", workerClass.getCanonicalName());
            }
            this.workerConstructor = workerClass.getConstructor(WorkerFactory.workerConstructorParameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new WorkerInstantiationException("Could not find appropriate constructor in provided Worker class.");
        }
    }

    /**
     * Initialises the fields of the objects that are shared by all workers. This method should only be called once
     * and must be executed before {@link #createWorker} is called.
     *
     * @param memoryController the memory controller
     * @param cyclicBarrier the cyclic barrier used for synchronisation
     * @param runExceptionHandler the runnable to be executed on worker execution error
     */
    void init(MemoryController memoryController, CyclicBarrier cyclicBarrier, Runnable runExceptionHandler) {
        if (null == memoryController || null == cyclicBarrier || null == runExceptionHandler) {
            throw new IllegalArgumentException("All the provided references to init must be non-null");
        } else if (null != this.memoryController || null != this.cyclicBarrier || null != this.runExceptionHandler) {
            throw new IllegalStateException("The init method should only be invoked once");
        } else {
            this.memoryController = memoryController;
            this.cyclicBarrier = cyclicBarrier;
            this.runExceptionHandler = runExceptionHandler;
        }
    }

    /**
     * Creates and returns Worker(i, j)
     *
     * @param i row ID
     * @param j column ID
     * @param p number of processing elements in each row and column
     * @param numPhases number of computation phases for each worker
     * @param privateMemory a reference to the worker's private memory
     * @return a new worker
     * @throws WorkerInstantiationException if the constructor fails
     */
    Worker createWorker(int i, int j, int p, int numPhases, PrivateMemory privateMemory) throws WorkerInstantiationException {
        try {
            return (Worker) this.workerConstructor.newInstance(i, j, p, numPhases, privateMemory, this.memoryController, this.cyclicBarrier, this.runExceptionHandler);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new WorkerInstantiationException(String.format("Failed to instantiate Worker(%d, %d).", i, j));
        }
    }
}
