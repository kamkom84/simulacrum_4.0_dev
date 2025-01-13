package classesSeparated;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Defender extends Character {
    private double speed = 0.03;
    private double angleOffset;
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private static final int SHOOT_RANGE = 400;/////////////////////////////////////////////////////////////////////////
    private static final int SHOOT_INTERVAL = 1000;//////////////////////////////////////////////////////////////////////
    private final ScoutGame game;
    private long lastShotTime = 0;
    private double currentAngle;
    private boolean scoutInSight = false;
    private int healthPoints;

    public Defender(int startX, int startY, String team, String role, ScoutGame game, double initialAngle) {
        super(startX, startY, team, role);
        this.game = game;
        this.angleOffset = initialAngle;
        this.healthPoints = 25;////////////////////////////////////////////////////////////////////////////////////////

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

        Projectile projectile = new Projectile(
                this.x,
                this.y,
                scout.getX(),
                scout.getY(),
                40.0,
                500.0
        );
        projectiles.add(projectile);

        game.drawShot(
                (int) this.x,
                (int) this.y,
                (int) (this.x + 5 * Math.cos(currentAngle)),
                (int) (this.y + 5 * Math.sin(currentAngle))
        );

        double hitChance = 0.7;
        if (Math.random() < hitChance) {
            scout.decreaseHealth(1);
        }
    }

    public void updateProjectiles(Scout scout) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.updateProjectilePosition();

            if (projectile.hasProjectileHit(scout)) {
                //System.out.println("Hit! Scout loses 1 point.");
                scout.decreaseHealth(1);
                //game.addExplosionEffect(scout.getX(), scout.getY(), 20, Color.RED, 500);
                scout.moveBackFrom((int) this.x, (int) this.y);

                iterator.remove();
            } else if (!projectile.isProjectileActive()) {
                iterator.remove();
            }
        }
    }

    public void updateProjectilesForSoldier(ArrayList<Soldier> soldiers) {
        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.updateProjectilePosition(); // Актуализиране на позицията

            // Проверка за попадение върху някой от войниците
            boolean hit = false;
            for (Soldier soldier : soldiers) {
                if (soldier != null && soldier.isActive() && projectile.hasProjectileHit(soldier)) {
                    soldier.decreaseHealth(1); // Намаляване на здравето на войника
                    soldier.moveBackFrom((int) this.x, (int) this.y); // Отдръпване на войника
                    hit = true; // Маркираме, че патронът е ударил цел
                    break; // Спиране на проверките след първото попадение
                }
            }

            // Премахваме проектилите, които са ударили целта или са изчерпали максималното си разстояние
            if (hit || !projectile.isProjectileActive()) {
                iterator.remove(); // Премахване чрез итератора
            }
        }
    }

    public void drawDefenderProjectiles(Graphics g) {
        for (Projectile projectile : projectiles) {
            projectile.drawProjectile(g);
        }
    }

    public void checkAndShootIfScoutInRange(Scout scout) {
        if (scout == null) return;

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
        for (Soldier soldier : soldiers) {
            if (!soldier.getTeam().equalsIgnoreCase(this.team) && soldier.isActive()) {
                double distanceToSoldier = Math.hypot(soldier.getX() - this.x, soldier.getY() - this.y);
                if (distanceToSoldier <= SHOOT_RANGE) {
                    // Стреля по всеки войник в обсега
                    this.currentAngle = Math.atan2(soldier.getY() - this.y, soldier.getX() - this.x);
                    defenderShootAtSoldier(soldier);
                }
            }
        }
    }

    private void defenderShootAtSoldier(Soldier soldier) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < SHOOT_INTERVAL) {
            return; // Ограничение за стрелба през определен интервал
        }

        lastShotTime = currentTime;

        // Изчисляване на ъгъла към войника
        double angleToTarget = Math.atan2(soldier.getY() - this.y, soldier.getX() - this.x);

        // Създаване на нов патрон към конкретния войник
        Projectile projectile = new Projectile(
                this.x, // Начална X позиция
                this.y, // Начална Y позиция
                soldier.getX(), // Целева X позиция
                soldier.getY(), // Целева Y позиция
                40.0, // Скорост на патрона
                500.0 // Максимален обхват на патрона
        );
        projectiles.add(projectile); // Добавяне на патрона в списъка за визуализация и движение

        // Визуализация на изстрела
        game.drawShot(
                (int) this.x,
                (int) this.y,
                (int) (this.x + 15 * Math.cos(angleToTarget)), // По-добра видимост на изстрела
                (int) (this.y + 15 * Math.sin(angleToTarget))
        );
    }

    public void drawDefenderWeaponDirection(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{10f, 10f}, 0f));

        int endX = (int) (this.x + 20 * Math.cos(currentAngle));
        int endY = (int) (this.y + 20 * Math.sin(currentAngle));

        g2d.drawLine((int) this.x, (int) this.y, endX, endY);
    }

    public void drawDefender(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        int visualRadius = getRadius() / 2;

        g2d.setColor("red".equalsIgnoreCase(team) ? Color.RED : Color.BLUE);
        g2d.fillOval((int) (x - visualRadius), (int) (y - visualRadius), visualRadius * 2, visualRadius * 2);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        g2d.drawString(String.valueOf(healthPoints), (int) x - 5, (int) y - visualRadius - 5);

        drawDefenderWeaponDirection(g);
    }

    @Override
    public String getType() {
        return "defender";
    }

    public void reduceHealthPoints(int points) {
        this.healthPoints -= points;
        if (this.healthPoints <= 0) {
            this.healthPoints = 0;
            this.setActive(false); // Деактивиране на защитника
        }
    }

    public int getRadius() {
        return 15; // Примерен радиус на защитника
    }

    public int getHealthPoints() {
        return this.healthPoints;
    }

}
