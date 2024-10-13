package classesSeparated;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static java.awt.geom.Point2D.distance;

public class Worker extends Character {
    private boolean isActive = false;
    private boolean hasResource = false;
    private Point targetResource = null;
    private int targetResourceIndex = -1;
    private boolean returningToBase = false;
    private static final int RESOURCE_POINTS = 5;
    private long resourceAcquisitionTime = 0;
    private long baseStayStartTime = 0;
    private boolean waitingInBase = false;
    private Random random = new Random();
    private int[] resourceValues;
    private boolean[] resourceOccupied;
    private int baseWidth;
    private int baseHeight;
    private ScoutGame scoutGame;
    private int[] resourceOccupancy;
    private Point[] resources;
    private int health = 100; // Здраве на работника
    private int workerId; // Номер на работника
    private List<Worker> allWorkers;

    public Worker(int startX, int startY, String team, Point[] resources, int[] resourceValues,
                  boolean[] resourceOccupied, int[] resourceOccupancy, int baseWidth, int baseHeight,
                  ScoutGame game, int workerId) {
        super(startX, startY, team, "worker");
        this.scoutGame = game;
        this.resourceValues = resourceValues;
        this.resourceOccupied = resourceOccupied;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.resourceOccupancy = resourceOccupancy;
        this.resources = resources;
        this.workerId = workerId; // Инициализация на номера на работника
        this.isActive = true;

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
        this.hasResource = false;
        this.returningToBase = false;
        this.targetResource = null;
        System.out.println("Worker " + workerId + " activated. isActive set to: " + isActive);
    }



    public void updateWorkerCycle(Point[] resources, int baseX, int baseY, Scout blueScout) {
        if (!isActive) return;

        if (returningToBase) {
            System.out.println("Worker " + workerId + " is returning to base.");
            returnToBase(baseX, baseY);
        } else if (hasResource) {
            System.out.println("Worker " + workerId + " is gathering resources.");
            gatherResource();
        } else {
            if (targetResource == null || resourceValues[targetResourceIndex] <= 0) {
                targetResource = findNearestAvailableResource(resources);
                if (targetResource == null) {
                    returningToBase = true;
                } else {
                    System.out.println("Worker " + workerId + " moving to target resource at (" + targetResource.x + ", " + targetResource.y + ")");
                }
            }

            if (targetResource != null) {
                moveToResource(resources, baseX, baseY, resourceValues);
            }
        }
    }






    private Point findNearestAvailableResource(Point[] resources) {
        Point nearest = null;
        double minDistance = Double.MAX_VALUE;
        int closestResourceIndex = -1;

        System.out.println("Worker " + workerId + " is searching for the nearest available resource.");

        for (int i = 0; i < resources.length; i++) {
            if (!resourceOccupied[i] && resourceValues[i] > 0) {
                double distance = distance(resources[i].x, resources[i].y, this.x, this.y);
                System.out.println("Resource " + i + " at (" + resources[i].x + ", " + resources[i].y +
                        ") with value " + resourceValues[i] + " is available. Distance: " + distance);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = resources[i];
                    closestResourceIndex = i;
                }
            } else {
                if (resourceOccupied[i]) {
                    System.out.println("Resource " + i + " is occupied.");
                } else if (resourceValues[i] <= 0) {
                    System.out.println("Resource " + i + " is depleted.");
                }
            }
        }

        // Ако е намерен валиден ресурс
        if (closestResourceIndex != -1) {
            resourceOccupied[closestResourceIndex] = true;
            targetResourceIndex = closestResourceIndex;
            System.out.println("Worker " + workerId + " selected resource " + closestResourceIndex +
                    " at (" + nearest.x + ", " + nearest.y + ").");
        } else {
            System.out.println("Worker " + workerId + " could not find any available resource.");
        }

