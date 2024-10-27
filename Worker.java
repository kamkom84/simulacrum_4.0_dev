package classesSeparated;

import java.awt.*;
import java.util.Random;

import static java.awt.geom.Point2D.distance;

public class Worker extends Character {
    private boolean isActive = false;
    private boolean hasStarted = false;
    private boolean hasResource = false;
    private Point targetResource = null;
    private int targetResourceIndex = -1;
    private boolean returningToBase = false;
    private boolean waitingOutsideBase = false;
    private boolean waitingInBase = false;
    private long resourceAcquisitionTime = 0;
    private long baseStayStartTime = 0;
    private Point startPosition; // Начална позиция
    private Random random = new Random();
    private int[] resourceValues;
    private static boolean[] resourceOccupied;
    private int baseWidth;
    private int baseHeight;
    private ScoutGame scoutGame;
    private Point[] resources;
    private int workerId;
    private static final int RESOURCE_POINTS = 5;
    private int health = 100; // Начално здраве на работника

    public Worker(int startX, int startY, String team, Point[] resources, int[] resourceValues,
                  boolean[] resourceOccupied, int baseWidth, int baseHeight, ScoutGame game, int workerId) {
        super(startX, startY, team, "worker");
        this.scoutGame = game;
        this.resourceValues = resourceValues;
        Worker.resourceOccupied = resourceOccupied;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.resources = resources;
        this.workerId = workerId;
        this.startPosition = new Point(startX, startY); // Запазваме началната позиция
    }

    public void activate() {
        this.isActive = true;
        this.hasStarted = true;
        this.hasResource = false;
        this.returningToBase = false;
        this.targetResource = null;
        this.waitingOutsideBase = false;
    }

    public void updateWorkerCycle(Point[] resources, int baseX, int baseY, Scout enemyScout) {
        if (!isActive) return;

        if (waitingOutsideBase) {
            return;
        }

        if (returningToBase) {
            returnToBase(baseX, baseY);
        } else if (hasResource) {
            gatherResource();
        } else {
            if (targetResource == null || resourceValues[targetResourceIndex] < RESOURCE_POINTS) {
                if (targetResourceIndex >= 0) {
                    resourceOccupied[targetResourceIndex] = false;
                }
                targetResource = findNearestAvailableResource(resources);
                if (targetResource == null) {
                    moveOutsideBase(baseX, baseY);
                    return;
                } else {
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
            if (!resourceOccupied[i] && resourceValues[i] >= RESOURCE_POINTS) {
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

        double distance = distance(targetResource.x, targetResource.y, x, y);
        double moveSpeed = 5.0;
        int resourceRadius = 20;
        int workerRadius = 6;

        if (distance > (resourceRadius + workerRadius)) {
            double moveX = ((targetResource.x - x) / distance) * moveSpeed;
            double moveY = ((targetResource.y - y) / distance) * moveSpeed;
            x += moveX;
            y += moveY;
            angle = Math.toDegrees(Math.atan2(targetResource.y - y, targetResource.x - x));
        } else {
            hasResource = true;
            resourceAcquisitionTime = System.currentTimeMillis();
        }
    }

    private void gatherResource() {
        long currentTime = System.currentTimeMillis();

        // Уверяваме се, че работникът стои 1 минута преди да събере ресурса
        if (currentTime - resourceAcquisitionTime >= 60000) { // 60000 ms = 1 минута
            int resourceIndex = targetResourceIndex;

            // Проверка дали ресурсът все още има достатъчно точки
            if (resourceIndex >= 0 && resourceValues[resourceIndex] >= RESOURCE_POINTS) {
                resourceValues[resourceIndex] -= RESOURCE_POINTS; // Намаляване на точките на ресурса
                hasResource = false;
                returningToBase = true;
                System.out.println("Worker " + workerId + " събра ресурс и се връща в базата.");

                // Проверка дали ресурсът е изчерпан
                if (resourceValues[resourceIndex] < 5) {
                    System.out.println("Resource " + resourceIndex + " is depleted or below minimum value.");
                    resourceOccupied[resourceIndex] = false; // Освобождаване на ресурса
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
        double moveSpeed = 4.0;

        if (distance > 5) {
            x += (targetX - x) / distance * moveSpeed;
            y += (targetY - y) / distance * moveSpeed;
            angle = Math.toDegrees(Math.atan2(targetY - y, targetX - x));
        } else {
            long currentTime = System.currentTimeMillis();
            if (!waitingInBase) {
                waitingInBase = true;
                baseStayStartTime = System.currentTimeMillis();
            }

            if (currentTime - baseStayStartTime >= 60000) {
                addPointsToBase();
                prepareForNextCycle();
            }
        }
    }

    private void moveOutsideBase(int baseX, int baseY) {
        int waitX = startPosition.x;
        int waitY = startPosition.y;
        double distance = distance(waitX, waitY, x, y);
        double moveSpeed = 4.0;

        if (distance > 5) {
            x += (waitX - x) / distance * moveSpeed;
            y += (waitY - y) / distance * moveSpeed;
            angle = Math.toDegrees(Math.atan2(waitY - y, waitX - x));
        } else {
            waitingOutsideBase = true;
        }
    }

    private void prepareForNextCycle() {
        returningToBase = false;
        waitingInBase = false;
        hasResource = false;
        targetResource = null;
        waitingOutsideBase = false;
        if (targetResourceIndex >= 0) {
            resourceOccupied[targetResourceIndex] = false;
        }
        targetResourceIndex = -1;
    }

    private void addPointsToBase() {
        int pointsToAdd = RESOURCE_POINTS;
        scoutGame.addPointsToScoutBase(team, pointsToAdd);
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health <= 0) {
            // Работникът е елиминиран, обработка при елиминация
            deactivateWorker();
        }
    }

    public void deactivateWorker() {
        this.isActive = false;
        this.x = -1000; // Преместване на работника извън екрана или зоната на играта
        this.y = -1000;
        System.out.println("Работникът на " + team + " е деактивиран!");
    }


}
