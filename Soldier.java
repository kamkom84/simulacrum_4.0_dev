package classesSeparated;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Timer;
import java.util.TimerTask;

import static java.awt.geom.Point2D.distance;

public class Soldier extends Character {
    private final int weaponLength = 15;////////////////////////////////////////////////////////////////////////////////
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
    private Grenade grenade;
    private boolean hasThrownGrenade = false;
    private Grenade currentGrenade = null;
    private int previousHealthPoints = 0;
    private boolean isFlanking = false;
    private boolean flanking = false;
    private Soldier flankTarget;




    public Soldier(int x, int y, String team, int baseX, int baseY, int enemyBaseX, int enemyBaseY, ScoutGame game, int id) {
        super(x, y, team, "soldier");
        this.healthPoints = 20;/////////////////////////////////////////////////////////////////////////////////////////
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
        int soldierBodyRadius = 5;//////////////////////////////////////////////////////////////////////////////////////

        // Рисуване на тялото на войника
        g2d.setColor(teamColor);
        g2d.fillOval((int) (x - soldierBodyRadius), (int) (y - soldierBodyRadius), soldierBodyRadius * 2, soldierBodyRadius * 2);

        // Рисуване на оръжието
        g2d.setColor(Color.YELLOW);
        int x1 = (int) x;
        int y1 = (int) y;
        int x2 = x1 + (int) (weaponLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = y1 + (int) (weaponLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.drawLine(x1, y1, x2, y2);

        // Рисуване на ID
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        g2d.drawString("" + id, (int) x - 6, (int) y - soldierBodyRadius - 1);

        // Рисуване на здравето
        if (showHealth) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("" + healthPoints, (int) x - 8, (int) y - soldierBodyRadius - 9);
        }

        // Рисуване на текущия патрон
        if (currentProjectile != null) {
            currentProjectile.draw(g2d);
        }

        // Рисуване на гранатата, ако е хвърлена
        if (currentGrenade != null) {
            currentGrenade.drawGrenade(g2d);
        }
    }


    public void soldierShoot(Character target) {
        if (target == null || !target.isActive()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShotTime < 1000) return;//////////////////////////////////////////////////////////////////

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

            lastShotTime = currentTime;
        }
    }

