package classesSeparated;

import java.awt.*;
import java.util.Random;

import static java.awt.geom.Point2D.distance;

public class Worker extends Character {
    private boolean isActive = false;
    private boolean hasStarted = false; // Ново поле
    private boolean hasResource = false;
    private Point targetResource = null;
    private int targetResourceIndex = -1;
    private boolean returningToBase = false;
    private boolean waitingOutsideBase = false;
    private static final int RESOURCE_POINTS = 5;
    private long resourceAcquisitionTime = 0;
    private long baseStayStartTime = 0;
    private boolean waitingInBase = false;
    private Random random = new Random();
    private int[] resourceValues;
    private static boolean[] resourceOccupied; // Споделен масив
    private int baseWidth;
    private int baseHeight;
    private ScoutGame scoutGame;
    private Point[] resources;
    private int health = 100; // Здраве на работника
    private int workerId; // Номер на работника

    public Worker(int startX, int startY, String team, Point[] resources, int[] resourceValues,
                  boolean[] resourceOccupied, int baseWidth, int baseHeight, ScoutGame game, int workerId) {
        super(startX, startY, team, "worker");
        this.scoutGame = game;
        this.resourceValues = resourceValues;
        Worker.resourceOccupied = resourceOccupied; // Инициализиране на споделения масив
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.resources = resources;
        this.workerId = workerId;
        this.isActive = false;
    }

