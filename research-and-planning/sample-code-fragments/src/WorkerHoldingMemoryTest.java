public class WorkerHoldingMemoryTest {
    private int a = 5;

    public void set(int x) {
        this.a = x;
    }

    public int get() {
        return this.a;
    }

    public WorkerHoldingMemoryTest(int a) {
        this.a = a;
    }

    public static void main(String[] args) {

        WorkerHoldingMemoryTest mem = new WorkerHoldingMemoryTest(10);

        Thread t = new Thread(() -> {
            System.out.println(mem.get());
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(mem.get());
        });

        t.start();
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mem.set(42);
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mem.set(2 ) ;
    }
}
