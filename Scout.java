package classesSeparated;

import java.awt.*;
import static java.awt.geom.Point2D.distance;

public class Scout extends Character {
    private int points = 2000;
    private static final int MAX_POINTS = 2000;
    private static final int MIN_POINTS = 50;
    private long lastPointReductionTime = System.currentTimeMillis();
    private long lastShootTime = 0;
    private static final int POINT_REDUCTION_INTERVAL = 60 * 1000; // 1 minute
    private static final int SHOOT_INTERVAL = 2000; // 2 seconds
    private ScoutGame scoutGame;
    private boolean returningToBase = false;
    private boolean recharging = false;
    private boolean isActive = false;
    private long startTime; // Време на активация на скаута

    public Scout(int startX, int startY, String team, ScoutGame game) {
        super(startX, startY, team, "scout");
        this.scoutGame = game;
    }

    public void draw(Graphics g) {
        // Draw the scout as a circle
        g.setColor(team.equals("blue") ? Color.BLUE : Color.RED);
        g.fillOval((int)x - 5, (int)y - 5, 10, 10);

        // Draw the points above the scout
        g.setColor(Color.WHITE);
        g.drawString("Points: " + points, (int)x - 10, (int)y - 10);
    }

    public void update(Point baseCenter) {
        long currentTime = System.currentTimeMillis();

        // Reduce points by 1 every minute
        if (currentTime - lastPointReductionTime >= POINT_REDUCTION_INTERVAL) {
            points = Math.max(points - 1, 0);
            lastPointReductionTime = currentTime;
        }

        // Return to base if points fall below 50
        if (points < MIN_POINTS && !returningToBase) {
            returningToBase = true;
        }

        // Move towards the base if returning for recharge
        if (returningToBase) {
            moveTo(baseCenter.x, baseCenter.y, 1); // Slow movement
            if (distance(baseCenter.x, baseCenter.y, this.x, this.y) < 5) {
                recharging = true;
                returningToBase = false;
                scoutGame.addPointsToScoutBase(team, points); // Take points from base if needed
                points = MAX_POINTS;
            }
        }

        // If not returning, move randomly and attack workers
        if (!returningToBase && !recharging) {
            moveRandomly(); // Slow movement
            if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
                shootEnemyWorker();
                lastShootTime = currentTime;
            }
        }
    }

    private void moveTo(int targetX, int targetY, int speed) {
        double dx = targetX - this.x;
        double dy = targetY - this.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 0) {
            x += speed * dx / distance;
            y += speed * dy / distance;
        }
    }

    private void moveRandomly() {
        double angle = Math.toRadians(Math.random() * 360);
        x += Math.cos(angle);
        y += Math.sin(angle);
    }

    private void shootEnemyWorker() {
        Worker targetWorker = scoutGame.findClosestEnemyWorker(this, team);
        if (targetWorker != null) {
            targetWorker.takeDamage(1);
            animateShot(targetWorker.getX(), targetWorker.getY());
        }
    }

    private void animateShot(int targetX, int targetY) {
        // Draw a line between the scout and the target (placeholder for actual animation)
        Graphics g = scoutGame.getGraphics();
        g.setColor(Color.YELLOW);
        g.drawLine((int)x, (int)y, targetX, targetY);
    }

    public double distanceTo(Character other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void activate() {
        this.isActive = true;
        this.startTime = System.currentTimeMillis(); // Започва времето за активация на скаута
    }

    public boolean isActive() {
        return isActive;
    }


}
