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

    private ArtilleryProjectile currentProjectile; // Активен снаряд
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
        this.damage = 20; ////////////////////////////////////////////////////////////////////// Увеличена щета
        this.fireRate = 3000; /////////////////////////////////////////////////////////////////// Интервал между изстрелите (3 секунди)
        this.lastShotTime = System.currentTimeMillis();

        this.healthPoints = 100;
        this.explosions = new ArrayList<>();

        double distanceFromBase = 500.0; // Разстояние на артилерията от базата
        this.x = baseX + distanceFromBase * Math.cos(Math.toRadians(angleToEnemyBase));
        this.y = baseY + distanceFromBase * Math.sin(Math.toRadians(angleToEnemyBase));
    }

    public void drawArtillery(Graphics2D g2d) {
        int bodyRadius = 10;
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int) (x - bodyRadius), (int) (y - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        int lineLength = 20;
        int x2 = (int) (x + lineLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = (int) (y + lineLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.setColor(Color.RED);
        g2d.drawLine((int) x, (int) y, x2, y2);

        if (currentProjectile != null) {
            currentProjectile.drawArtilleryProjectile(g2d);
        }

        for (ExplosionEffect explosion : explosions) {
            explosion.draw(g2d);
        }
    }

    public void updateArtillery() {
        if (!isActive()) return;

        if (currentProjectile != null) {
            currentProjectile.update();
            if (currentProjectile.hasReachedTarget()) {
                createExplosion(currentProjectile.getTargetX(), currentProjectile.getTargetY());
                currentProjectile = null;
            }
        }

        explosions.removeIf(ExplosionEffect::isExpired);

        if (currentProjectile == null && canShoot()) {
            fireProjectile();
        }
    }

    private void fireProjectile() {
        if (currentProjectile == null) {
            double shieldRadius = game.getBaseShieldRadius(); // Радиусът на щита
            Point shieldEdge = calculateShieldEdge(enemyBaseX, enemyBaseY, this.x, this.y, shieldRadius);
            currentProjectile = new ArtilleryProjectile(this.x, this.y, shieldEdge.getX(), shieldEdge.getY());
        }
    }

    private void createExplosion(double targetX, double targetY) {
        explosions.add(new ExplosionEffect(targetX, targetY, 15, Color.RED, 1000));
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

    private class ArtilleryProjectile {
        private double x, y;
        private final double targetX, targetY;
        private final double speed = 30.0; ///////////////////////////////////////////////////////////////////////

        public ArtilleryProjectile(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;

            // Изчисляваме вектора към целевата точка
            double dx = targetX - startX;
            double dy = targetY - startY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Ако разстоянието е валидно, задаваме целевите координати
            if (distance > 0) {
                this.targetX = targetX;
                this.targetY = targetY;
            } else {
                // Ако разстоянието е нула (рядко се случва), задаваме целта на стартовите координати
                this.targetX = startX;
                this.targetY = startY;
            }
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
            return Math.hypot(x - targetX, y - targetY) <= speed;
        }

        public void drawArtilleryProjectile(Graphics2D g2d) {
            g2d.setColor(Color.RED);

            // Определете дължината на "патрона" (линията)
            int lineLength = 10; // Можете да промените тази стойност

            // Изчислете ъгъла на движение
            double angle = Math.atan2(targetY - y, targetX - x);

            int endX = (int) (x + lineLength * Math.cos(angle));
            int endY = (int) (y + lineLength * Math.sin(angle));

            g2d.drawLine((int) x, (int) y, endX, endY);
        }


        public double getTargetX() {
            return targetX;
        }

        public double getTargetY() {
            return targetY;
        }
    }

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

    private Point calculateShieldEdge(double baseX, double baseY, double startX, double startY, double radius) {
        double angle = Math.atan2(baseY - startY, baseX - startX);
        double edgeX = baseX - radius * Math.cos(angle);
        double edgeY = baseY - radius * Math.sin(angle);
        return new Point((int) edgeX, (int) edgeY);
    }

}
