package work;

import memoryModel.MemoryController;
import memoryModel.PrivateMemory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerFactory {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private MemoryController memoryController;
    private final Constructor<?> workerConstructor;

    /**
     * Creates a WorkerFactory that can produce object of subtypes of Workers on demand.
     * The subtype class provided must have a constructor taking the same arguments as the Worker
     * constructor. Otherwise, an exception is thrown.
     *
     * @param workerClass a Class object of a subtype of Worker. The subtype MUST HAVE A PUBLIC CONSTRUCTOR.
     * @throws WorkerInstantiationException if the subclass provided does not have the appropriate constructor
     */
    public WorkerFactory(Class<? extends Algorithm> workerClass) throws WorkerInstantiationException {
        try {
            if (workerClass.getConstructors().length == 0) {
                LOGGER.log(Level.SEVERE, "No constructors were found from reflection of the worker subclass {0}." +
                        "Try to make its constructor public.", workerClass.getCanonicalName());
            }
            this.workerConstructor = workerClass.getConstructor(Algorithm.workerConstructorParameterTypes);
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
     */
    void init(MemoryController memoryController) {
        if (null == memoryController) {
            throw new IllegalArgumentException("A memory controller reference must be provided");
        } else {
            this.memoryController = memoryController;
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
    Algorithm createWorker(int i, int j, int p, int n, int numPhases, PrivateMemory privateMemory) throws WorkerInstantiationException {
        try {
            return (Algorithm) this.workerConstructor.newInstance(i, j, p, n, numPhases, privateMemory, this.memoryController);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new WorkerInstantiationException(String.format("Failed to instantiate Worker(%d, %d).", i, j));
        }
    }
}
