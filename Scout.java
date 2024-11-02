package classesSeparated;

import javax.swing.Timer;
import java.awt.*;
import static java.awt.geom.Point2D.distance;

public class Scout extends Character {
    private int points = 1000;
    private static final int MAX_POINTS = 1000;
    private static final int MIN_POINTS = 50;
    private long lastPointReductionTime = System.currentTimeMillis();
    private long lastShootTime = 0;
    private static final int POINT_REDUCTION_INTERVAL = 60 * 1000;
    private static final int SHOOT_INTERVAL = 2000;
    private static final int MAX_BULLET_DISTANCE = 200;  // Максимално разстояние за патрона
    private int kills = 0;  // Брояч за убитите работници
    private ScoutGame scoutGame;
    private boolean returningToBase = false;
    private boolean recharging = false;
    private boolean isActive = true;
    private long rechargeStartTime = 0;
    private static final int RECHARGE_DURATION = 10 * 1000;
    private double currentAngle;
    private Worker currentTargetWorker = null;
    private Resource currentTargetResource = null;
    private int resourceIndex = 0;

    public Scout(double startX, double startY, String team, ScoutGame game) {
        super(startX, startY, team, "scout");
        this.scoutGame = game;
        this.currentAngle = Math.random() * 360;
    }

    public void update(Point baseCenter, Resource[] resources) {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();
        managePointsReduction(currentTime);

        if (points <= MIN_POINTS && !returningToBase && !recharging) {
            returningToBase = true;
        }

        if (returningToBase) {
            moveToBase(baseCenter, currentTime, resources);
        } else if (recharging) {
            handleRecharging(currentTime);
        } else {
            Worker targetWorker = scoutGame.findClosestEnemyWorkerWithinRange(this, team, 100);

            if (targetWorker != null) {
                double dx = targetWorker.getX() - x;
                double dy = targetWorker.getY() - y;
                this.currentAngle = Math.toDegrees(Math.atan2(dy, dx));

                double distanceToTarget = distanceTo(targetWorker);
                if (distanceToTarget > 60) {
                    moveDirectlyToAvoidingResources((int) targetWorker.getX(), (int) targetWorker.getY(), resources);
                } else {
                    handleShooting(targetWorker, currentTime);
                }
            } else {
                patrolResources(resources);
            }
        }

        keepWithinBounds(scoutGame.getWidth(), scoutGame.getHeight());
    }

    public double distanceTo(Worker worker) {
        double dx = this.x - worker.getX();
        double dy = this.y - worker.getY();
        return Math.hypot(dx, dy);
    }

    private void patrolResources(Resource[] resources) {
        if (resources.length == 0) {
            moveRandomly();
            return;
        }

        if (currentTargetResource == null || reachedResource(currentTargetResource)) {
            currentTargetResource = resources[resourceIndex];
            resourceIndex = (resourceIndex + 1) % resources.length;
        }

        moveDirectlyToAvoidingResources((int) currentTargetResource.getX(), (int) currentTargetResource.getY(), resources);
    }

    private boolean reachedResource(Resource resource) {
        return distance(this.x, this.y, resource.getX(), resource.getY()) < 10;
    }

    private void moveDirectlyToAvoidingResources(int targetX, int targetY, Resource[] resources) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);

        double scoutRadius = 5;
        double minimumDistance = 30;

        if (distance > 2) {
            double intendedAngle = Math.toDegrees(Math.atan2(dy, dx));
            double adjustedAngle = intendedAngle;
            boolean obstacleDetected = false;

            for (Resource resource : resources) {
                double distanceToResource = distance(resource.getX(), resource.getY(), x, y);
                double requiredAvoidanceDistance = resource.getRadius() + scoutRadius + minimumDistance;

                if (distanceToResource < requiredAvoidanceDistance) {
                    obstacleDetected = true;
                    double angleToResource = Math.toDegrees(Math.atan2(resource.getY() - y, resource.getX() - x));
                    adjustedAngle += (angleToResource > intendedAngle) ? -30 : 30;
                    break;
                }
            }

            if (!obstacleDetected) {
                adjustedAngle = intendedAngle;
            }

            this.currentAngle = adjustedAngle;
            x += Math.cos(Math.toRadians(currentAngle));
            y += Math.sin(Math.toRadians(currentAngle));
        }
    }

    private void moveToBase(Point baseCenter, long currentTime, Resource[] resources) {
        moveDirectlyToAvoidingResources((int) baseCenter.getX(), (int) baseCenter.getY(), resources);
        if (distance(baseCenter.getX(), baseCenter.getY(), x, y) < 5) {
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

    public int getKills() {
        return kills;
    }

    public void incrementKills() {
        kills++;
    }

    private void handleShooting(Worker targetWorker, long currentTime) {
        if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
            int startX = (int) this.x;
            int startY = (int) this.y;
            final int[] bulletX = {(int) (startX + Math.cos(Math.toRadians(this.currentAngle)) * this.getBodyRadius() * 2)};
            final int[] bulletY = {(int) (startY + Math.sin(Math.toRadians(this.currentAngle)) * this.getBodyRadius() * 2)};

            int targetX = (int) targetWorker.getX();
            int targetY = (int) targetWorker.getY();

            double angleToTarget = Math.atan2(targetY - bulletY[0], targetX - bulletX[0]);
            final double[] travelledDistance = {0};

            Timer timer = new Timer(50, e -> {
                int currentEndX = (int) (bulletX[0] + Math.cos(angleToTarget) * 10);
                int currentEndY = (int) (bulletY[0] + Math.sin(angleToTarget) * 10);

                scoutGame.drawShot(bulletX[0], bulletY[0], currentEndX, currentEndY);

                bulletX[0] += Math.cos(angleToTarget) * 5;
                bulletY[0] += Math.sin(angleToTarget) * 5;
                travelledDistance[0] += 5;

                // Проверка за попадение в работника
                if (distance(bulletX[0], bulletY[0], targetX, targetY) <= targetWorker.getBodyRadius()) {
                    targetWorker.takeDamage(1);

                    // Проверка дали здравето на работника е намаляло до 0
                    if (targetWorker.getHealth() <= 0) {
                        incrementKills();  // Увеличаване на броя на убийствата само ако работникът е елиминиран
                    }

                    ((Timer) e.getSource()).stop();
                }

                // Проверка за максимално разстояние на патрона
                if (travelledDistance[0] >= MAX_BULLET_DISTANCE) {
                    ((Timer) e.getSource()).stop();
                }
            });
            timer.setRepeats(true);
            timer.start();

            lastShootTime = currentTime;
        }
    }


    public void drawKills(Graphics2D g2d, int xPosition, int yPosition) {
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(team.equals("blue") ? Color.RED : Color.BLUE);

        // Форматиране на текст с "точки - убийства"
        String scoreText = points + " - " + kills;

        // Изчертаване на текста на съответната позиция
        g2d.drawString(scoreText, xPosition, yPosition);
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

    public int getBodyRadius() {
        return 5;
    }

    private void moveRandomly() {
        double angleChange = (Math.random() - 0.5) * 20;
        currentAngle += angleChange;

        if (currentAngle < 0) currentAngle += 360;
        if (currentAngle >= 360) currentAngle -= 360;

        double speed = 1;
        x += speed * Math.cos(Math.toRadians(currentAngle));
        y += speed * Math.sin(Math.toRadians(currentAngle));
    }
}
