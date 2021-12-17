package graphReader;

class Triple<X, Y, Z> {
    public final X x;
    public final Y y;
    public final Z z;

    public Triple(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return x.toString() + " " +
                y.toString() + " " +
                z.toString();
    }
}
