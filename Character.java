package classesSeparated;

public abstract class Character {
    protected double x;
    protected double y;
    protected double angle = 0;
    protected String team;
    protected String role;
    protected double currentAngle;

    public Character(double startX, double startY, String team, String role) {
        this.x = startX;
        this.y = startY;
        this.team = team;
        this.role = role.toLowerCase();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }


    public double getAngle() {
        return angle;
    }

    public double distanceToAnt(Character otherAnt) {
        if (otherAnt == null) {
            return -1;
        }

        double dx = this.x - otherAnt.getX();
        double dy = this.y - otherAnt.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }


    public double getCurrentAngle() {
        return currentAngle;
    }

    public void setCurrentAngle(double angle) {
        this.currentAngle = angle;
    }
}
