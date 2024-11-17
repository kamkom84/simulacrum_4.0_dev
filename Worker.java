package classesSeparated;

import java.awt.*;
import java.awt.geom.Point2D;
import javax.swing.Timer;
import static java.awt.geom.Point2D.distance;

public class Worker extends Character {
    private boolean isActive = false;
    private boolean hasStarted = false;
    private boolean hasResource = false;
    private Resource targetResource = null;
    private int targetResourceIndex = -1;
    private boolean returningToBase = false;
    private boolean waitingOutsideBase = false;
    private boolean waitingInBase = false;
    private long resourceAcquisitionTime = 0;
    private long baseStayStartTime = 0;
    private Point startPosition;
    private int[] resourceValues;
    private static boolean[] resourceOccupied;
    private int baseWidth;
    private int baseHeight;
    private ScoutGame scoutGame;
    private Resource[] resources;
    private int workerId;
    private static final int RESOURCE_POINTS = 5;
    private int health = 2000;
    private boolean underAttack = false;
    private long lastDamageTime = 0;
    private static final int ATTACK_DISPLAY_DURATION = 500;
    private int id;
    private Color color;


    public Worker(int startX, int startY, String team, Resource[] resources, int[] resourceValues,
                  boolean[] resourceOccupied, int baseWidth, int baseHeight, ScoutGame game, int id) {
        super(startX, startY, team, "worker");
        this.scoutGame = game;
        this.resourceValues = resourceValues;
        Worker.resourceOccupied = resourceOccupied;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.resources = resources;
        this.id = id; // Запазваме идентификатора
        this.startPosition = new Point(startX, startY);

    }

    public void activate() {
        this.isActive = true;
        this.hasStarted = true;
        this.hasResource = false;
        this.returningToBase = false;
        this.targetResource = null;
        this.waitingOutsideBase = false;
    }

