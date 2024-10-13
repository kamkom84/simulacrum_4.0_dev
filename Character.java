package classesSeparated;
public abstract class Character {
    protected int x, y;
    protected double angle = 0;
    protected String team;
    protected String role;

    public Character(int startX, int startY, String team, String role) {
        this.x = startX;
        this.y = startY;
        this.team = team;
        this.role = role.toLowerCase();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public double distanceToAnt(Character otherAnt) {
        if (otherAnt == null) {
            // Връща стойност (например -1), за да означи, че няма валиден обект за сравнение
            return -1;
        }

        double dx = this.x - otherAnt.getX();
        double dy = this.y - otherAnt.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

}
