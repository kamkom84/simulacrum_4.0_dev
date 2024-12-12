package classesSeparated;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Timer;
import java.util.TimerTask;

import static java.awt.geom.Point2D.distance;

public class Soldier extends Character {
    private final int weaponLength = 15;
    private boolean showHealth = false;
    private int damageDealt = 0;
    private Color teamColor;
    private ScoutGame game;
    private int id;
    private int healthPoints;
    private int enemyBaseX;
    private int enemyBaseY;
    private int baseX;
    private int baseY;
    private boolean waiting = false;
    private Projectile currentProjectile;

    public Soldier(int x, int y, String team, int baseX, int baseY, int enemyBaseX, int enemyBaseY, ScoutGame game, int id) {
        super(x, y, team, "soldier");
        this.healthPoints = 10;
        this.teamColor = team.equals("blue") ? Color.BLUE : Color.RED;
        this.currentAngle = Math.toDegrees(Math.atan2(game.getHeight() / 2 - y, game.getWidth() / 2 - x));
        this.game = game;
        this.id = id;
        this.enemyBaseX = enemyBaseX;
        this.enemyBaseY = enemyBaseY;
        this.baseX = baseX;
        this.baseY = baseY;
    }

    public void drawSoldier(Graphics2D g2d) {
        int soldierBodyRadius = 4;

        g2d.setColor(teamColor);
        g2d.fillOval((int) (x - soldierBodyRadius), (int) (y - soldierBodyRadius), soldierBodyRadius * 2, soldierBodyRadius * 2);

        g2d.setColor(Color.YELLOW);
        int x1 = (int) x;
        int y1 = (int) y;
        int x2 = x1 + (int) (weaponLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = y1 + (int) (weaponLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.drawLine(x1, y1, x2, y2);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        g2d.drawString("" + id, (int) x - 6, (int) y - soldierBodyRadius - 1);

        if (showHealth) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("" + healthPoints, (int) x - 8, (int) y - soldierBodyRadius - 9);
        }

        if (currentProjectile != null) {
            currentProjectile.draw(g2d);
        }
    }

    public void soldierShoot(Character target) {
        if (target == null || !target.isActive()) return;

        double angleToTarget = calculateAngleTo(this.x, this.y, target.getX(), target.getY());
        double distanceToTarget = distance(this.x, this.y, target.getX(), target.getY());

        if (distanceToTarget <= 100) {
            this.currentAngle = angleToTarget;

            currentProjectile = new Projectile(
                    x + weaponLength * Math.cos(Math.toRadians(angleToTarget)),
                    y + weaponLength * Math.sin(Math.toRadians(angleToTarget)),
                    target.getX(),
                    target.getY()
            );

        }
    }

    public void updateProjectile(Character target) {
        if (currentProjectile != null && currentProjectile.isActive()) {
            currentProjectile.updateSoldierProjectilePosition();

            // Проверка за сблъсък с целта
            if (currentProjectile.checkCollision(target)) {
                target.takeDamage(1);
                currentProjectile.deactivate(); // Деактивиране на патрона
            }
        }
    }

    private void maintainDistanceFromTeammates(Soldier[] teammates) {
        double minDistance = 30.0;

        for (Soldier teammate : teammates) {
            if (teammate != null && teammate != this) { // Игнорираме себе си
                double distance = Point2D.distance(this.x, this.y, teammate.getX(), teammate.getY());
                if (distance < minDistance) {
                    // Отместваме войника в обратната посока на съотборника
                    double angleAwayFromTeammate = calculateAngleTo(teammate.getX(), teammate.getY(), this.x, this.y);
                    this.x += 1.0 * Math.cos(Math.toRadians(angleAwayFromTeammate));
                    this.y += 1.0 * Math.sin(Math.toRadians(angleAwayFromTeammate));
                }
            }
        }
    }



    private double calculateAngleTo(double x1, double y1, double x2, double y2) {
        return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    @Override
    public void takeDamage(int damage) {
        this.healthPoints -= damage;

        if (this.healthPoints <= 0) {
            this.healthPoints = 0;
            this.setActive(false);
        } else {
            showHealthTemporarily();
            moveBack();
        }
    }

    private void moveBack() {
        double moveAngle = Math.toRadians(currentAngle + 180);
        final int MOVE_BACK_DISTANCE = 50;
        this.x += MOVE_BACK_DISTANCE * Math.cos(moveAngle);
        this.y += MOVE_BACK_DISTANCE * Math.sin(moveAngle);
    }

    private void showHealthTemporarily() {
        showHealth = true;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showHealth = false;
            }
        }, 1000);
    }

    public void soldierMoveTowardsCenter(Soldier[] teammates) {
        double speed = 2.0;

        // Център на картата
        double centerX = game.getWidth() / 2.0;
        double centerY = game.getHeight() / 2.0;

        // Изчисляваме ъгъла към центъра
        double angleToCenter = calculateAngleTo(this.x, this.y, centerX, centerY);

        // Движим се към центъра
        this.x += speed * Math.cos(Math.toRadians(angleToCenter));
        this.y += speed * Math.sin(Math.toRadians(angleToCenter));

        // Уверяваме се, че не се приближаваме прекалено до съотборниците
        maintainDistanceFromTeammates(teammates);
    }


    public Character findTarget() {
        Character closestTarget = null;
        double closestDistance = 250;

        for (Character character : game.getCharacters()) {
            if (character.getTeam().equals(this.team)) continue;
            if (!character.isActive()) continue;

            double distanceToCharacter = distance(this.x, this.y, character.getX(), character.getY());
            if (distanceToCharacter <= closestDistance) {
                closestTarget = character;
                closestDistance = distanceToCharacter;
            }
        }

        return closestTarget;
    }

    public void updateSoldier(Soldier[] teammates) {
        if (waiting) {
            return;
        }

        if (!isActive()) return;

        Character target = findTarget();

        if (target != null) {
            soldierShoot(target);
            updateProjectile(target);
        } else {
            soldierMoveTowardsCenter(teammates); // Подаваме съотборниците
        }
    }


    @Override
    public String getType() {
        return "Soldier";
    }

    public int getId() {
        return id;
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public int getWeaponLength() {
        return weaponLength;
    }

    private class Projectile {
        private double x, y;
        private double targetX, targetY;
        private double directionAngle;
        private double speed = 5.0;
        private boolean active = true;

        public Projectile(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.directionAngle = Math.toDegrees(Math.atan2(targetY - startY, targetX - startX));
        }

        public void updateSoldierProjectilePosition() {
            if (!active) return;

            double dx = speed * Math.cos(Math.toRadians(directionAngle));
            double dy = speed * Math.sin(Math.toRadians(directionAngle));

            // Движение към целта
            this.x += dx;
            this.y += dy;

            // Проверка дали патронът е достигнал целта
            if (Math.abs(x - targetX) <= speed && Math.abs(y - targetY) <= speed) {
                this.x = targetX;
                this.y = targetY;
                this.active = false; // Деактивиране на патрона
            }
        }

        public boolean checkCollision(Character target) {
            // Проверка за сблъсък с врага
            return Point2D.distance(x, y, target.getX(), target.getY()) < 5;
        }

        public void draw(Graphics2D g2d) {
            if (!active) return;

            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine((int) x, (int) y, (int) targetX, (int) targetY);
        }

        public boolean isActive() {
            return active;
        }

        public void deactivate() {
            this.active = false;
        }
    }



}