    public void updateWorkerCycle(Resource[] resources, int baseX, int baseY, Scout enemyScout) {
        if (health <= 0) { // Ако работникът е убит, не правим нищо
            return;
        }

        if (!isActive) {
            return; // Ако работникът е неактивен, не го реактивираме автоматично
        }

        if (waitingOutsideBase) {
            return;
        }

        if (returningToBase) {
            returnToBase(baseX, baseY);
        } else if (hasResource) {
            gatherResource();
        } else {
            if (targetResource == null || (targetResourceIndex >= 0 && resourceValues[targetResourceIndex] < 5)) {
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

        update();
    }






    public int getId() {
        return id;
    }

    private Resource findNearestAvailableResource(Resource[] resources) {
        Resource nearest = null;
        double minDistance = Double.MAX_VALUE;
        int closestResourceIndex = -1;

        for (int i = 0; i < resources.length; i++) {
            if (!resourceOccupied[i] && resources[i].getValue() > 0) { // Проверяваме само наличните ресурси
                double distance = distance(resources[i].getX(), resources[i].getY(), this.x, this.y);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = resources[i];
                    closestResourceIndex = i;
                }
            }
        }

        if (closestResourceIndex != -1) {
            targetResourceIndex = closestResourceIndex;
            resourceOccupied[targetResourceIndex] = true; // Запазваме ресурса
            System.out.println("Worker " + id + " chose resource " + closestResourceIndex + " with " + nearest.getValue() + " points.");
        }

        return nearest;
    }


    private void moveToResource() {
        if (targetResource == null) return;

        double distance = distance(targetResource.getX(), targetResource.getY(), x, y);
        double moveSpeed = 5.0;
        int resourceRadius = 20;
        int workerRadius = 6;

        if (distance > (resourceRadius + workerRadius)) {
            double moveX = ((targetResource.getX() - x) / distance) * moveSpeed;
            double moveY = ((targetResource.getY() - y) / distance) * moveSpeed;
            x += moveX;
            y += moveY;
            setAngle(Math.toDegrees(Math.atan2(targetResource.getY() - y, targetResource.getX() - x)));
        } else {
            hasResource = true;
            resourceAcquisitionTime = System.currentTimeMillis();
        }
    }

    private void gatherResource() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - resourceAcquisitionTime >= 60000) {
            if (targetResource != null && targetResource.getValue() >= RESOURCE_POINTS) {
                targetResource.reducePoints(RESOURCE_POINTS);
                hasResource = false;
                returningToBase = true;

                if (targetResource.getValue() < 5) {
                    resourceOccupied[targetResourceIndex] = false;
                }
            } else {
                hasResource = false;
                resourceOccupied[targetResourceIndex] = false;
                targetResource = null;
                targetResourceIndex = -1;
            }
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
            setAngle(Math.toDegrees(Math.atan2(targetY - y, targetX - x)));
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
            setAngle(Math.toDegrees(Math.atan2(waitY - y, waitX - x)));
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
        scoutGame.addPointsToScoutBase(team, RESOURCE_POINTS);
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            setInactive();
        } else {
            underAttack = true;
            lastDamageTime = System.currentTimeMillis();

            Color originalColor = this.color;
            this.color = Color.YELLOW;

            new Timer(ATTACK_DISPLAY_DURATION, e -> {
                this.color = originalColor;
                underAttack = false;
                ((Timer) e.getSource()).stop();
            }).start();
        }
    }

    public boolean shouldDisplayPoints() {
        long currentTime = System.currentTimeMillis();
        return underAttack && (currentTime - lastDamageTime < ATTACK_DISPLAY_DURATION);
    }

    private void update() {
        if (underAttack && System.currentTimeMillis() - lastDamageTime > 2000) {
            underAttack = false;
        }
    }

    public void draw(Graphics2D g2d) {
        int bodyRadius = 5;
        g2d.setColor(getCurrentColor());
        g2d.fillOval((int) (x - bodyRadius), (int) (y - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        int lineLength = 10;
        int x2 = (int) (x + lineLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = (int) (y + lineLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.setColor(Color.YELLOW);
        g2d.drawLine((int) x, (int) y, x2, y2);

        if (shouldDisplayPoints()) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("" + health, (int) x - 10, (int) y - 10);
        }
    }

    public Color getCurrentColor() {
        if (isUnderAttack()) {
            return Color.YELLOW;
        }

        return team.equals("blue") ? new Color(0, 100, 255) : new Color(200, 50, 50);
    }

    public void setInactive() {
        this.isActive = false;
        this.hasStarted = true; // Убедете се, че е маркиран като стартиран
        this.health = 0; // Установете здравето на 0, за да отбележите, че работникът е мъртъв

        // Освобождаване на ресурс, ако работникът е бил зает с такъв
        if (targetResourceIndex >= 0 && resourceOccupied[targetResourceIndex]) {
            resourceOccupied[targetResourceIndex] = false;
        }

        this.targetResource = null;
        this.targetResourceIndex = -1;
        this.returningToBase = false;
        this.hasResource = false;
        this.waitingOutsideBase = false;
        this.waitingInBase = false;

        this.x = -1000;
        this.y = -1000;
        System.out.println("Worker " + id + " of team " + team + " is now inactive.");
    }


    public String getTeam() {
        return this.team;
    }

    public boolean isWorkingOn(Resource resource) {
        return this.targetResource != null && this.targetResource.equals(resource);
    }

    public void setAngle(double angle) {
        this.currentAngle = angle;
    }

    public boolean isUnderAttack() {
        long currentTime = System.currentTimeMillis();
        return underAttack && (currentTime - lastDamageTime <= ATTACK_DISPLAY_DURATION);
    }

    public int getHealth() {
        return health;
    }

    public int getBodyRadius() {
        return 5;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String getType() {
        return "worker";
    }


    public boolean isAtBase(int baseX, int baseY) {
        int tolerance = 10; // Толеранс в пиксели
        return Math.abs(this.x - baseX) <= tolerance && Math.abs(this.y - baseY) <= tolerance;
    }


    public void moveToBase(int baseX, int baseY) {
        // Местене на работника към координатите на базата
        this.x = baseX; // Или базовото X
        this.y = baseY; // Или базовото Y

        // Ако имате нужда от плавно движение, използвайте допълнителна логика.
        System.out.println("Worker " + this.getId() + " moved to base (" + baseX + ", " + baseY + ").");
    }

    public boolean isAtStartPosition(int baseX, int baseY) {
        int startX = baseX + (team.equals("blue") ? 50 : -50);
        int startY = baseY + (id * 30); // Изчислява се уникалната стартова позиция по ред

        return Math.abs(x - startX) <= 5 && Math.abs(y - startY) <= 5; // Допустима грешка от 5 пиксела
    }

    public void moveToStartPosition(int baseX, int baseY) {
        int startX = baseX + (team.equals("blue") ? 50 : -50);
        int startY = baseY + (id * 30); // Изчислява се уникалната стартова позиция по ред

        if (Math.abs(x - startX) > 5) {
            x += (startX - x) / Math.abs(startX - x); // Преместване по X
        }
        if (Math.abs(y - startY) > 5) {
            y += (startY - y) / Math.abs(startY - y); // Преместване по Y
        }
    }

}