        return nearest;
    }


    private void checkForEnemyScout(Scout enemyScout, Point[] resources) {
        if (enemyScout == null) return;

        double visionRadius = 15.0;

        if (distanceToAnt(enemyScout) <= visionRadius) {
            System.out.println(team + " worker spotted enemy scout! Avoiding!");
            double angleAwayFromScout = Math.atan2(this.y - enemyScout.getY(), this.x - enemyScout.getX());
            Point newTarget = findNearestResourceInOppositeDirection(resources, angleAwayFromScout);
            if (newTarget != null) {
                this.targetResource = newTarget;
                this.hasResource = false;
                this.returningToBase = false;
            }
        }
    }

    private Point findNearestResourceInOppositeDirection(Point[] resources, double angle) {
        Point nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Point resource : resources) {
            double distance = distance(resource.x, resource.y, this.x, this.y);
            double angleToResource = Math.atan2(resource.y - this.y, resource.x - this.x);

            if (distance < minDistance && Math.abs(angleToResource - angle) < Math.PI / 2) {
                minDistance = distance;
                nearest = resource;
            }
        }

        return nearest;
    }

    private void moveToResource(Point[] resources, int baseX, int baseY, int[] resourceValues) {
        System.out.println("Worker " + workerId + " moving towards resource at (" + targetResource.x + ", " + targetResource.y + ")");

        if (targetResource == null) {
            targetResource = findNearestAvailableResource(resources);
            System.out.println("Worker " + workerId + " target resource: " + targetResource);
            returningToBase = targetResource == null; // Only return to base if no resource is found
        }

        int resourceRadius = 20;
        int workerRadius = 6;
        double moveSpeed = 5.0;

        double distance = distance(targetResource.x, targetResource.y, x, y);

        if (distance > (resourceRadius + workerRadius)) {
            double moveX = ((targetResource.x - x) / distance) * moveSpeed;
            double moveY = ((targetResource.y - y) / distance) * moveSpeed;
            x += moveX;
            y += moveY;
            System.out.println("Worker " + workerId + " moving towards resource. Position: (" + x + ", " + y + ")");

            // Логика за избягване на сблъсък с други работници
            for (Worker other : scoutGame.getAllWorkers()) {
                if (other != this && distanceToAnt(other) < 20) {
                    double avoidanceFactor = 0.05;
                    moveX += (this.x - other.x) * avoidanceFactor;
                    moveY += (this.y - other.y) * avoidanceFactor;
                }
            }

            // Добавяне на случайно отклонение
            double randomFactor = 0.5;
            moveX += (random.nextDouble() - 0.5) * randomFactor;
            moveY += (random.nextDouble() - 0.5) * randomFactor;

            // Актуализиране на координатите на работника
            x += moveX;
            y += moveY;
            angle = Math.toDegrees(Math.atan2(targetResource.y - y, targetResource.x - x));

            System.out.println("Worker " + workerId + " moved to (" + x + ", " + y + "), targeting (" + targetResource.x + ", " + targetResource.y + ")");
        } else {
            System.out.println("Worker " + workerId + " reached the resource.");
            hasResource = true;
            resourceOccupancy[targetResourceIndex]++;
            resourceAcquisitionTime = System.currentTimeMillis();
        }

        if (distance <= (resourceRadius + workerRadius)) {
            if (!hasResource) {
                resourceAcquisitionTime = System.currentTimeMillis();
                hasResource = true;
                resourceOccupancy[targetResourceIndex]++;
                System.out.println("Worker " + workerId + " reached the resource and started gathering.");
            }
        }
    }




    private int getResourceIndex(Point resource) {
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].equals(resource)) {
                return i;
            }
        }
        return -1;
    }

    private Point findNearestResource(Point[] resources) {
        Point nearest = null;
        double minDistance = Double.MAX_VALUE;
        int closestResourceIndex = -1;

        for (int i = 0; i < resources.length; i++) {
            if (!resourceOccupied[i] && resourceValues[i] > 0) {
                double distance = distance(resources[i].x, resources[i].y, this.x, this.y);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = resources[i];
                    closestResourceIndex = i;
                }
            }
        }

        if (closestResourceIndex != -1) {
            resourceOccupied[closestResourceIndex] = true;
            targetResourceIndex = closestResourceIndex;
        }

        return nearest;
    }

    private void gatherResource() {
        long currentTime = System.currentTimeMillis();
        // Assuming it takes 60 seconds to gather resources
        if (currentTime - resourceAcquisitionTime >= 60000) {
            int resourceIndex = targetResourceIndex;

            if (resourceIndex >= 0 && resourceValues[resourceIndex] > 0) {
                resourceValues[resourceIndex] -= RESOURCE_POINTS;
                hasResource = false;
                returningToBase = true;
                System.out.println("Worker " + workerId + " gathered resources and is returning to base.");
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
            System.out.println("Worker " + workerId + " moving to base at (" + targetX + ", " + targetY + "). Current Position: (" + x + ", " + y + ")");

        } else {
            long currentTime = System.currentTimeMillis();
            if (!waitingInBase) {
                waitingInBase = true;
                baseStayStartTime = System.currentTimeMillis();
                System.out.println("Worker " + workerId + " reached base. Waiting to unload.");

            }
//            long currentTime = System.currentTimeMillis();

            if (currentTime - baseStayStartTime >= 60000) {
                addPointsToBase();
                prepareForNextCycle();
                System.out.println("Worker " + workerId + " completed unload. Ready to start next cycle.");

            }
        }
    }

    private void prepareForNextCycle() {
        returningToBase = false;
        waitingInBase = false;
        hasResource = false;
        isActive = true;
        baseStayStartTime = 0;
        System.out.println("Worker " + workerId + " is ready to start the next cycle.");
    }



    private void addPointsToBase() {
        int pointsToAdd = 5;
        if (team.equals("blue")) {
            scoutGame.setBlueBaseHealth(scoutGame.getBlueBaseHealth() + pointsToAdd);
        } else if (team.equals("red")) {
            scoutGame.setRedBaseHealth(scoutGame.getRedBaseHealth() + pointsToAdd);
        }
    }


    public String getTeam() {
        return this.team;
    }


}
