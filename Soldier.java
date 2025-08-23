package classesSeparated;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

public class Soldier extends Character {
    // ---------- настройки ----------
    private static final int    BODY_RADIUS       = 5;
    private static final int    WEAPON_LENGTH     = 15;

    private static final double VISUAL_RANGE      = 250.0;  // „виждам“ и преследвам
    private static final double SHOOT_RANGE       = 120.0;  // стрелям
    private static final double MOVE_SPEED        = 1.8;

    private static final long   COOLDOWN_BASE_MS  = 900;
    private static final long   COOLDOWN_JITTER_MS= 350;

    // куршум – права линия, видим до 150px
    private static final double BULLET_SPEED      = 35.0;
    private static final double BULLET_MAX_TRAVEL = 150.0;
    private static final double BULLET_HIT_RADIUS = 6.0;

    // фланг – геометрия (ново: фиксирана дълбочина 250 px под врага)
    private static final double FLANK_DEPTH_BELOW_ENEMY = 250.0;
    private static final double FLANK_SAFE_MARGIN = 40.0; // оставена за евентуални проверки

    // граната – шрапнели
    private static final int    GRENADE_SHRAPNELS = 12;
    private static final double SHRAPNEL_SPEED    = 2.4;
    private static final float  SHRAPNEL_FADE     = 0.12f;
    private static final int    SHRAPNEL_SIZE     = 3;

    // граници на екрана
    private static final double SCREEN_MARGIN = BODY_RADIUS + 2;

    // бойна лента по Y (армията не се разтяга нагоре/надолу)
    private static final double LEASH_Y = 90.0;
    private double anchorY; // редът на войника (може да се префиксира след подреждане)

    // ---------- състояние ----------
    private final Color teamColor;
    private final ScoutGame game;
    private final int id;
    private int healthPoints;

    private final int enemyBaseX, enemyBaseY;
    private final int baseX, baseY;

    private boolean waiting = false;

    private Projectile currentProjectile;
    private long lastShotTime = 0;
    private final long shootCooldownMs;

    private boolean showHealth = false;
    private String speech = null;
    private long speechEndTime = 0;

    // гранати по прагове 20→15→10→5
    private Grenade currentGrenade = null;
    private int nextGrenadeThreshold = 20;

    // (стар флаг)
    private boolean isFlanking = false;

    // --------- СЪВМЕСТИМОСТ + реален фланг ----------
    private boolean compatFlankingMode = false; // включено от играта
    private boolean compatFlankLeader  = false;
    private Soldier compatFlankTarget  = null;
    private Soldier flankLeaderRef     = null;  // съвместимост

    // маршрут за фланга (видимо движение)
    private final java.util.List<Point2D.Double> flankPath = new ArrayList<>();
    private int   flankIdx = 0;
    private boolean flankAttacking = false; // преминали етапа „под тях“

    public Soldier(int x, int y, String team,
                   int baseX, int baseY, int enemyBaseX, int enemyBaseY,
                   ScoutGame game, int id) {
        super(x, y, team, "soldier");
        this.healthPoints = 20;/////////////////////////////////////////////////////////////////////////////////////////
        this.teamColor = team.equals("blue") ? Color.BLUE : Color.RED;
        this.game = game;
        this.id = id;
        this.baseX = baseX;  this.baseY = baseY;
        this.enemyBaseX = enemyBaseX; this.enemyBaseY = enemyBaseY;

        this.currentAngle = Math.toDegrees(Math.atan2(game.getHeight()/2.0 - y, game.getWidth()/2.0 - x));

        this.shootCooldownMs = COOLDOWN_BASE_MS + (long)(Math.random() * COOLDOWN_JITTER_MS);
        this.lastShotTime = System.currentTimeMillis() - (long)(Math.random() * shootCooldownMs);

        // фиксирай реда на войника – ако по-късно го подредиш наново, извикай setAnchorY(...)
        this.anchorY = y;
    }

