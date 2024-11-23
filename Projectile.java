package classesSeparated;

import java.awt.*;

public class Projectile {
    private double x, y; // Позиция на куршума
    private final double targetX, targetY; // Целева позиция
    private final double speed; // Скорост на движение
    private boolean active; // Статус на активност
    private final double maxDistance; // Максимално разстояние
    private double traveledDistance; // Изминато разстояние
    private final Color color; // Цвят на куршума

    // Конструктор
    public Projectile(double startX, double startY, double targetX, double targetY, double speed, double maxDistance) {
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.active = true;
        this.traveledDistance = 0;
        this.color = Color.RED; // Цвят на куршума (по подразбиране червен)
    }

    // Метод за актуализиране на позицията
    public void updatePosition() {
        // Ако куршумът вече е деактивиран, не правим нищо
        if (!active) return;

        // Изчисляване на посоката към целта
        double angle = Math.atan2(targetY - y, targetX - x);

        // Изчисляване на промяната в позицията (делта)
        double deltaX = speed * Math.cos(angle);
        double deltaY = speed * Math.sin(angle);

        // Актуализиране на текущата позиция на куршума
        x += deltaX;
        y += deltaY;

        // Актуализиране на изминатото разстояние
        traveledDistance += Math.hypot(deltaX, deltaY);

        // Проверка дали куршумът е достигнал целта или максималното разстояние
        double distanceToTarget = Math.hypot(targetX - x, targetY - y);
        if (distanceToTarget <= speed || traveledDistance >= maxDistance) {
            active = false; // Деактивира куршума
        }
    }


    // Рисуване на куршума
    public void draw(Graphics g) {
        if (!active) return; // Ако куршумът е неактивен, пропускаме

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);

        // Настройка на дебелината на линията
        g2d.setStroke(new BasicStroke(1.0f)); // По-тънка линия

        // Изчисляваме посоката на движение
        double angle = Math.atan2(targetY - y, targetX - x);

        // Начална и крайна точка на линията
        int endX = (int) (x + 10 * Math.cos(angle)); // Дължина на линията: 10 пиксела
        int endY = (int) (y + 10 * Math.sin(angle));

        // Рисуваме линията
        g2d.drawLine((int) x, (int) y, endX, endY);
    }




    // Проверка дали куршумът е ударил целта
    public boolean hasHit(Character target) {
        double distanceToTarget = Math.hypot(x - target.getX(), y - target.getY());
        if (distanceToTarget <= target.getBodyRadius()) {
            // Ако целта е скаут, изпълняваме специфична логика
            if (target instanceof Scout) {
                Scout scout = (Scout) target;
                scout.decreasePoints(1); // Намаляване на точките
                scout.reverseDirection(); // Промяна на посоката
            }
            return true;
        }
        return false;
    }

    // Проверка дали куршумът е активен
    public boolean isActive() {
        return active;
    }
}
