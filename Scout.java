package classesSeparated;

import java.awt.*;

import static java.awt.geom.Point2D.distance;

public class Scout extends Character {
    private long shootAnimationStartTime = 0;
    private static final int SHOOT_ANIMATION_DURATION = 200; // Продължителност на анимацията в милисекунди
    private Worker currentTargetWorker = null; // Съхранява текущата цел за анимация

    private int points = 2000;
    private static final int MAX_POINTS = 2000;
    private static final int MIN_POINTS = 50;
    private long lastPointReductionTime = System.currentTimeMillis();
    private long lastShootTime = 0;
    private static final int POINT_REDUCTION_INTERVAL = 60 * 1000; // 1 минута
    private static final int SHOOT_INTERVAL = 2000; // 2 секунди
    private ScoutGame scoutGame;
    private boolean returningToBase = false;
    private boolean recharging = false;
    private boolean isActive = false;
    private long startTime; // Време на активация на скаута
    private long rechargeStartTime = 0;
    private static final int RECHARGE_DURATION = 10 * 1000; // 10 секунди

    private double prevX, prevY;
    private double currentAngle; // Текуща посока на движение

    // **Конструктор**
    public Scout(int startX, int startY, String team, ScoutGame game) {
        super(startX, startY, team, "scout");
        this.scoutGame = game;
        this.prevX = startX;
        this.prevY = startY;
        this.currentAngle = Math.random() * 360; // Случайна начална посока
    }

    // **Метод draw()**
    public void draw(Graphics2D g) {
        int bodyRadius = 5;

        // Рисуване на скаута като кръг
        g.setColor(team.equals("blue") ? Color.BLUE : Color.RED);
        g.fillOval((int) x, (int) y, bodyRadius * 2, bodyRadius * 2);

        // Рисуване на посоката
        int lineLength = bodyRadius * 2;
        g.setColor(Color.GREEN);

        int x1 = (int) x + bodyRadius;
        int y1 = (int) y + bodyRadius;
        int x2 = x1 + (int) (lineLength * Math.cos(Math.toRadians(angle)));
        int y2 = y1 + (int) (lineLength * Math.sin(Math.toRadians(angle)));
        g.drawLine(x1, y1, x2, y2);

        // Показване на точките над скаута
        g.setColor(Color.WHITE);
        g.drawString("Points: " + points, (int) x - 10, (int) y - 10);

        // Рисуване на анимацията на стрелбата, ако е активна
        if (System.currentTimeMillis() - shootAnimationStartTime < SHOOT_ANIMATION_DURATION && currentTargetWorker != null) {
            g.setColor(Color.YELLOW);
            int targetX = (int) currentTargetWorker.getX();
            int targetY = (int) currentTargetWorker.getY();
            g.drawLine((int) x + bodyRadius, (int) y + bodyRadius, targetX, targetY);
        }

        // Проверка за край на анимацията
        if (System.currentTimeMillis() - shootAnimationStartTime >= SHOOT_ANIMATION_DURATION) {
            currentTargetWorker = null; // Край на анимацията
        }
    }


