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

    public Defender(int startX, int startY, String team, String role, double initialAngle, ScoutGame game) {
        super(startX, startY, team, role);
        this.angleOffset = initialAngle;
        this.game = game;

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
                System.out.println("Hit! Soldier loses 1 point.");
                soldier.decreaseHealth(1); // Намалява здравето на войника

                soldier.moveBackFrom((int) this.x, (int) this.y); // Отместване назад след попадение

                iterator.remove(); // Премахваме куршума след попадение
            } else if (!projectile.isActive()) {
                iterator.remove(); // Премахваме неактивните куршуми
            }
        }
    }


    public void drawProjectiles(Graphics g) {
        for (Projectile projectile : projectiles) {
            projectile.draw(g);
        }
    }

    public void checkAndShootIfScoutInRange(Scout scout) {
        double distance = Math.hypot(this.x - scout.getX(), this.y - scout.getY());

        if (distance <= SHOOT_RANGE) {
            double angleToScout = Math.atan2(scout.getY() - this.y, scout.getX() - this.x);
            rotateTowards(angleToScout);

            // Проверете дали охраната е насочена достатъчно близо до целта
            if (Math.abs(currentAngle - angleToScout) < Math.toRadians(5)) {
                shootAt(scout); // Стреляйте само ако сме насочени към целта
            }
        } else {
            // Върнете се към началния ъгъл, ако няма цел
            if ("red".equalsIgnoreCase(this.team)) {
                rotateTowards(Math.toRadians(180));
            } else if ("blue".equalsIgnoreCase(this.team)) {
                rotateTowards(Math.toRadians(0));
            }
        }
    }


    public void checkAndShootIfSoldiersInRange(ArrayList<Soldier> soldiers) {
        Soldier closestSoldier = null;
        double closestDistance = Double.MAX_VALUE;

        for (Soldier soldier : soldiers) {
            if (!soldier.getTeam().equalsIgnoreCase(this.team)) {
                double distance = Math.hypot(this.x - soldier.getX(), this.y - soldier.getY());
                if (distance <= SHOOT_RANGE && distance < closestDistance) {
                    closestSoldier = soldier;
                    closestDistance = distance;
                }
            }
        }

        if (closestSoldier != null) {
            double angleToSoldier = Math.atan2(closestSoldier.getY() - this.y, closestSoldier.getX() - this.x);
            rotateTowards(angleToSoldier);

            if (Math.abs(currentAngle - angleToSoldier) < Math.toRadians(5)) {
                shootAtSoldier(closestSoldier);
            }
        } else {
            if ("red".equalsIgnoreCase(this.team)) {
                rotateTowards(Math.toRadians(180));
            } else if ("blue".equalsIgnoreCase(this.team)) {
                rotateTowards(Math.toRadians(0));
            }
        }
    }


    private void shootAtSoldier(Soldier soldier) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < SHOOT_INTERVAL) {
            return; // Прекратяване, ако времето между изстрелите не е изминало
        }

        lastShotTime = currentTime;

        // Създаване на куршум с правилните параметри
        Projectile projectile = new Projectile(
                this.x,                   // Начална позиция X
                this.y,                   // Начална позиция Y
                soldier.getX(),           // Целева позиция X
                soldier.getY(),           // Целева позиция Y
                10.0,                     // Скорост на куршума (оптимална стойност)
                500.0                     // Максимално разстояние на куршума
        );
        projectiles.add(projectile);

        // Визуализация на изстрела
        game.drawShot(
                (int) this.x,
                (int) this.y,
                (int) (this.x + 5 * Math.cos(currentAngle)),
                (int) (this.y + 5 * Math.sin(currentAngle))
        );

        // Възможност за попадение
        double hitChance = 0.6;
        if (Math.random() < hitChance) {
            soldier.decreaseHealth(1);
        }
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
