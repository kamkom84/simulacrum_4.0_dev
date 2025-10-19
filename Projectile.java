package classesSeparated;

import java.awt.*;

public class Projectile {
    private double x, y;
    private final double targetX, targetY;
    private final double speed;
    private boolean active;
    private final double maxDistance;
    private double traveledDistance;
    private final Color color;
    private double directionAngle;

    public Projectile(double startX, double startY, double targetX, double targetY,
                      double speed, double maxDistance) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.active = true;
        this.traveledDistance = 0;
        this.color = Color.GREEN;
        this.directionAngle = Math.atan2(targetY - startY, targetX - startX);
    }

    public void updateProjectilePosition() {
        if (!active) return;

        double deltaX = speed * Math.cos(directionAngle);
        double deltaY = speed * Math.sin(directionAngle);

        x += deltaX;
        y += deltaY;

        traveledDistance += Math.hypot(deltaX, deltaY);

        double distanceToTarget = Math.hypot(targetX - x, targetY - y);
        if (distanceToTarget <= 5) {
            active = false;
            return;
        }

        if (traveledDistance >= maxDistance) {
            active = false;
        }
    }

    public void drawProjectile(Graphics g) {
        if (!active) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawLine(
                (int) x, (int) y,
                (int) (x + 5 * Math.cos(directionAngle)),
                (int) (y + 5 * Math.sin(directionAngle))
        );
    }

    public boolean hasProjectileHit(Character target) {
        double distanceToTarget = Math.hypot(x - target.getX(), y - target.getY());
        if (distanceToTarget <= target.getBodyRadius()) {
            if (target instanceof Scout) {
                Scout scout = (Scout) target;
                scout.decreasePoints(1);
                scout.reverseDirection();
            }
            return true;
        }
        return false;
    }

    public boolean isProjectileActive() { return active; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getDirectionAngle() { return directionAngle; }
    public void deactivate() { active = false; }
}
