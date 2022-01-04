package util;

public class Triple<T, U, V> {
    private final T first;
    private final U second;
    private final V third;

    public Triple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    public V getThird() {
        return third;
    }

    public T x() {
        return first;
    }

    public U y() {
        return second;
    }

    public V z() {
        return third;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", first.toString(), second.toString(), third.toString());
    }
}
