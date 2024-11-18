package classesSeparated;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import static java.awt.geom.Point2D.distance;

public class Soldier extends Character {
    private final int weaponLength = 150;
    private final int maxBulletDistance = 200;
    private final int healthBarDuration = 500;
    private boolean showHealth = false;
    private int damageDealt = 0;
    private Color teamColor;
    private ScoutGame game;
    private int id;

    public Soldier(int x, int y, String team, int baseX, int baseY, ScoutGame game, int id) {
        super(x, y, team, "soldier"); // Добавяме "soldier" като role
        this.health = 20;
        this.teamColor = team.equals("blue") ? Color.BLUE : Color.RED;
        this.currentAngle = Math.toDegrees(Math.atan2(game.getHeight() / 2 - y, game.getWidth() / 2 - x)); // Гледа към средата на екрана
        this.game = game;
        this.id = id;
        this.teamColor = team.equals("blue") ? Color.BLUE : Color.RED;
    }

    public void draw(Graphics2D g2d) {
        int bodyRadius = 5;/////////////////////////////////////////////////////////////////
        int lineLength = 15;

        g2d.setColor(teamColor);
        g2d.fillOval((int) (x - bodyRadius), (int) (y - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        g2d.setColor(Color.YELLOW);
        int x1 = (int) x;
        int y1 = (int) y;
        int x2 = x1 + (int) (lineLength * Math.cos(Math.toRadians(currentAngle)));
        int y2 = y1 + (int) (lineLength * Math.sin(Math.toRadians(currentAngle)));
        g2d.drawLine(x1, y1, x2, y2);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 8));
        g2d.drawString("" + id, (int) x - 6, (int) y - bodyRadius - 10);
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

