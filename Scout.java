package classesSeparated;

import java.awt.*;
import static java.awt.geom.Point2D.distance;

public class Scout extends Character {
    private int points = 2000;
    private static final int MAX_POINTS = 2000;
    private static final int MIN_POINTS = 50;
    private long lastPointReductionTime = System.currentTimeMillis();
    private long lastShootTime = 0;
    private static final int POINT_REDUCTION_INTERVAL = 60 * 1000;
    private static final int SHOOT_INTERVAL = 2000;
    private ScoutGame scoutGame;
    private boolean returningToBase = false;
    private boolean recharging = false;
    private boolean isActive = false;
    private long startTime;
    private long rechargeStartTime = 0;
    private static final int RECHARGE_DURATION = 10 * 1000;
    private double currentAngle;
    private Worker currentTargetWorker = null;

    public Scout(int startX, int startY, String team, ScoutGame game) {
        super(startX, startY, team, "scout");
        this.scoutGame = game;
        this.currentAngle = Math.random() * 360;
    }

    public void update(Point baseCenter, Resource[] resources) {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();
        managePointsReduction(currentTime);

        if (returningToBase) {
            moveToBase(baseCenter, currentTime);
        } else if (recharging) {
            handleRecharging(currentTime);
        } else {
            Worker targetWorker = scoutGame.findClosestEnemyWorkerWithinRange(this, team, 100);
            if (targetWorker != null && distanceTo(targetWorker) > 30) {
                moveDirectlyTo(targetWorker.getX(), targetWorker.getY());
            } else if (targetWorker != null) {
                handleShooting(targetWorker, currentTime);
            } else {
                moveRandomlyAvoidingResources(resources);
            }
        }

        keepWithinBounds(scoutGame.getWidth(), scoutGame.getHeight());
    }

    // В класа Scout
    public double distanceTo(Worker worker) {
        double dx = this.x - worker.getX();
        double dy = this.y - worker.getY();
        return Math.hypot(dx, dy);
    }


    private void moveToBase(Point baseCenter, long currentTime) {
        moveDirectlyTo(baseCenter.x, baseCenter.y);
        if (distance(baseCenter.x, baseCenter.y, x, y) < 5) {
            recharging = true;
            returningToBase = false;
            rechargeStartTime = currentTime;
            scoutGame.addPointsToScoutBase(team, points);
            points = MAX_POINTS;
        }
    }

    private void handleRecharging(long currentTime) {
        if (currentTime - rechargeStartTime >= RECHARGE_DURATION) {
            recharging = false;
        }
    }

    private void managePointsReduction(long currentTime) {
        if (currentTime - lastPointReductionTime >= POINT_REDUCTION_INTERVAL) {
            points = Math.max(points - 1, 0);
            lastPointReductionTime = currentTime;
        }
    }

    private void moveDirectlyTo(int targetX, int targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);

        if (distance > 0) {
            dx /= distance;
            dy /= distance;
            x += dx;
            y += dy;
        }
    }

    private void moveRandomlyAvoidingResources(Resource[] resources) {
        double angleChange = (Math.random() - 0.5) * 20;
        currentAngle += angleChange;

        if (currentAngle < 0) currentAngle += 360;
        if (currentAngle >= 360) currentAngle -= 360;

        double speed = 1;
        boolean obstacleDetected;
        int attempts = 0;

        do {
            obstacleDetected = false;
            for (Resource resource : resources) {
                if (isNearFutureResource(resource)) {
                    obstacleDetected = true;
                    currentAngle += 15;
                    if (currentAngle >= 360) currentAngle -= 360;
                    attempts++;
                    break;
                }
            }
        } while (obstacleDetected && attempts < 24);

        x += speed * Math.cos(Math.toRadians(currentAngle));
        y += speed * Math.sin(Math.toRadians(currentAngle));
    }

    private boolean isNearFutureResource(Resource resource) {
        double futureX = this.x + Math.cos(Math.toRadians(currentAngle)) * 10;
        double futureY = this.y + Math.sin(Math.toRadians(currentAngle)) * 10;
        double minDistance = 20;

        double resourceDx = futureX - resource.getX();
        double resourceDy = futureY - resource.getY();

        return Math.hypot(resourceDx, resourceDy) < minDistance;
    }

    private void handleShooting(Worker targetWorker, long currentTime) {
        if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
            targetWorker.takeDamage(1);
            lastShootTime = currentTime;
        }
    }

    private void keepWithinBounds(int panelWidth, int panelHeight) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > panelWidth - 10) x = panelWidth - 10;
        if (y > panelHeight - 10) y = panelHeight - 10;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public int getPoints() {
        return points;
    }

    public double getCurrentAngle() {
        return currentAngle;
    }

}
