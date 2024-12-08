package classesSeparated;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Artillery extends Character {
    private ScoutGame game;
    private int enemyBaseX, enemyBaseY;
    private double range;
    private int damage;
    private long lastShotTime;
    private long fireRate;
    private int healthPoints;

    private Projectile currentProjectile; // Активен снаряд
    private List<ExplosionEffect> explosions; // Ефекти на експлозии

    public Artillery(int baseX, int baseY, int enemyBaseX, int enemyBaseY, String team, ScoutGame game) {
        super(baseX, baseY, team, "artillery");
        this.game = game;
        this.enemyBaseX = enemyBaseX;
        this.enemyBaseY = enemyBaseY;

        double angleToEnemyBase = Math.toDegrees(Math.atan2(enemyBaseY - baseY, enemyBaseX - baseX));
        this.x = baseX;
        this.y = baseY;
        this.currentAngle = angleToEnemyBase;

        this.range = 700.0;
        this.damage = 20; // Увеличена щета
        this.fireRate = 3000; // Интервал между изстрелите (3 секунди)
        this.lastShotTime = System.currentTimeMillis();

        this.healthPoints = 100;
        this.explosions = new ArrayList<>();
    }

    public void drawArtillery(Graphics2D g2d) {
        // Рисуване на артилерията
        int bodyRadius = 10;
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int) (x - bodyRadius), (int) (y - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        // Рисуване на цевта
        int lineLength = 40;
        int x2 = (int) (x + lineLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = (int) (y + lineLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.setColor(Color.RED);
        g2d.drawLine((int) x, (int) y, x2, y2);

        // Рисуване на снаряда
        if (currentProjectile != null) {
            currentProjectile.draw(g2d);
        }

        // Рисуване на експлозиите
        for (ExplosionEffect explosion : explosions) {
            explosion.draw(g2d);
        }
    }

    public void updateArtillery() {
        if (!isActive()) return;

        // Обновяване на снаряда
        if (currentProjectile != null) {
            currentProjectile.update();
            if (currentProjectile.hasReachedTarget()) {
                createExplosion(currentProjectile.getTargetX(), currentProjectile.getTargetY());
                currentProjectile = null;
            }
        }

        // Обновяване на експлозиите
        explosions.removeIf(ExplosionEffect::isExpired);

        // Проверка дали можем да стреляме
        if (canShoot()) {
            fireProjectile();
        }
    }

    private void fireProjectile() {
        currentProjectile = new Projectile(x, y, enemyBaseX, enemyBaseY);
    }

    private void createExplosion(double targetX, double targetY) {
        explosions.add(new ExplosionEffect(targetX, targetY, 30, Color.RED, 3000));
    }

    private boolean canShoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime >= fireRate) {
            lastShotTime = currentTime;
            return true;
        }
        return false;
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
        return "artillery";
    }

    // Вътрешен клас за снаряд
    private class Projectile {
        private double x, y;
        private final double targetX, targetY;
        private final double speed = 8.0;

        public Projectile(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public void update() {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= speed) {
                x = targetX;
                y = targetY;
            } else {
                x += (dx / distance) * speed;
                y += (dy / distance) * speed;
            }
        }

        public boolean hasReachedTarget() {
            return x == targetX && y == targetY;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval((int) x - 5, (int) y - 5, 10, 10);
        }

        public double getTargetX() {
            return targetX;
        }

        public double getTargetY() {
            return targetY;
        }
    }

    // Вътрешен клас за експлозия
    private class ExplosionEffect {
        private final double x, y;
        private final int radius;
        private Color color;
        private final long expirationTime;
        private final long duration;

        public ExplosionEffect(double x, double y, int radius, Color color, long duration) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.duration = duration;
            this.expirationTime = System.currentTimeMillis() + duration;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public void draw(Graphics2D g2d) {
            long timeLeft = expirationTime - System.currentTimeMillis();
            if (timeLeft > 0) {
                int alpha = (int) (255 * timeLeft / duration);
                Color fadingColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                g2d.setColor(fadingColor);
                g2d.fillOval((int) x - radius, (int) y - radius, radius * 2, radius * 2);
            }
        }
    }
}
