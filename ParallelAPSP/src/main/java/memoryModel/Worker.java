package memoryModel;

public abstract class Worker<T> implements Runnable {
    int i;
    int j;
    int n;
    PrivateMemory<T> privateMemory;

    public Worker(int i, int j, int n, PrivateMemory<T> privateMemory) {
        this.i = i;
        this.j = j;
        this.n = n;
        this.privateMemory = privateMemory;
    }

    abstract void computation(int l);

    abstract void communicationBefore(int l);

    abstract void communicationAfter();

    private void send(int i, int j, T value) {

    }

    private void receive(int mi, int mj, String label) {

    }

    private void broadcastRow(T value) {

    }

    private void broadcastCol(T value) {

    }

    private void receiveRowBroadcast(int mi, int mj, String label) {

    }

    private void receiveColBroadcast(int mi, int mj, String label) {

    }

    @Override
    public void run() {

    }

    public PrivateMemory<T> getPrivateMemory() {
        return privateMemory;
    }
}
