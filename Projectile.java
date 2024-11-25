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

    public Projectile(double startX, double startY, double targetX, double targetY, double speed, double maxDistance) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.active = true;
        this.traveledDistance = 0;
        this.color = Color.RED;
    }

    public void updatePosition() {
        if (!active) return;

        double angle = Math.atan2(targetY - y, targetX - x);

        double deltaX = speed * Math.cos(angle);
        double deltaY = speed * Math.sin(angle);

        x += deltaX;
        y += deltaY;

        traveledDistance += Math.hypot(deltaX, deltaY);

        double distanceToTarget = Math.hypot(targetX - x, targetY - y);
        if (distanceToTarget <= speed || traveledDistance >= maxDistance) {
            active = false;
        }
    }

    public void draw(Graphics g) {
        if (!active) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);

        g2d.setStroke(new BasicStroke(1.0f));

        double angle = Math.atan2(targetY - y, targetX - x);

        int endX = (int) (x + 10 * Math.cos(angle));
        int endY = (int) (y + 10 * Math.sin(angle));

        g2d.drawLine((int) x, (int) y, endX, endY);
    }

    public boolean hasHit(Character target) {
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

    public boolean isActive() {
        return active;
    }

}