    public void draw(Graphics g) {
        // Рисува работника (примерно като кръг)
        g.setColor(Color.BLACK);
        g.fillOval((int)x - 5, (int)y - 5, 10, 10);

        // Показва номера на работника в центъра му
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(workerId), (int)x - 3, (int)y + 3);
    }

    public int getWorkerId() {
        return workerId;
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health <= 0) {
            System.out.println("Работникът на " + team + " е елиминиран!");
            deactivateWorker();
        }
    }

    public void deactivateWorker() {
        this.isActive = false;
        this.x = -1000;
        this.y = -1000;
        System.out.println("Работникът на " + team + " е деактивиран!");
    }

    public void activate() {
        this.isActive = true;
        this.hasStarted = true; // Задаваме, че работникът е стартирал
        this.hasResource = false;
        this.returningToBase = false;
        this.targetResource = null;
        this.waitingOutsideBase = false;
        System.out.println("Worker " + workerId + " activated. isActive set to: " + isActive);
    }

    public void updateWorkerCycle(Point[] resources, int baseX, int baseY, Scout enemyScout) {
        if (!isActive) return;

        if (waitingOutsideBase) {
            // Работникът чака извън базата
            return;
        }

        if (returningToBase) {
            returnToBase(baseX, baseY);
        } else if (hasResource) {
            gatherResource();
        } else {
            if (targetResource == null || resourceValues[targetResourceIndex] < 5) {
                // Освобождаваме ресурса, ако вече не ни трябва
                if (targetResourceIndex >= 0) {
                    resourceOccupied[targetResourceIndex] = false;
                }
                targetResource = findNearestAvailableResource(resources);
                if (targetResource == null) {
                    // Няма свободни ресурси с 5 или повече точки
                    moveOutsideBase(baseX, baseY);
                    return;
                } else {
                    // Отбелязваме ресурса като зает
                    resourceOccupied[targetResourceIndex] = true;
                }
            }

            if (targetResource != null) {
                moveToResource();
            }
        }
    }

    private Point findNearestAvailableResource(Point[] resources) {
        Point nearest = null;
        double minDistance = Double.MAX_VALUE;
        int closestResourceIndex = -1;

        for (int i = 0; i < resources.length; i++) {
            if (!resourceOccupied[i] && resourceValues[i] >= 5) {
                double distance = distance(resources[i].x, resources[i].y, this.x, this.y);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = resources[i];
                    closestResourceIndex = i;
                }
            }
        }

        if (closestResourceIndex != -1) {
            targetResourceIndex = closestResourceIndex;
        }

        return nearest;
    }

    private void moveToResource() {
        if (targetResource == null) return;

        int resourceRadius = 20;
        int workerRadius = 6;
        double moveSpeed = 5.0;

        double distance = distance(targetResource.x, targetResource.y, x, y);

        if (distance > (resourceRadius + workerRadius)) {
            double moveX = ((targetResource.x - x) / distance) * moveSpeed;
            double moveY = ((targetResource.y - y) / distance) * moveSpeed;

            // Добавяне на случайно отклонение
            double randomFactor = 0.5;
            moveX += (random.nextDouble() - 0.5) * randomFactor;
            moveY += (random.nextDouble() - 0.5) * randomFactor;

            // Актуализиране на координатите на работника
            x += moveX;
            y += moveY;
            angle = Math.toDegrees(Math.atan2(targetResource.y - y, targetResource.x - x));
        } else {
            hasResource = true;
            resourceAcquisitionTime = System.currentTimeMillis();
            System.out.println("Worker " + workerId + " reached the resource and started gathering.");
        }
    }

    private void gatherResource() {
        long currentTime = System.currentTimeMillis();
        // Предполагаме, че събирането на ресурс отнема 60 секунди
        if (currentTime - resourceAcquisitionTime >= 60000) {
            int resourceIndex = targetResourceIndex;

            if (resourceIndex >= 0 && resourceValues[resourceIndex] >= 5) {
                resourceValues[resourceIndex] -= RESOURCE_POINTS;
                hasResource = false;
                returningToBase = true;
                System.out.println("Worker " + workerId + " gathered resources and is returning to base.");
                if (resourceValues[resourceIndex] < 5) {
                    System.out.println("Resource " + resourceIndex + " is depleted or below minimum value.");
                    resourceOccupied[resourceIndex] = false;
                }
            } else {
                // Ресурсът е изчерпан по време на събирането
                hasResource = false;
                resourceOccupied[resourceIndex] = false;
                targetResource = null;
                targetResourceIndex = -1;
                System.out.println("Resource " + resourceIndex + " became unavailable during gathering.");
            }
        } else {
            System.out.println("Worker " + workerId + " is still gathering resources. Time elapsed: " + (currentTime - resourceAcquisitionTime));
        }
    }

    private void returnToBase(int baseX, int baseY) {
        int targetX = baseX + baseWidth / 2;
        int targetY = baseY + baseHeight / 2;
        double distance = distance(targetX, targetY, x, y);

        if (distance > 5) {
            double moveSpeed = 4.0;
            x += (targetX - x) / distance * moveSpeed;
            y += (targetY - y) / distance * moveSpeed;
            angle = Math.toDegrees(Math.atan2(targetY - y, targetX - x));
        } else {
            long currentTime = System.currentTimeMillis();
            if (!waitingInBase) {
                waitingInBase = true;
                baseStayStartTime = System.currentTimeMillis();
                System.out.println("Worker " + workerId + " reached base. Waiting to unload.");
            }

            if (currentTime - baseStayStartTime >= 60000) {
                addPointsToBase();
                prepareForNextCycle();
                System.out.println("Worker " + workerId + " completed unload. Ready to start next cycle.");
            }
        }
    }

    private void moveOutsideBase(int baseX, int baseY) {
        // Определяне на позиция извън базата, където работникът ще изчака
        int waitX = baseX + baseWidth + 50 + random.nextInt(100);
        int waitY = baseY + baseHeight + 50 + random.nextInt(100);

        double distance = distance(waitX, waitY, x, y);

        if (distance > 5) {
            double moveSpeed = 4.0;
            x += (waitX - x) / distance * moveSpeed;
            y += (waitY - y) / distance * moveSpeed;
            angle = Math.toDegrees(Math.atan2(waitY - y, waitX - x));
        } else {
            waitingOutsideBase = true;
            System.out.println("Worker " + workerId + " is waiting outside the base.");
        }
    }

    private void prepareForNextCycle() {
        returningToBase = false;
        waitingInBase = false;
        hasResource = false;
        targetResource = null;
        waitingOutsideBase = false; // Добавено, за да може работникът да търси нов ресурс
        // Освобождаваме ресурса
        if (targetResourceIndex >= 0) {
            resourceOccupied[targetResourceIndex] = false;
        }
        targetResourceIndex = -1;
        // Не задаваме isActive = false;
    }

    private void addPointsToBase() {
        int pointsToAdd = 5;
        scoutGame.addPointsToScoutBase(team, pointsToAdd);
    }

    public String getTeam() {
        return this.team;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean hasStarted() {
        return hasStarted;
    }
}