    // ---------- рисуване ----------
    public void drawSoldier(Graphics2D g2d) {
        g2d.setColor(teamColor);
        g2d.fillOval((int)(x - BODY_RADIUS), (int)(y - BODY_RADIUS), BODY_RADIUS * 2, BODY_RADIUS * 2);

        g2d.setColor(Color.YELLOW);
        int x1 = (int)x, y1 = (int)y;
        int x2 = x1 + (int)(WEAPON_LENGTH * Math.cos(Math.toRadians(currentAngle)));
        int y2 = y1 + (int)(WEAPON_LENGTH * Math.sin(Math.toRadians(currentAngle)));
        g2d.drawLine(x1, y1, x2, y2);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        g2d.drawString(String.valueOf(id), (int)x - 6, (int)y - BODY_RADIUS - 1);

        if (showHealth) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(String.valueOf(healthPoints), (int)x - 8, (int)y - BODY_RADIUS - 9);
        }

        if (currentProjectile != null) currentProjectile.draw(g2d);
        if (currentGrenade != null)   currentGrenade.drawGrenade(g2d);

        if (speech != null && System.currentTimeMillis() < speechEndTime) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(speech, (int)x - 18, (int)y - 20);
        } else {
            speech = null;
        }
    }

    // ---------- основна логика ----------
    public void updateSoldier(Soldier[] teammates, Soldier[] enemyArmy) {
        if (waiting || !isActive()) return;

        if (currentProjectile != null && currentProjectile.isActive()) {
            currentProjectile.updateSoldierProjectilePosition();
        }
        if (currentGrenade != null) {
            currentGrenade.update();
            if (currentGrenade.isExploded()) currentGrenade = null;
        }

        if (compatFlankingMode) {
            runFlank(teammates, enemyArmy);
            return;
        }

        Character target = findTargetInVisualRange();
        if (target != null) {
            double d = Point2D.distance(this.x, this.y, target.getX(), target.getY());
            tryThrowGrenadeAtThreshold(target);

            if (d > SHOOT_RANGE) {
                moveTowards(target.getX(), target.getY(), MOVE_SPEED);
                maintainDistanceFromTeammates(teammates);
            } else {
                soldierShoot(target);
            }
        } else {
            soldierAdvance(teammates);
        }
    }

    // ---------- реален фланг ----------
    private static class BBox {
        double minX =  1e9, minY =  1e9, maxX = -1e9, maxY = -1e9; boolean any=false;
        void add(Soldier s){ any=true;
            double X=s.getX(),Y=s.getY();
            if(X<minX)minX=X; if(X>maxX)maxX=X; if(Y<minY)minY=Y; if(Y>maxY)maxY=Y; }
        double centerX(){return (minX+maxX)/2.0;}
    }
    private BBox calcEnemyBox(Soldier[] enemyArmy){
        BBox b=new BBox(); if(enemyArmy!=null)
            for(Soldier s:enemyArmy) if(s!=null&&s.isActive()) b.add(s);
        return b;
    }

    private void planFlankPathIfNeeded(Soldier[] enemyArmy){
        if (!flankPath.isEmpty() || flankAttacking) return;
        BBox b = calcEnemyBox(enemyArmy);
        if (!b.any) return;

        // Ново: винаги слизаме на линия 250 px под най-долния враг,
        // но никога под долния ръб на екрана.
        double safeY = Math.min(game.getHeight() - SCREEN_MARGIN,
                b.maxY + FLANK_DEPTH_BELOW_ENEMY);

        flankPath.clear();
        flankPath.add(clampPointToArena(this.x, safeY));          // 1) надолу
        flankPath.add(clampPointToArena(b.centerX(), safeY));     // 2) хоризонтално под тях
        flankIdx = 0;
        flankAttacking = false;
    }

    private void runFlank(Soldier[] teammates, Soldier[] enemyArmy){
        planFlankPathIfNeeded(enemyArmy);

        if (!flankAttacking && flankIdx < flankPath.size()){
            Point2D.Double wp = flankPath.get(flankIdx);
            moveTowards(wp.x, wp.y, MOVE_SPEED);
            if (Point2D.distance(this.x, this.y, wp.x, wp.y) < 10) flankIdx++;
            return;
        }

        flankAttacking = true;

        Soldier tgt = compatFlankTarget;
        if (tgt == null || !tgt.isActive()) {
            tgt = findBottomEnemy(enemyArmy);
            compatFlankTarget = tgt;
        }

        if (tgt != null) {
            moveTowards(tgt.getX(), tgt.getY(), MOVE_SPEED);
            if (Point2D.distance(this.x, this.y, tgt.getX(), tgt.getY()) <= SHOOT_RANGE) {
                soldierShoot(tgt);
            }
        } else {
            compatFlankingMode = false;
            flankPath.clear(); flankIdx = 0; flankAttacking = false;
        }
    }

    private Soldier findBottomEnemy(Soldier[] enemyArmy){
        Soldier bot=null; if(enemyArmy==null) return null;
        for(Soldier s:enemyArmy){
            if(s!=null && s.isActive()){
                if(bot==null || s.getY()>bot.getY()) bot=s;
            }
        }
        return bot;
    }

    // ---------- стрелба ----------
    public void soldierShoot(Character target) {
        if (target == null || !target.isActive()) return;
        long now = System.currentTimeMillis();
        if (now - lastShotTime < shootCooldownMs) return;

        double dist = Point2D.distance(this.x, this.y, target.getX(), target.getY());
        if (dist <= SHOOT_RANGE) {
            double angDeg = Math.toDegrees(Math.atan2(target.getY() - this.y, target.getX() - this.x));
            this.currentAngle = angDeg;
            currentProjectile = new Projectile(
                    x + WEAPON_LENGTH * Math.cos(Math.toRadians(angDeg)),
                    y + WEAPON_LENGTH * Math.sin(Math.toRadians(angDeg)),
                    angDeg
            );
            lastShotTime = now;
        }
    }

    // (съвместимост със ScoutGame – няма нужда от target)
    public void updateProjectile(Character ignored) {
        if (currentProjectile != null && currentProjectile.isActive()) {
            currentProjectile.updateSoldierProjectilePosition();
        }
    }

    // ---------- помощници ----------
    private void moveTowards(double tx, double ty, double speed) {
        double ang = Math.atan2(ty - this.y, tx - this.x);
        this.currentAngle = Math.toDegrees(ang);
        this.x += speed * Math.cos(ang);
        this.y += speed * Math.sin(ang);
        clampToArena();
        applyLeashY(); // държи „стената“ по Y
    }

    private void soldierAdvance(Soldier[] teammates) {
        // настъпване основно по Х към врага; по Y стоим на собствения ред
        double tx = enemyBaseX;
        double ty = this.anchorY;
        moveTowards(tx, ty, MOVE_SPEED);
        maintainDistanceFromTeammates(teammates);
    }

    private void maintainDistanceFromTeammates(Soldier[] teammates) {
        double minDistance = 50.0;
        for (Soldier t : teammates) {
            if (t == null || t == this) continue;
            double d = Point2D.distance(this.x, this.y, t.getX(), t.getY());
            if (d < minDistance) {
                double angleAway = Math.toDegrees(Math.atan2(this.y - t.getY(), this.x - t.getX()));
                this.x += 1.0 * Math.cos(Math.toRadians(angleAway));
                this.y += 1.0 * Math.sin(Math.toRadians(angleAway));
                clampToArena();
                applyLeashY();
            }
        }
    }

    private void applyLeashY() {
        if (compatFlankingMode) return; // при фланг няма ограничение
        double minY = anchorY - LEASH_Y;
        double maxY = anchorY + LEASH_Y;
        if (y < minY) y = minY;
        if (y > maxY) y = maxY;
    }

    private Character findTargetInVisualRange() {
        Character best = null;
        double bestDist = VISUAL_RANGE;
        for (Character c : game.getCharacters()) {
            if (c == null || !c.isActive()) continue;
            if (this.team.equals(c.getTeam())) continue;
            double d = Point2D.distance(this.x, this.y, c.getX(), c.getY());
            if (d <= bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    private Character findClosestEnemyAnywhere() {
        Character best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Character c : game.getCharacters()) {
            if (c == null || !c.isActive()) continue;
            if (this.team.equals(c.getTeam())) continue;
            double d = Point2D.distance(this.x, this.y, c.getX(), c.getY());
            if (d < bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    private void tryThrowGrenadeAtThreshold(Character target) {
        if (currentGrenade != null) return;
        if (nextGrenadeThreshold < 5) return;
        if (healthPoints <= nextGrenadeThreshold) {
            currentGrenade = new Grenade(this.x, this.y, target.getX(), target.getY());
            nextGrenadeThreshold -= 5; // 20→15→10→5
        }
    }

    private void clampToArena() {
        double w = game.getWidth(), h = game.getHeight();
        if (x < SCREEN_MARGIN) x = SCREEN_MARGIN;
        if (x > w - SCREEN_MARGIN) x = w - SCREEN_MARGIN;
        if (y < SCREEN_MARGIN) y = SCREEN_MARGIN;
        if (y > h - SCREEN_MARGIN) y = h - SCREEN_MARGIN;
    }

    private Point2D.Double clampPointToArena(double px, double py) {
        double w = game.getWidth(), h = game.getHeight();
        px = Math.max(SCREEN_MARGIN, Math.min(w - SCREEN_MARGIN, px));
        py = Math.max(SCREEN_MARGIN, Math.min(h - SCREEN_MARGIN, py));
        return new Point2D.Double(px, py);
    }

    public void say(String text, int durationMs) {
        this.speech = text;
        this.speechEndTime = System.currentTimeMillis() + durationMs;
    }

    private void showHealthTemporarily() {
        showHealth = true;
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() { showHealth = false; }
        }, 1000);
    }

    @Override
    public void takeDamage(int dmg) {
        this.healthPoints -= dmg;
        if (this.healthPoints <= 0) {
            this.healthPoints = 0;
            this.setActive(false);
            return;
        }
        showHealthTemporarily();

        double moveAngle = Math.toRadians(currentAngle + 180);
        final int MOVE_BACK_DISTANCE = 120;
        this.x += MOVE_BACK_DISTANCE * Math.cos(moveAngle);
        this.y += MOVE_BACK_DISTANCE * Math.sin(moveAngle);
        clampToArena();
        applyLeashY();
    }

    // ---------- публични дреболии ----------
    @Override public String getType() { return "Soldier"; }
    public int  getId() { return id; }
    public int  getHealthPoints() { return healthPoints; }
    public void setWaiting(boolean waiting) { this.waiting = waiting; }
    public boolean isWaiting() { return waiting; }
    public int  getWeaponLength() { return WEAPON_LENGTH; }
    public void setFlanking(boolean v) { this.isFlanking = v; }

    // --- СЪВМЕСТИМОСТ със старите извиквания от ScoutGame ---
    public boolean getFlankingMode() { return compatFlankingMode; }
    public void setFlankingMode(boolean v) {
        compatFlankingMode = v;
        if (!v) { flankPath.clear(); flankIdx = 0; flankAttacking = false; }
    }
    public void setFlankLeader(boolean v) { compatFlankLeader = v; }
    public void setFlankTarget(Soldier s) { compatFlankTarget = s; }
    public Soldier getFlankTarget() { return compatFlankTarget; }
    public void setFlankLeaderRef(Soldier leader) { this.flankLeaderRef = leader; }
    public int getFlankPhase() { return flankAttacking ? 3 : (flankPath.isEmpty() ? 0 : 1); }

    // Позволява ти да заключиш реда повторно, ако подреждаш след конструктора
    public void setAnchorY(double newY) { this.anchorY = newY; }
    public double getAnchorY() { return this.anchorY; }

    public void moveBackFrom(int x, int y) {
        double ang = Math.atan2(this.y - y, this.x - x);
        int offset = 100;
        this.x += offset * Math.cos(ang);
        this.y += offset * Math.sin(ang);
        clampToArena();
        applyLeashY();
    }

    // ---------- вътрешни класове ----------
    private class Projectile {
        private double x, y;
        private final double dirDeg;
        private boolean active = true;
        private double traveled = 0.0;

        Projectile(double startX, double startY, double directionDeg) {
            this.x = startX; this.y = startY; this.dirDeg = directionDeg;
        }

        void updateSoldierProjectilePosition() {
            if (!active) return;

            double rad = Math.toRadians(dirDeg);
            double dx = BULLET_SPEED * Math.cos(rad);
            double dy = BULLET_SPEED * Math.sin(rad);

            x += dx; y += dy;
            traveled += Math.hypot(dx, dy);

            if (x < 0 || x > game.getWidth() || y < 0 || y > game.getHeight()) {
                active = false; return;
            }

            for (Character c : game.getCharacters()) {
                if (!c.isActive() || c.getTeam().equals(Soldier.this.team)) continue;
                if (Point2D.distance(x, y, c.getX(), c.getY()) <= BULLET_HIT_RADIUS) {
                    c.takeDamage(1);
                    active = false;
                    break;
                }
            }
            if (traveled >= BULLET_MAX_TRAVEL) active = false;
        }

        boolean isActive() { return active; }

        void draw(Graphics2D g2d) {
            if (!active) return;
            double rad = Math.toRadians(dirDeg);
            double sx = x - 3 * Math.cos(rad);
            double sy = y - 3 * Math.sin(rad);
            double ex = x + 3 * Math.cos(rad);
            double ey = y + 3 * Math.sin(rad);
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawLine((int)sx, (int)sy, (int)ex, (int)ey);
        }
    }

    private class Grenade {
        private double x, y;
        private final double targetX, targetY;
        private final double speed = 12.0;
        private int countdown = 5;
        private boolean exploded = false;

        private class Shard {
            double x, y, dx, dy; float alpha = 1.0f;
            Shard(double x,double y,double dx,double dy){this.x=x;this.y=y;this.dx=dx;this.dy=dy;}
            void update(){
                x+=dx; y+=dy;
                if (x < 0 || x > game.getWidth() || y < 0 || y > game.getHeight()) { alpha = 0; return; }
                alpha-=SHRAPNEL_FADE; if(alpha<0) alpha=0;
            }
            void draw(Graphics2D g2d){
                if(alpha<=0) return;
                Composite oc = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(Color.RED);
                g2d.fillOval((int)x,(int)y,SHRAPNEL_SIZE,SHRAPNEL_SIZE);
                g2d.setComposite(oc);
            }
            boolean dead(){ return alpha<=0; }
        }
        private final java.util.List<Shard> shards = new ArrayList<>();

        Grenade(double sx, double sy, double tx, double ty) {
            this.x=sx; this.y=sy; this.targetX=tx; this.targetY=ty;
        }

        void update() {
            if (exploded) {
                for (Shard s : shards) s.update();
                shards.removeIf(Shard::dead);
                return;
            }

            double ang = Math.atan2(targetY - y, targetX - x);
            x += speed * Math.cos(ang);
            y += speed * Math.sin(ang);

            Point2D.Double p = clampPointToArena(x, y);
            x = p.x; y = p.y;

            if (Point2D.distance(x, y, targetX, targetY) < speed) {
                countdown--;
                if (countdown <= 0) explode();
            }
        }

        private void explode() {
            exploded = true;

            for (Character c : game.getCharacters()) {
                if (!c.isActive() || c.getTeam().equals(Soldier.this.team)) continue;
                double d = Point2D.distance(x, y, c.getX(), c.getY());
                if (d <= 50) {
                    c.takeDamage(5);
                    double ang = Math.atan2(c.getY() - y, c.getX() - x);
                    c.setX(c.getX() + 50 * Math.cos(ang));
                    c.setY(c.getY() + 50 * Math.sin(ang));
                }
            }

            for (int i = 0; i < GRENADE_SHRAPNELS; i++) {
                double ang = 2 * Math.PI * i / GRENADE_SHRAPNELS;
                double dx = SHRAPNEL_SPEED * Math.cos(ang) + (Math.random() - 0.5) * 0.6;
                double dy = SHRAPNEL_SPEED * Math.sin(ang) + (Math.random() - 0.5) * 0.6;
                shards.add(new Shard(x, y, dx, dy));
            }
        }

        void drawGrenade(Graphics2D g2d) {
            if (!exploded) {
                g2d.setColor(Color.RED);
                g2d.fillOval((int)x - 4, (int)y - 4, 5, 5);
            } else {
                for (Shard s : shards) s.draw(g2d);
            }
        }

        boolean isExploded() { return exploded && shards.isEmpty(); }

    }
}
