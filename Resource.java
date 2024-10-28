package classesSeparated;

public class Resource {
    private double x;
    private double y;
    private int value;

    public Resource(double x, double y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getValue() {
        return value;
    }

    public void reducePoints(int amount) {
        value = Math.max(0, value - amount); // Уверяваме се, че стойността не пада под 0
    }
}

