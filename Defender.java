package classesSeparated;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Defender extends Character {
    private double speed = 0.02;
    private double angleOffset;
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private static final int SHOOT_RANGE = 500;
    private static final int SHOOT_INTERVAL = 1500;
    private final ScoutGame game;
    private long lastShotTime = 0;
    private double currentAngle;
    private boolean scoutInSight = false;

    public Defender(int startX, int startY, String team, String role, ScoutGame game, double initialAngle) {
        super(startX, startY, team, role);
        this.game = game;
        this.angleOffset = initialAngle;

        if ("red".equalsIgnoreCase(team)) {
            this.currentAngle = Math.toRadians(180);
        } else if ("blue".equalsIgnoreCase(team)) {
            this.currentAngle = Math.toRadians(0);
        }
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

    private void shootAt(Scout scout) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < SHOOT_INTERVAL) {
            return;
        }

        lastShotTime = currentTime;

        // Създаване на куршум с правилните аргументи
        Projectile projectile = new Projectile(
                this.x,               // Начална позиция X
                this.y,               // Начална позиция Y
                scout.getX(),         // Целева позиция X
                scout.getY(),         // Целева позиция Y
                30.0,                 // Скорост
                500.0                 // Максимално разстояние
        );
        projectiles.add(projectile);

        // Визуализация на изстрела
        game.drawShot(
                (int) this.x,
                (int) this.y,
                (int) (this.x + 5 * Math.cos(currentAngle)),
                (int) (this.y + 5 * Math.sin(currentAngle))
        );

        // Шанс за попадение
        double hitChance = 0.7;
        if (Math.random() < hitChance) {
            scout.decreaseHealth(1);
        }
    }


    public void updateProjectiles(Scout scout) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.updatePosition();

            if (projectile.hasHit(scout)) {
                System.out.println("Hit! Scout loses 1 point.");
                scout.decreaseHealth(1);
                //game.addExplosionEffect(scout.getX(), scout.getY(), 20, Color.RED, 500);
                scout.moveBackFrom((int) this.x, (int) this.y);

                iterator.remove();
            } else if (!projectile.isActive()) {
                iterator.remove();
            }
        }
    }

    public void updateProjectilesForSoldier(Soldier soldier) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.updatePosition();

            if (projectile.hasHit(soldier)) {
                System.out.println("Hit! Soldier " + soldier.getId() + " loses 1 point.");
                soldier.decreaseHealth(1); // Намаляване на точките на войника

                if (soldier.isActive()) {
                    System.out.println("Soldier " + soldier.getId() + " is still alive.");
                } else {
                    System.out.println("Soldier " + soldier.getId() + " is dead and removed.");
                }

                iterator.remove(); // Премахване на куршума след попадение
            }

        }
    }



    public void drawProjectiles(Graphics g) {
        for (Projectile projectile : projectiles) {
            projectile.draw(g);
        }
    }

    public void checkAndShootIfScoutInRange(Scout scout) {
        if (scout == null) return; // Ако скаутът е премахнат, спираме метода

        double distance = Math.hypot(this.x - scout.getX(), this.y - scout.getY());

        if (distance <= SHOOT_RANGE) {
            double angleToScout = Math.atan2(scout.getY() - this.y, scout.getX() - this.x);
            currentAngle = angleToScout;
            scoutInSight = true;
            shootAt(scout);
        } else if (scoutInSight) {
            if ("red".equalsIgnoreCase(this.team)) {
                currentAngle = Math.toRadians(180);
            } else if ("blue".equalsIgnoreCase(this.team)) {
                currentAngle = Math.toRadians(0);
            }
            scoutInSight = false;
        }
    }



    public void checkAndShootIfSoldiersInRange(ArrayList<Soldier> soldiers) {
        Soldier closestSoldier = null;
        double closestDistance = Double.MAX_VALUE;

        // Find the closest soldier within range
        for (Soldier soldier : soldiers) {
            if (!soldier.getTeam().equalsIgnoreCase(this.team) && soldier.isActive()) { // Ensure not same team and active
                double distanceToSoldier = Math.hypot(soldier.getX() - this.x, soldier.getY() - this.y);
                if (distanceToSoldier <= SHOOT_RANGE && distanceToSoldier < closestDistance) {
                    closestSoldier = soldier;
                    closestDistance = distanceToSoldier;
                }
            }
        }

        // If a target is found, point towards it and shoot
        if (closestSoldier != null) {
            // Update the angle to point directly at the closest soldier
            this.currentAngle = Math.atan2(closestSoldier.getY() - this.y, closestSoldier.getX() - this.x);

            // Shoot at the closest soldier
            shootAtSoldier(closestSoldier);
        }
    }




    private void shootAtSoldier(Soldier soldier) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < SHOOT_INTERVAL) {
            return; // Prevent shooting too frequently
        }

        lastShotTime = currentTime;

        // Create and shoot a projectile aimed at the soldier
        Projectile projectile = new Projectile(
                this.x,
                this.y,
                soldier.getX(),
                soldier.getY(),
                30.0, // Speed
                500.0 // Max distance
        );
        projectiles.add(projectile);

        // Log for debugging
        System.out.println("Defender shot at Soldier " + soldier.getId());
    }



    private void rotateTowards(double targetAngle) {
        double angleDifference = targetAngle - currentAngle;

        // Уверете се, че ъгълът е в диапазона [-PI, PI]
        angleDifference = (angleDifference + Math.PI) % (2 * Math.PI) - Math.PI;

        // Определете скоростта на въртене
        double rotationSpeed = Math.toRadians(5); // Скорост на завъртане (градуси)

        // Завъртане в посока на целевия ъгъл
        if (Math.abs(angleDifference) <= rotationSpeed) {
            currentAngle = targetAngle; // Ако сме много близо до целта, директно задайте целевия ъгъл
        } else {
            currentAngle += Math.signum(angleDifference) * rotationSpeed;
        }

        // Уверете се, че currentAngle е в диапазона [0, 2*PI]
        currentAngle = (currentAngle + 2 * Math.PI) % (2 * Math.PI);
    }






    public void drawDirectionLine(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10f, 10f}, 0f));

        int endX = (int) (this.x + 20 * Math.cos(currentAngle));
        int endY = (int) (this.y + 20 * Math.sin(currentAngle));

        g2d.drawLine((int) this.x, (int) this.y, endX, endY);
    }

    @Override
    public String getType() {
        return "defender";
    }

}
