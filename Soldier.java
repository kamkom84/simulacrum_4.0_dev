package classesSeparated;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

import static java.awt.geom.Point2D.distance;

public class Soldier extends Character {
    private final int weaponLength = 150; // Дължина на оръжието
    private final int bulletLength = 10; // Дължина на патрона
    private final int maxBulletDistance = 200; // Максимална дистанция на патрона
    private final int healthBarDuration = 500; // Продължителност на показване на здравето (в ms)
    private boolean showHealth = false; // Дали да показваме здравето
    private int damageDealt = 0; // Нанесена щета от войника
    private Color teamColor;
    private ScoutGame game;

    public Soldier(int x, int y, String team, int baseX, int baseY, ScoutGame game) {
        super(x, y, team, "soldier"); // Добавяме "soldier" като role
        this.health = 20;
        this.teamColor = team.equals("blue") ? Color.BLUE : Color.RED;
        this.currentAngle = Math.toDegrees(Math.atan2(game.getHeight() / 2 - y, game.getWidth() / 2 - x)); // Гледа към средата на екрана
        this.game = game;
    }



    public void draw(Graphics2D g2d) {
        // Рисуване на тялото
        g2d.setColor(teamColor);
        g2d.fillOval((int) (x - 10), (int) (y - 10), 20, 20);

        // Рисуване на "емблемата" (жълтите линии)
        g2d.setColor(Color.YELLOW);
        drawMercedesSymbol(g2d);

        if (showHealth) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(String.valueOf(health), (int) x - 10, (int) y - 15);
        }

        if (damageDealt > 0) {
            g2d.setColor(Color.GREEN);
            g2d.drawString("+" + damageDealt, (int) x - 10, (int) y - 25);
        }
    }

    private void drawMercedesSymbol(Graphics2D g2d) {
        double angleIncrement = 120; // Ъгъл между линиите
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(currentAngle + i * angleIncrement);
            int xEnd = (int) (x + 15 * Math.cos(angle));
            int yEnd = (int) (y + 15 * Math.sin(angle));
            g2d.drawLine((int) x, (int) y, xEnd, yEnd);
        }

        double weaponAngle = Math.toRadians(currentAngle);
        int weaponXEnd = (int) (x + weaponLength * Math.cos(weaponAngle));
        int weaponYEnd = (int) (y + weaponLength * Math.sin(weaponAngle));
        g2d.drawLine((int) x, (int) y, weaponXEnd, weaponYEnd);
    }

    public void shoot(Character target) {
        if (target == null || !target.isActive()) return;

        double angleToTarget = calculateAngleTo(this.x, this.y, target.getX(), target.getY());
        double distanceToTarget = distance(this.x, this.y, target.getX(), target.getY());

        if (distanceToTarget <= weaponLength) {
            int bulletEndX = (int) (x + maxBulletDistance * Math.cos(Math.toRadians(angleToTarget)));
            int bulletEndY = (int) (y + maxBulletDistance * Math.sin(Math.toRadians(angleToTarget)));

            game.drawShot((int) x, (int) y, bulletEndX, bulletEndY);

            int damage = 2;
            target.takeDamage(damage);
            target.showHealthTemporarily();
            damageDealt += damage;
            System.out.println(team + " Soldier hit " + target.getType() + " for " + damage + " damage.");
        }
    }

    private double calculateAngleTo(double x1, double y1, double x2, double y2) {
        return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    @Override
    public void takeDamage(int damage) {
        super.takeDamage(damage);
        showHealthTemporarily();
    }

    public void showHealthTemporarily() {
        showHealth = true;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showHealth = false;
            }
        }, healthBarDuration);
    }

    @Override
    public String getType() {
        return "Soldier";
    }


}

