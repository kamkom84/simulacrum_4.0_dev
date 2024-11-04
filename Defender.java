package classesSeparated;

public class Defender extends Character {

    private double speed = 0.01;
    private double angleOffset;

    public Defender(int startX, int startY, String team, String role, double initialAngle) {
        super(startX, startY, team, role);
        this.angleOffset = initialAngle;
    }

    public void patrolAroundBase(int baseCenterX, int baseCenterY, int shieldRadius) {
        angleOffset += speed;
        if (angleOffset >= 2 * Math.PI) {
            angleOffset -= 2 * Math.PI;
        } else if (angleOffset < 0) {
            angleOffset += 2 * Math.PI;
        }
        this.x = baseCenterX + (int) (shieldRadius * Math.cos(angleOffset)) - 5;
        this.y = baseCenterY + (int) (shieldRadius * Math.sin(angleOffset)) - 5;
        this.angle = Math.toDegrees(angleOffset);
    }

}

