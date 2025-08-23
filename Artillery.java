package classesSeparated;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Artillery extends Character {
    private final ScoutGame game;
    private final int enemyBaseX, enemyBaseY;

    // баланс / логика
    private final double range = 700.0;
    private final int damage = 20;
    private long lastShotTime;
    private final long fireRate = 3000;
    private int healthPoints = 100;

    private ArtilleryProjectile currentProjectile;
    private final List<ExplosionEffect> explosions = new ArrayList<>();

    // шрапнели (визуален взрив)
    private static final int    FRAG_COUNT = 12;
    private static final double FRAG_SPEED = 6.0;
    private static final float  FRAG_FADE  = 0.06f;
    private static final int    FRAG_SIZE  = 3;
    private final List<Frag> frags = new ArrayList<>();

    // визия и анимация
    private static final int    WHEEL_RADIUS   = 10;
    private static final int    WHEEL_SPOKES   = 6;
    private static final double BARREL_LENGTH  = 42.0;
    private static final double BARREL_RADIUS  = 6.0;

    // стабилен откат без „пълзене“
    private double recoil = 0.0;                 // текущо изтегляне на цевта назад (px)
    private static final double RECOIL_KICK    = 8.0;   // импулс при изстрел
    private static final double RECOIL_RECOVER = 1.2;   // връщане към 0 на тик (~150ms)
    private long muzzleFlashUntil = 0;
    private double wheelAngle = 0;

    private final List<Smoke> smoke = new ArrayList<>();

    // нови константи
    private static final double DIRECT_HIT_RADIUS = 6.0;   // малък хитбокс за защитник
    private static final int    BASE_BOOM_RADIUS  = 140;   // визуален радиус на големия взрив

    public Artillery(int baseX, int baseY, int enemyBaseX, int enemyBaseY, String team, ScoutGame game) {
        super(baseX, baseY, team, "artillery");
        this.game = game;
        this.enemyBaseX = enemyBaseX;
        this.enemyBaseY = enemyBaseY;

        double angleToEnemyBase = Math.toDegrees(Math.atan2(enemyBaseY - baseY, enemyBaseX - baseX));
        this.currentAngle = angleToEnemyBase;

        // изнасяне на оръдието напред от базата
        double distanceFromBase = 500.0;
        this.x = baseX + distanceFromBase * Math.cos(Math.toRadians(angleToEnemyBase));
        this.y = baseY + distanceFromBase * Math.sin(Math.toRadians(angleToEnemyBase));

        this.lastShotTime = System.currentTimeMillis();
    }

    // -------- рисуване --------
    public void drawArtillery(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // пръстен за обсег
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
        g.setColor(team.equals("blue") ? Color.CYAN : Color.PINK);
        g.drawOval((int)(x - range), (int)(y - range), (int)(range*2), (int)(range*2));
        g.setComposite(AlphaComposite.SrcOver);

        // локална трансформация за тяло + дуло (винаги на едно място)
        double angRad = Math.toRadians(currentAngle);
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angRad);

        // лафет
        int baseW = 36, baseH = 16;
        Rectangle baseRect = new Rectangle(-baseW/2, -baseH/2, baseW, baseH);
        Paint bodyPaint = new GradientPaint(-baseW/2f, 0, new Color(45,45,45), baseW/2f, 0, new Color(25,25,25));
        g.setPaint(bodyPaint);
        g.fill(baseRect);
        g.setColor(new Color(0,0,0,120));
        g.draw(baseRect);

        // щит на лафета
        Polygon shield = new Polygon();
        shield.addPoint(0, -baseH/2 - 2);
        shield.addPoint(0,  baseH/2 + 2);
        shield.addPoint(18, 0);
        g.setPaint(new GradientPaint(0, -10, new Color(90,90,90), 18, 0, new Color(60,60,60)));
        g.fill(shield);
        g.setColor(new Color(0,0,0,120));
        g.draw(shield);

        // дуло – от (-recoil, 0) до (BARREL_LENGTH - recoil, 0)
        int sX = (int)Math.round(-recoil);
        int tX = (int)Math.round(BARREL_LENGTH - recoil);
        g.setStroke(new BasicStroke((float)BARREL_RADIUS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(80,80,80));
        g.drawLine(sX, 0, tX, 0);
        g.setStroke(new BasicStroke((float)(BARREL_RADIUS-2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(160,160,160));
        g.drawLine(sX + 2, 1, tX + 2, 1);

        // муцунен пламък
        if (System.currentTimeMillis() < muzzleFlashUntil) {
            Polygon flash = new Polygon();
            flash.addPoint(tX, 0);
            flash.addPoint(tX + 10,  4);
            flash.addPoint(tX + 16,  0);
            flash.addPoint(tX + 10, -4);
            g.setColor(new Color(255, 240, 120, 230));
            g.fill(flash);
            g.setColor(new Color(255, 160, 40, 220));
            g.draw(flash);
        }

        // връщаме глобалната трансформация преди колелата/частиците
        g.setTransform(old);

        // колела (в глобални координати)
        double sin = Math.sin(angRad), cos = Math.cos(angRad);
        drawWheel(g, x - 12 * sin, y + 12 * cos, wheelAngle);
        drawWheel(g, x + 12 * sin, y - 12 * cos, wheelAngle);

        // снаряд/трайлър
        if (currentProjectile != null) currentProjectile.drawArtilleryProjectile(g);

        // ефекти
        for (ExplosionEffect ex : explosions) ex.drawExplosion(g);
        for (Frag f : frags) f.draw(g);
        for (Smoke s : smoke) s.draw(g);

        g.dispose();
    }

    private void drawWheel(Graphics2D g, double cx, double cy, double spokesRot) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gg.translate(cx, cy);

        gg.setColor(new Color(30,30,30));
        gg.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2);
        gg.setColor(Color.DARK_GRAY);
        gg.drawOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2);

        gg.setColor(new Color(70,70,70));
        gg.fillOval(-4, -4, 8, 8);

        gg.rotate(spokesRot);
        gg.setStroke(new BasicStroke(1.2f));
        gg.setColor(new Color(180,180,180));
        for (int i=0;i<WHEEL_SPOKES;i++){
            double a = 2*Math.PI*i/WHEEL_SPOKES;
            int x2 = (int)(Math.cos(a)*(WHEEL_RADIUS-2));
            int y2 = (int)(Math.sin(a)*(WHEEL_RADIUS-2));
            gg.drawLine(0,0,x2,y2);
        }
        gg.dispose();
    }

    // -------- ъпдейт --------
    public void updateArtillery() {
        if (!isActive()) return;

        // плавно връщане на отката към 0 (без натрупване)
        double prev = recoil;
        if (recoil > 0) recoil = Math.max(0, recoil - RECOIL_RECOVER);
        wheelAngle += (prev - recoil) * 0.12; // въртене според промяната (малко)

        smoke.removeIf(Smoke::dead);
        for (Smoke s: smoke) s.update();

        // шрапнели
        for (int i = frags.size()-1; i>=0; i--) {
            Frag f = frags.get(i);
            f.update();
            if (f.dead()) frags.remove(i);
        }

        // снаряд
        if (currentProjectile != null) {
            currentProjectile.updateArtilleryProjectile();

            // 1) директен удар в защитник (първо!)
            for (Defender defender : game.getDefenders()) {
                if (defender == null) continue;
                double d = Math.hypot(currentProjectile.getX() - defender.getX(),
                        currentProjectile.getY() - defender.getY());
                if (defender.isActive() && d <= DIRECT_HIT_RADIUS) {
                    defender.reduceHealthPoints(5);
                    if (defender.getHealthPoints() <= 0) defender.setActive(false);
                    spawnFragmentExplosion(currentProjectile.getX(), currentProjectile.getY());
                    currentProjectile = null;
                    return;
                }
            }

            // 2) щит или база?
            String enemyTeam = this.getTeam().equals("red") ? "blue" : "red";
            double dBase = Math.hypot(currentProjectile.getX() - enemyBaseX,
                    currentProjectile.getY() - enemyBaseY);

            if (game.getBaseShieldPoints(enemyTeam) > 0) {
                // още има щит → чукaме щита
                if (dBase <= game.getBaseShieldRadius() + 2.0) {
                    game.reduceShieldPoints(enemyTeam, 5);
                    spawnFragmentExplosion(currentProjectile.getX(), currentProjectile.getY());
                    currentProjectile = null;
                    return;
                }
            } else {
                // щитът е паднал → 1 попадение в базата = разрушение
                if (dBase <= 40.0) {
                    destroyEnemyBase();
                    currentProjectile = null;
                    return;
                }
            }

            // 3) стигнал е целта
            if (currentProjectile.reachedTarget()) {
                boolean enemyShieldUp2 = game.getBaseShieldPoints(enemyTeam) > 0;
                if (enemyShieldUp2) {
                    spawnFragmentExplosion(currentProjectile.getX(), currentProjectile.getY());
                } else {
                    destroyEnemyBase();
                }
                currentProjectile = null;
                return;
            }
        }

        explosions.removeIf(ExplosionEffect::isExplosionExpired);

        boolean enemyShieldActive = game.getBaseShieldPoints(this.getTeam().equals("red") ? "blue" : "red") > 0;
        boolean defendersActive = Arrays.stream(game.getDefenders()).anyMatch(Defender::isActive);
        if (!enemyShieldActive && !defendersActive) { setActive(false); return; }

        if (currentProjectile == null && canShoot()) fireProjectile();
    }

    private void fireProjectile() {
        if (currentProjectile != null) return;

        // муцуна в глобални координати (с текущ откат)
        double ang = Math.toRadians(currentAngle);
        double sx = (x - recoil * Math.cos(ang)) + BARREL_LENGTH * Math.cos(ang);
        double sy = (y - recoil * Math.sin(ang)) + BARREL_LENGTH * Math.sin(ang);

        // ако щитът е паднал → целим центъра на базата, иначе ръба на щита
        String enemyTeam = this.getTeam().equals("red") ? "blue" : "red";
        if (game.getBaseShieldPoints(enemyTeam) <= 0) {
            currentProjectile = new ArtilleryProjectile(sx, sy, enemyBaseX, enemyBaseY);
        } else {
            double shieldRadius = game.getBaseShieldRadius();
            Point2D.Double edge = calculateShieldEdge(enemyBaseX, enemyBaseY, sx, sy, shieldRadius);
            currentProjectile = new ArtilleryProjectile(sx, sy, edge.x, edge.y);
        }

        // импулс, пламък, дим
        recoil = RECOIL_KICK;                         // задаваме откат, не скорост
        muzzleFlashUntil = System.currentTimeMillis() + 120;
        spawnSmokeBurst(sx, sy, ang);
    }

    private void spawnSmokeBurst(double sx, double sy, double ang) {
        for (int i=0;i<6;i++){
            double a = ang + (Math.random()-0.5)*0.6;
            double sp = 0.8 + Math.random()*0.8;
            smoke.add(new Smoke(sx, sy, Math.cos(a)*sp, Math.sin(a)*sp));
        }
    }

    private void spawnFragmentExplosion(double x, double y) {
        explosions.add(new ExplosionEffect(x, y, 18, new Color(255,120,60), 600));
        for (int i = 0; i < FRAG_COUNT; i++) {
            double ang = 2 * Math.PI * i / FRAG_COUNT + (Math.random() - 0.5) * 0.6;
            double sp  = FRAG_SPEED * (0.7 + Math.random() * 0.6);
            frags.add(new Frag(x, y, Math.cos(ang) * sp, Math.sin(ang) * sp));
        }
    }

    // Разрушаване на вражеската база: голям взрив + деактивиране на всички нейни защитници
    private void destroyEnemyBase() {
        game.addExplosionEffect(enemyBaseX, enemyBaseY, BASE_BOOM_RADIUS, new Color(255,110,40), 1200);
        for (int i = 0; i < 5; i++) {
            game.addExplosionEffect(enemyBaseX, enemyBaseY, 80 - i * 12, new Color(255,180,80), 900);
        }
        spawnFragmentExplosion(enemyBaseX, enemyBaseY);

        String enemyTeam = this.getTeam().equals("red") ? "blue" : "red";
        for (Defender d : game.getDefenders()) {
            if (d != null && enemyTeam.equalsIgnoreCase(d.getTeam())) {
                d.setActive(false);
            }
        }
        // Артилерията ще се самоизключи при следващия тик, когато няма нито щит, нито активни защитници.
    }

    private boolean canShoot() {
        long now = System.currentTimeMillis();
        if (now - lastShotTime >= fireRate) { lastShotTime = now; return true; }
        return false;
    }

    @Override public void takeDamage(int dmg) {
        healthPoints -= dmg;
        if (healthPoints <= 0) { healthPoints = 0; setActive(false); }
    }

    @Override public String getType() { return "artillery"; }



    // --- PUBLIC API за защитниците да виждат/удрят снаряда ---
    public boolean hasActiveProjectile() {
        return currentProjectile != null;
    }

    public Point2D.Double getProjectilePosition() {
        if (currentProjectile == null) return null;
        return new Point2D.Double(currentProjectile.getX(), currentProjectile.getY());
    }

    /** Унищожава текущия снаряд с „пух“ експлозия (използва се от защитниците при попадение). */
    public void destroyProjectileWithPop() {
        if (currentProjectile != null) {
            spawnFragmentExplosion(currentProjectile.getX(), currentProjectile.getY());
            currentProjectile = null;
        }
    }




    // -------- снаряд --------
    private class ArtilleryProjectile {
        private double x, y;
        private final double tx, ty;
        private final double speed = 30.0;

        ArtilleryProjectile(double sx, double sy, double tx, double ty) {
            this.x=sx; this.y=sy; this.tx=tx; this.ty=ty;
        }

        void updateArtilleryProjectile() {
            double dx = tx - x, dy = ty - y;
            double d = Math.sqrt(dx*dx + dy*dy);
            if (d <= speed) { x = tx; y = ty; }
            else { x += (dx/d)*speed; y += (dy/d)*speed; }
        }

        boolean reachedTarget() {
            return Math.hypot(tx - x, ty - y) <= 0.6 * speed;
        }

        void drawArtilleryProjectile(Graphics2D g) {
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(255, 200, 80));
            double a = Math.atan2(ty - y, tx - x);
            g.drawLine((int)x, (int)y,
                    (int)(x - 10*Math.cos(a)),
                    (int)(y - 10*Math.sin(a)));
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval((int)x-2, (int)y-2, 4, 4);
        }

        double getX(){ return x; }
        double getY(){ return y; }
    }

    // -------- експлозия „пух“ --------
    private static class ExplosionEffect {
        private final double x, y;
        private final int radius;
        private final Color color;
        private final long expirationTime;
        private final long duration;

        ExplosionEffect(double x, double y, int radius, Color color, long duration) {
            this.x=x; this.y=y; this.radius=radius; this.color=color; this.duration=duration;
            this.expirationTime = System.currentTimeMillis() + duration;
        }
        boolean isExplosionExpired() { return System.currentTimeMillis() > expirationTime; }
        void drawExplosion(Graphics2D g) {
            long left = expirationTime - System.currentTimeMillis();
            if (left <= 0) return;
            int alpha = (int)(255 * left / duration);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillOval((int)x - radius, (int)y - radius, radius*2, radius*2);
        }
    }

    // -------- шрапнел --------
    private class Frag {
        double x, y, dx, dy; float alpha = 1f; int size = FRAG_SIZE;
        Frag(double x,double y,double dx,double dy){this.x=x;this.y=y;this.dx=dx;this.dy=dy;}
        void update(){ x+=dx; y+=dy; alpha -= FRAG_FADE; }
        boolean dead(){ return alpha <= 0f; }
        void draw(Graphics2D g){
            if (dead()) return;
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(255, 170, 60));
            g.fillOval((int)(x - size/2.0), (int)(y - size/2.0), size, size);
            g.setComposite(oc);
        }
    }

    // -------- дим --------
    private static class Smoke {
        double x,y,dx,dy,r=3.0; float a=0.9f;
        Smoke(double x,double y,double dx,double dy){this.x=x;this.y=y;this.dx=dx;this.dy=dy;}
        void update(){ x+=dx; y+=dy; r+=0.15; a-=0.02f; }
        boolean dead(){ return a<=0f; }
        void draw(Graphics2D g){
            if (dead()) return;
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(new Color(180,180,180));
            g.fillOval((int)(x-r), (int)(y-r), (int)(2*r), (int)(2*r));
            g.setComposite(oc);
        }
    }

    // по ръба на щита – с двойна точност
    private Point2D.Double calculateShieldEdge(double baseX, double baseY, double startX, double startY, double radius) {
        double angle = Math.atan2(baseY - startY, baseX - startX);
        double edgeX = baseX - radius * Math.cos(angle);
        double edgeY = baseY - radius * Math.sin(angle);
        return new Point2D.Double(edgeX, edgeY);
    }
}
