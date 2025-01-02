package lava;

public class Triple<X, Y, Z> {
    public X first;
    public Y second;
    public Z third;

    public Triple(X first, Y second, Z third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        result = prime * result + ((third == null) ? 0 : third.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + "," + third + ")";
    }
}