    public void updateProjectile(Character target) {
        if (currentProjectile != null && currentProjectile.isActive()) {
            currentProjectile.updateSoldierProjectilePosition();

            if (currentProjectile.checkCollision(target)) {
                target.takeDamage(1);
                currentProjectile.deactivate();
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

            if (healthPoints > 1 && healthPoints <= 3) {
                Character target = findTarget();
                if (target != null) {
                    currentGrenade = new Grenade(this.x, this.y, target.getX(), target.getY());
                }
            }

            if (healthPoints <= 20 && currentGrenade == null) {
                Character target = findTarget();
                if (target != null) {
                    currentGrenade = new Grenade(this.x, this.y, target.getX(), target.getY());
                }
            }

        }
    }

    public void moveBack() {
        double moveAngle = Math.toRadians(currentAngle + 180);
        final int MOVE_BACK_DISTANCE = 120;//////////////////////////////////////////////////////////////////////////////
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
        double speed = 1.0;

        // Център на екрана / бойното поле
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        double centerX = screenWidth / 2.0;
        double centerY = screenHeight / 2.0;

        // Ако е флангов войник, заобикаля центъра под ъгъл
        double angleToCenter = calculateAngleTo(this.x, this.y, centerX, centerY);
        if (isFlanking) {
            angleToCenter += 40; // или друг ъгъл
        }

        this.x += speed * Math.cos(Math.toRadians(angleToCenter));
        this.y += speed * Math.sin(Math.toRadians(angleToCenter));

        maintainDistanceFromTeammates(teammates);
    }



    public void setFlanking(boolean flanking) {
        this.isFlanking = flanking;
    }

    public boolean isFlanking() {
        return flanking;
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

    public void updateSoldier(Soldier[] teammates, Soldier[] enemyArmy) {
        if (waiting || !isActive()) return;

        // 🔁 Флангови маневри (ако сме флангови)
        if (flanking) {
            Soldier flankTarget = getFlankTarget();
            if (flankTarget != null && flankTarget.isActive()) {
                moveToFlank(flankTarget);
            } else {
                // Целта е мъртва или липсва — връщаме войника към нормално поведение
                setFlanking(false);
                setFlankTarget(null);
            }
            return;
        }

        // 🔫 Нормално поведение
        Character target = findTarget();

        if (target != null) {
            if (healthPoints <= 5 && !hasThrownGrenade) {
                currentGrenade = new Grenade(this.x, this.y, target.getX(), target.getY());
                hasThrownGrenade = true;
            }

            if (healthPoints > 5 || hasThrownGrenade) {
                soldierShoot(target);
                updateProjectile(target);
            }
        } else {
            soldierMoveTowardsCenter(teammates);
        }

        if (currentGrenade != null) {
            currentGrenade.update();
        }
    }




    public Soldier findEnemyRearTarget(Soldier[] enemyArmy) {
        // За "blue" вземи най-задния (най-малък Y), за "red" най-голям Y
        Soldier rear = null;
        for (Soldier s : enemyArmy) {
            if (s != null && s.isActive()) {
                if (rear == null ||
                        (team.equals("blue") && s.getY() < rear.getY()) ||
                        (team.equals("red") && s.getY() > rear.getY())) {
                    rear = s;
                }
            }
        }
        return rear;
    }

    public void moveToFlank(Soldier target) {
        double speed = 1.5;
        double angle = Math.atan2(target.getY() - y, target.getX() - x);
        x += speed * Math.cos(angle);
        y += speed * Math.sin(angle);
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
        double angle = Math.atan2(this.y - y, this.x - x);
        int offset = 100; ///////////////////////////////////////////////////////////////////////////////////////////////

        this.x += offset * Math.cos(angle);
        this.y += offset * Math.sin(angle);
    }

    public void setFlankTarget(Soldier target) {
        this.flankTarget = target;
    }

    public Soldier getFlankTarget() {
        return this.flankTarget;
    }


    private class Projectile {
        private double x, y;
        private double targetX, targetY;
        private double directionAngle;
        private double speed = 35.0; ///////////////////////////////////////////////////////////////////////////////////
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

            this.x += dx;
            this.y += dy;

            for (Character character : game.getCharacters()) {
                if (!character.isActive() || character.getTeam().equals(Soldier.this.team)) continue;

                if (Point2D.distance(x, y, character.getX(), character.getY()) < 5) {
                    character.takeDamage(1);
                    this.active = false;
                    return;
                }
            }

            if (Point2D.distance(x, y, targetX, targetY) > 5) {
                alpha -= 0.1f;
                if (alpha <= 0) {
                    this.active = false;
                }
            }
        }

        public boolean checkCollision(Character target) {
            return Point2D.distance(x, y, target.getX(), target.getY()) < 5;
        }

        public void draw(Graphics2D g2d) {
            if (!active) return;

            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            double startX = x - 3 * Math.cos(Math.toRadians(directionAngle));
            double startY = y - 3 * Math.sin(Math.toRadians(directionAngle));
            double endX = x + 3 * Math.cos(Math.toRadians(directionAngle));
            double endY = y + 3 * Math.sin(Math.toRadians(directionAngle));

            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawLine((int) startX, (int) startY, (int) endX, (int) endY);

            g2d.setComposite(originalComposite);
        }

        public boolean isActive() {
            return active;
        }

        public void deactivate() {
            this.active = false;
        }
    }

    private class Grenade {
        private double x, y;
        private final double targetX, targetY;
        private final double speed = 12.0;//////////////////////////////////////////////////////////////////////////////
        private int countdown = 5;
        private boolean exploded = false;
        private float alpha = 1.0f;

        public Grenade(double startX, double startY, double targetX, double targetY) {
            this.x = startX;
            this.y = startY;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public void update() {
            if (exploded) return;

            double angle = Math.atan2(targetY - y, targetX - x);
            double dx = speed * Math.cos(angle);
            double dy = speed * Math.sin(angle);

            x += dx;
            y += dy;

            if (Point2D.distance(x, y, targetX, targetY) < speed) {
                countdown--;
                if (countdown <= 0) {
                    explode();
                }
            }
        }

        private void explode() {
            exploded = true;

            for (Character character : game.getCharacters()) {
                if (!character.getTeam().equals(Soldier.this.team)) { // Само врагове
                    double distance = Point2D.distance(x, y, character.getX(), character.getY());
                    if (distance <= 50) {
                        character.takeDamage(5);

                        double angle = Math.atan2(character.getY() - y, character.getX() - x);
                        character.setX(character.getX() + 50 * Math.cos(angle));
                        character.setY(character.getY() + 50 * Math.sin(angle));
                    }
                }
            }
        }

        public void drawGrenade(Graphics2D g2d) {
            if (alpha <= 0) return;

            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            if (!exploded) {
                // Рисуване на гранатата
                g2d.setColor(Color.RED);
                g2d.fillOval((int) x - 5, (int) y - 5, 6, 6);

                //g2d.setColor(Color.WHITE);
                //g2d.setFont(new Font("Arial", Font.BOLD, 3));////////////////////////////////////////////////
                //g2d.drawString("" + countdown, (int) x - 3, (int) y + 3);
            } else {
                // Рисуване на експлозията
                g2d.setColor(new Color(255, 0, 0, (int) (alpha * 255)));
                g2d.fillOval((int) x - 25, (int) y - 25, 30, 30);////////////////////////////////////////

                alpha -= 0.02f;
            }

            g2d.setComposite(originalComposite);
        }

        public boolean isExploded() {
            return exploded && alpha <= 0;
        }
    }

}
