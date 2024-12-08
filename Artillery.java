package classesSeparated;

import java.awt.*;
import static java.awt.geom.Point2D.distance;

public class Artillery extends Character {
    private ScoutGame game;
    private int enemyBaseX, enemyBaseY;
    private int baseX, baseY;
    private double range;
    private int damage;
    private long lastShotTime;
    private long fireRate;
    protected int healthPoints;


    public Artillery(int baseX, int baseY, int enemyBaseX, int enemyBaseY, String team, ScoutGame game) {
        super(baseX, baseY, team, "artillery");
        this.game = game;
        this.enemyBaseX = enemyBaseX;
        this.enemyBaseY = enemyBaseY;
        this.baseX = baseX;
        this.baseY = baseY;

        double angleToEnemyBase = Math.toDegrees(Math.atan2(enemyBaseY - baseY, enemyBaseX - baseX));

        double distanceFromBase = 500.0;//////////////////////////////////////////////////////////////
        this.x = baseX + distanceFromBase * Math.cos(Math.toRadians(angleToEnemyBase));
        this.y = baseY + distanceFromBase * Math.sin(Math.toRadians(angleToEnemyBase));

        this.currentAngle = angleToEnemyBase;

        this.range = 700.0;
        this.damage = 5;
        this.fireRate = 260000;
        this.lastShotTime = System.currentTimeMillis();
    }

    public void drawArtillery(Graphics2D g2d) {
        int bodyRadius = 8;
        g2d.setColor(Color.GRAY);
        g2d.fillOval((int) (x - bodyRadius), (int) (y - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        g2d.setColor(Color.YELLOW);
        int lineLength = 20;
        int x2 = (int) (x + lineLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = (int) (y + lineLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.drawLine((int)x, (int)y, x2, y2);

        g2d.setFont(new Font("Consolas", Font.BOLD, 10));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Art", (int)x - 10, (int)y - 10);
    }


    public void updateArtillery() {
        if (!isActive()) return;
        Character target = findTarget();
        if (target != null && canShoot()) {
            shootAtEnemyBase(target);
        }
    }


    private Character findTarget() {
        Character closestTarget = null;
        double closestDistance = range;

        for (Character c : game.getCharacters()) {
            if (!c.getTeam().equals(this.team) && c.isActive()) {
                double dist = distance(this.x, this.y, c.getX(), c.getY());
                if (dist <= closestDistance) {
                    closestTarget = c;
                    closestDistance = dist;
                }
            }
        }

        return closestTarget;
    }

    private boolean canShoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= fireRate) {
            lastShotTime = currentTime;
            return true;
        }
        return false;
    }

    private void shootAtEnemyBase(Character target) {
        double angleToTarget = Math.toDegrees(Math.atan2(target.getY() - y, target.getX() - x));
        // Може да визуализираме изстрел
        game.drawShot((int)this.x, (int)this.y,
                (int)target.getX(), (int)target.getY());

        target.takeDamage(damage);

        this.currentAngle = angleToTarget;
    }

    @Override
    public void takeDamage(int dmg) {

        this.healthPoints -= dmg;
        if (this.healthPoints <= 0) {
            this.healthPoints = 0;
            this.setActive(false);
        }
    }

    @Override
    public String getType() {
        return "";
    }

    public double getRange() {
        return range;
    }

    public int getDamage() {
        return damage;
    }
}