    // **Метод update()**
    public void update(Point baseCenter, Resource[] resources) {
        if (!isActive) return; // Ако скаутът не е активен, не правим нищо

        long currentTime = System.currentTimeMillis();

        // Намаляване на точките на всеки 1 минута
        if (currentTime - lastPointReductionTime >= POINT_REDUCTION_INTERVAL) {
            points = Math.max(points - 1, 0);
            lastPointReductionTime = currentTime;
        }

        // Връщане към базата за презареждане
        if (points < MIN_POINTS && !returningToBase && !recharging) {
            returningToBase = true;
        }

        // Движение към базата
        if (returningToBase) {
            moveTo(baseCenter.x, baseCenter.y, 1); // Бавно движение
            if (distance(baseCenter.x, baseCenter.y, this.x, this.y) < 5) {
                recharging = true;
                returningToBase = false;
                rechargeStartTime = currentTime; // Започваме времето за презареждане
                scoutGame.addPointsToScoutBase(team, points); // Добавяме точките към базата
                points = MAX_POINTS;
            }
        }

        // Управление на състоянието на презареждане
        if (recharging) {
            if (currentTime - rechargeStartTime >= RECHARGE_DURATION) {
                recharging = false;
            }
        }

        // Ако скаутът не се връща или не презарежда
        if (!returningToBase && !recharging) {
            // Търсене на най-близкия вражески работник в определен радиус
            double detectionRange = 100; // Радиус на откриване
            Worker targetWorker = scoutGame.findClosestEnemyWorkerWithinRange(this, team, detectionRange);

            if (targetWorker != null && !isNearResource(resources, 20)) {
                // Движение към работника, ако няма ресурс в пътя
                moveTo(targetWorker.getX(), targetWorker.getY(), 1);
                System.out.println("Scout moving towards target worker at: (" + targetWorker.getX() + ", " + targetWorker.getY() + ")");
            } else if (isNearResource(resources, 20)) {
                // Ако има ресурс наблизо, променяме посоката на случайна
                System.out.println("Scout near resource, moving randomly.");
                moveRandomly();
            } else {
                // Ако няма работник в радиуса, се движим плавно
                moveRandomly();
                System.out.println("Scout moving randomly.");
            }

            // Обновяване на ъгъла на скаута
            this.angle = Math.toDegrees(Math.atan2(y - prevY, x - prevX));

            if (currentTime - lastShootTime >= SHOOT_INTERVAL) {
                shootEnemyWorker();
                lastShootTime = currentTime;
            }
        }

        // Поддържане на скаута в границите на екрана
        keepWithinBounds(scoutGame.getWidth(), scoutGame.getHeight());

        // Обновяване на предишните координати
        this.prevX = this.x;
        this.prevY = this.y;
    }

    // **Метод за проверка на близост до ресурс**
    private boolean isNearResource(Resource[] resources, double minDistance) {
        for (Resource resource : resources) {
            double distance = distanceTo(resource);
            if (distance < minDistance) {
                return true; // Скаутът е твърде близо до ресурс
            }
        }
        return false;
    }


    private void moveTo(int targetX, int targetY, int speed) {
        double dx = targetX - this.x;
        double dy = targetY - this.y;
        double distance = Math.hypot(dx, dy);
        if (distance > 0) {
            x += speed * dx / distance;
            y += speed * dy / distance;
        }

        // Обновяване на ъгъла на скаута
        this.angle = Math.toDegrees(Math.atan2(dy, dx));
        System.out.println("Scout moving towards target at: (" + targetX + ", " + targetY + ")");
        System.out.println("Current position of scout: (" + x + ", " + y + ")");
    }


    private void moveRandomly() {
        // Плавна промяна на текущия ъгъл
        double angleChange = (Math.random() - 0.5) * 10; // Промяна от -5 до +5 градуса
        currentAngle += angleChange;
        if (currentAngle < 0) currentAngle += 360;
        if (currentAngle >= 360) currentAngle -= 360;

        double angleRad = Math.toRadians(currentAngle);
        double speed = 1;
        x += speed * Math.cos(angleRad);
        y += speed * Math.sin(angleRad);
        System.out.println("Scout at: " + x + ", " + y);
    }


    private void keepWithinBounds(int panelWidth, int panelHeight) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > panelWidth - 10) x = panelWidth - 10;
        if (y > panelHeight - 10) y = panelHeight - 10;
    }


    private void shootEnemyWorker() {
        Worker targetWorker = scoutGame.findClosestEnemyWorker(this, team);
        if (targetWorker != null) {
            targetWorker.takeDamage(1);
            System.out.println("Скаутът стреля по противников работник!");

            // Стартиране на анимацията на стрелбата
            shootAnimationStartTime = System.currentTimeMillis();
            currentTargetWorker = targetWorker; // Запазваме целта за анимация
        } else {
            System.out.println("Няма противников работник в обсега.");
        }
    }


    // **Метод distanceTo()**
    public double distanceTo(Character other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.hypot(dx, dy);
    }

    // **Метод activate()**
    public void activate() {
        this.isActive = true;
        this.startTime = System.currentTimeMillis(); // Време на активация на скаута
    }

    // **Метод isActive()**
    public boolean isActive() {
        return isActive;
    }
}
