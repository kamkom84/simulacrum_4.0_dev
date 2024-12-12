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
        this.healthPoints = 50;
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

        // Рисуване на текущия патрон
        if (currentProjectile != null) {
            currentProjectile.draw(g2d);
        }
    }

    public void soldierShoot(Character target) {
        if (target == null || !target.isActive()) return;

        double angleToTarget = calculateAngleTo(this.x, this.y, target.getX(), target.getY());
        double distanceToTarget = distance(this.x, this.y, target.getX(), target.getY());

        if (distanceToTarget <= 100) {
            this.currentAngle = angleToTarget; // Оръжието сочи към врага

            // Създаване на нов патрон
            currentProjectile = new Projectile(
                    x + weaponLength * Math.cos(Math.toRadians(angleToTarget)),
                    y + weaponLength * Math.sin(Math.toRadians(angleToTarget)),
                    angleToTarget
            );
        }
    }

    public void updateProjectile(Character target) {
        if (currentProjectile != null && currentProjectile.isActive()) {
            currentProjectile.updatePosition();

            // Проверка за удар с целта
            if (currentProjectile.checkCollision(target)) {
                target.takeDamage(1);
                currentProjectile = null; // Изчистване на патрона
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

    public void soldierMoveTowardsEnemyBase() {
        double speed = 6.0;

        // Разстояния между колоните и редовете на армията
        int columnSpacing = 30;
        int rowSpacing = 20;
        int soldiersPerColumn = 20; // Максимален брой войници в една колона

        int columnIndex = id / soldiersPerColumn; // Определяне на колоната въз основа на ID-то на войника
        int rowIndex = id % soldiersPerColumn; // Определяне на реда във всяка колона

        // Целева позиция за формация във вертикални колони
        double formationTargetX = baseX + (team.equals("blue") ? columnIndex * columnSpacing : -columnIndex * columnSpacing);
        double formationTargetY = baseY + rowIndex * rowSpacing;

        // Ограничаване на позициите в рамките на екрана
        int screenWidth = game.getWidth();
        int screenHeight = game.getHeight();

        formationTargetX = Math.max(10, Math.min(screenWidth - 10, formationTargetX));
        formationTargetY = Math.max(10, Math.min(screenHeight - 10, formationTargetY));

        // Проверка дали войникът е достигнал целевата позиция на формацията
        if (distance(this.x, this.y, formationTargetX, formationTargetY) > 5) {
            // Движение към позицията на формацията
            double angleToFormation = calculateAngleTo(this.x, this.y, formationTargetX, formationTargetY);
            this.x += speed * Math.cos(Math.toRadians(angleToFormation));
            this.y += speed * Math.sin(Math.toRadians(angleToFormation));
            this.currentAngle = angleToFormation;
        } else {
            // След като е във формация, продължава към центъра на екрана
            double centerX = screenWidth / 2.0;
            double centerY = screenHeight / 2.0;
            double angleToCenter = calculateAngleTo(this.x, this.y, centerX, centerY);
            this.x += speed * Math.cos(Math.toRadians(angleToCenter));
            this.y += speed * Math.sin(Math.toRadians(angleToCenter));
            this.currentAngle = angleToCenter;
        }
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

    public void updateSoldier() {
        if (waiting) {
            return;
        }

        if (!isActive()) return;

        Character target = findTarget();

        if (target != null) {
            soldierShoot(target);
            updateProjectile(target);
        } else {
            soldierMoveTowardsEnemyBase();
        }
    }

    @Override
    public String getType() {
        return "Soldier";
    }

    public void drawPoints(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 8));
        g.drawString(String.valueOf(healthPoints), (int) this.x, (int) this.y - 10);
    }

    public void decreaseHealth(int amount) {
        this.healthPoints -= amount;
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

    public int getId() {
        return id;
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public int getWeaponLength() {
        return this.weaponLength;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    public boolean isWaiting() {
        return waiting;
    }

    // Вътрешен клас за патроните
    private class Projectile {
        private double x, y;
        private double directionAngle;
        private double speed = 5.0;
        private int length = 10;
        private boolean active = true;

        public Projectile(double startX, double startY, double directionAngle) {
            this.x = startX;
            this.y = startY;
            this.directionAngle = directionAngle;
        }

        public void updatePosition() {
            if (!active) return;

            x += speed * Math.cos(Math.toRadians(directionAngle));
            y += speed * Math.sin(Math.toRadians(directionAngle));
        }

        public boolean checkCollision(Character target) {
            double distanceToTarget = Point2D.distance(x, y, target.getX(), target.getY());
            if (distanceToTarget < 5) {
                active = false;
                return true;
            }
            return false;
        }

        public void draw(Graphics2D g2d) {
            if (!active) return;

            double endX = x + length * Math.cos(Math.toRadians(directionAngle));
            double endY = y + length * Math.sin(Math.toRadians(directionAngle));
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine((int) x, (int) y, (int) endX, (int) endY);
        }

        public boolean isActive() {
            return active;
        }
    }
}
