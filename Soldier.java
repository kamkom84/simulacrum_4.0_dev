package classesSeparated;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Timer;
import java.util.TimerTask;

import static java.awt.geom.Point2D.distance;

public class Soldier extends Character {
    private final int weaponLength = 10;////////////////////////////////////////////////////////////////////////////////
    private boolean showHealth = false;
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
    private long lastShotTime = 0;
    private float alpha = 1.0f;


    public Soldier(int x, int y, String team, int baseX, int baseY, int enemyBaseX, int enemyBaseY, ScoutGame game, int id) {
        super(x, y, team, "soldier");
        this.healthPoints = 50;/////////////////////////////////////////////////////////////////////////////////////////
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

        int soldierBodyRadius = 5;

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

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < 2000) return;//////////////////////////////////////////////////////////////////

        double angleToTarget = calculateAngleTo(this.x, this.y, target.getX(), target.getY());
        double distanceToTarget = distance(this.x, this.y, target.getX(), target.getY());

        if (distanceToTarget <= 100) {///////////////////////////////////////////////////////////////////////////////////
            this.currentAngle = angleToTarget;

            currentProjectile = new Projectile(
                    x + weaponLength * Math.cos(Math.toRadians(angleToTarget)),
                    y + weaponLength * Math.sin(Math.toRadians(angleToTarget)),
                    target.getX(),
                    target.getY()
            );

            lastShotTime = currentTime; // Запазване на времето на последния изстрел
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
        double minDistance = 50.0;

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

    public void moveBack() {
        double moveAngle = Math.toRadians(currentAngle + 180);
        final int MOVE_BACK_DISTANCE = 70;//////////////////////////////////////////////////////////////////////////////
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
        double speed = 0.8;

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

        // Намиране на цел
        Character target = findTarget();

        if (target != null) {
            // Ако има цел, стреля
            soldierShoot(target);
            updateProjectile(target);
        } else {
            // Ако няма цел, продължава движение към центъра
            soldierMoveTowardsCenter(teammates);
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

    public void moveBackFrom(int x, int y) {
        // Изчисляване на посоката за отместване
        double angle = Math.atan2(this.y - y, this.x - x);
        int offset = 10; // Стойност за отместване назад

        // Актуализиране на координатите
        this.x += offset * Math.cos(angle);
        this.y += offset * Math.sin(angle);
    }


    private class Projectile {
        private double x, y;
        private double targetX, targetY;
        private double directionAngle;
        private double speed = 30.0; ///////////////////////////////////////////////////////////////////////////////////
        private boolean active = true;
        private String team;

        public Projectile(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.directionAngle = Math.toDegrees(Math.atan2(targetY - startY, targetX - startX));
        }



        private float alpha = 1.0f;

        public void updateSoldierProjectilePosition() {
            if (!active) return;

            double dx = speed * Math.cos(Math.toRadians(directionAngle));
            double dy = speed * Math.sin(Math.toRadians(directionAngle));

            // Движение към целта
            this.x += dx;
            this.y += dy;

            // Проверка за удряне на всички цели на пътя
            for (Character character : game.getCharacters()) {
                if (!character.isActive() || character.getTeam().equals(Soldier.this.team)) continue;

                if (Point2D.distance(x, y, character.getX(), character.getY()) < 5) {
                    character.takeDamage(1);
                    this.active = false; // Деактивиране на патрона след удряне на цел
                    return;
                }
            }

            // Проверка дали патронът е преминал 5 пиксела след целта
            if (Point2D.distance(x, y, targetX, targetY) > 5) {
                alpha -= 0.1f; // Намаляване на прозрачността с всяка стъпка
                if (alpha <= 0) {
                    this.active = false; // Деактивиране на патрона, когато стане напълно невидим
                }
            }
        }







        public boolean checkCollision(Character target) {
            // Проверка за сблъсък с врага
            return Point2D.distance(x, y, target.getX(), target.getY()) < 5;
        }

        public void draw(Graphics2D g2d) {
            if (!active) return;

            // Настройка на прозрачността
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Рисуване на по-къса линия, започваща от края на оръжието
            double startX = x - 3 * Math.cos(Math.toRadians(directionAngle));
            double startY = y - 3 * Math.sin(Math.toRadians(directionAngle));
            double endX = x + 3 * Math.cos(Math.toRadians(directionAngle));
            double endY = y + 3 * Math.sin(Math.toRadians(directionAngle));

            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawLine((int) startX, (int) startY, (int) endX, (int) endY);

            // Възстановяване на оригиналната прозрачност
            g2d.setComposite(originalComposite);
        }

        public boolean isActive() {
            return active;
        }

        public void deactivate() {
            this.active = false;
        }
    }

}
