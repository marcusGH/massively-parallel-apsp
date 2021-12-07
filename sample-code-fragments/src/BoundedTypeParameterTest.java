import java.util.List;

public class BoundedTypeParameterTest<E, T extends List<E>> {
    T t;
    E e;

    void test() {
        this.e = t.get(0);
    }
}
