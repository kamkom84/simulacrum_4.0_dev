package classesSeparated;

import java.awt.*;
import java.awt.geom.Point2D; // <<< NEW
import java.util.ArrayList;
import java.util.Iterator;

public class Defender extends Character {
    private double speed = 0.03;
    private double angleOffset;
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private static final int SHOOT_RANGE = 450;/////////////////////////////////////////////////////////////////////////
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
        this.healthPoints = 100;////////////////////////////////////////////////////////////////////////////////////////

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
                scout.decreaseHealth(1);
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
            projectile.updateProjectilePosition();

            boolean hit = false;
            for (Soldier soldier : soldiers) {
                if (soldier != null && soldier.isActive() && projectile.hasProjectileHit(soldier)) {
                    soldier.decreaseHealth(1);
                    soldier.moveBackFrom((int) this.x, (int) this.y);
                    hit = true;
                    break;
                }
            }

            if (hit || !projectile.isProjectileActive()) {
                iterator.remove();
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
                    this.currentAngle = Math.atan2(soldier.getY() - this.y, soldier.getX() - this.x);
                    defenderShootAtSoldier(soldier);
                }
            }
        }
    }

    private void defenderShootAtSoldier(Soldier soldier) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < SHOOT_INTERVAL) {
            return;
        }

        lastShotTime = currentTime;

        double angleToTarget = Math.atan2(soldier.getY() - this.y, soldier.getX() - this.x);

        Projectile projectile = new Projectile(
                this.x,
                this.y,
                soldier.getX(),
                soldier.getY(),
                45.0, ///////////////////////////////////
                450.0 // Максимален обхват на патрона//////////////////////////////////////////////////////////////////
        );
        projectiles.add(projectile);

        game.drawShot(
                (int) this.x,
                (int) this.y,
                (int) (this.x + 15 * Math.cos(angleToTarget)),
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
        return 15;
    }

    public int getHealthPoints() {
        return this.healthPoints;
    }

    public void checkAndShootIfArtilleryProjectileInRange(Artillery artillery) {
        if (artillery == null || !artillery.isActive() || !artillery.hasActiveProjectile()) return;
        if (this.team.equalsIgnoreCase(artillery.getTeam())) return;

        Point2D.Double p = artillery.getProjectilePosition();
        if (p == null) return;

        double dist = Point2D.distance(this.x, this.y, p.x, p.y);
        if (dist > SHOOT_RANGE) return;

        // насочи оръжието и пусни куршум към точката
        double ang = Math.atan2(p.y - this.y, p.x - this.x);
        currentAngle = ang;

        long now = System.currentTimeMillis();
        if (now - lastShotTime < SHOOT_INTERVAL) return;
        lastShotTime = now;

        Projectile bullet = new Projectile(
                this.x,
                this.y,
                p.x,
                p.y,
                45.0,
                450.0
        );
        projectiles.add(bullet);

        game.drawShot(
                (int) this.x,
                (int) this.y,
                (int) (this.x + 15 * Math.cos(ang)),
                (int) (this.y + 15 * Math.sin(ang))
        );
    }

    public void updateProjectilesForArtillery(Artillery artillery) {
        if (artillery == null || !artillery.isActive() || !artillery.hasActiveProjectile()) {
            // все пак местим куршумите и ги чистим ако са извън обхват
            Iterator<Projectile> it = projectiles.iterator();
            while (it.hasNext()) {
                Projectile b = it.next();
                b.updateProjectilePosition();
                if (!b.isProjectileActive()) it.remove();
            }
            return;
        }

        Point2D.Double ap = artillery.getProjectilePosition();
        if (ap == null) return;

        final double HIT_RADIUS = 8.0; // колко близо куршумът трябва да мине до снаряда

        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile b = it.next();
            b.updateProjectilePosition();

            // ако имаш getters getX()/getY() – ползвай ги; иначе адаптирай според твоя клас Projectile
            double bx = b.getX();
            double by = b.getY();

            if (Point2D.distance(bx, by, ap.x, ap.y) <= HIT_RADIUS) {
                // унищожи снаряда + ефект
                artillery.destroyProjectileWithPop();
                game.addExplosionEffect(ap.x, ap.y, 22, new Color(255, 160, 60), 400);
                // премахни и нашия куршум
                it.remove();
                // няма повече какво да уцелим този тик
                break;
            }

            if (!b.isProjectileActive()) it.remove();
        }
    }
}
