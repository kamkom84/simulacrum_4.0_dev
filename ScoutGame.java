
package classesSeparated;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScoutGame extends JFrame {
    private int blueBaseX, blueBaseY, redBaseX, redBaseY;
    private int blueBaseHealth = 0; // Начална стойност 0
    private int redBaseHealth = 0;  // Начална стойност 0
    private final int baseWidth = 75;
    private final int baseHeight = 75;
    private Worker[] blueWorkers;
    private Worker[] redWorkers;
    private Point[] resources;
    private int[] resourceValues;
    private boolean[] resourceOccupied;
    private Defender[] blueDefenders;
    private Defender[] redDefenders;
    private Scout blueScout;
    private Scout redScout;
    private List<Worker> allWorkers;
    private long startTime;
    private final int DEFENDER_SHIELD_RADIUS = (int) (baseWidth * 1.5);
    private boolean gameOver = false;
    private String winner = "";

    public ScoutGame() {
        allWorkers = new ArrayList<>();

        setTitle("simulacrum");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Listener за възстановяване на цял екран при максимизиране
        addWindowStateListener(e -> {
            if ((e.getNewState() & Frame.NORMAL) == Frame.NORMAL) {
                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                gd.setFullScreenWindow(this); // Включване на цял екран при възстановяване
            }
        });

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gd.setFullScreenWindow(this);

        setVisible(true);

        // Задаване на координатите на базите
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        blueBaseX = 100;
        blueBaseY = screenHeight / 2 - baseHeight / 2;
        redBaseX = screenWidth - 100 - baseWidth;
        redBaseY = screenHeight / 2 - baseHeight / 2;

        // Създаване на скаутите след като координатите на базите са зададени
        blueScout = new Scout(blueBaseX, blueBaseY, "blue", this);
        redScout = new Scout(redBaseX, redBaseY, "red", this);

        // Инициализиране и генериране на ресурсите
        initializeResources();
        generateResources();

        blueWorkers = new Worker[40]; ///////////////////////////////////////////////////////// Брой на сините работници
        redWorkers = new Worker[40];  //  ///////////////////////////////////////////////////// Брой на червените работници
        for (int i = 0; i < blueWorkers.length; i++) {
            blueWorkers[i] = new Worker(
                    blueBaseX + baseWidth / 2,
                    blueBaseY + baseHeight + 20 + 30 * i,
                    "blue",
                    resources,
                    resourceValues,
                    resourceOccupied, // Предаваме масива тук
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );
            redWorkers[i] = new Worker(
                    redBaseX + baseWidth / 2,
                    redBaseY - 20 - 30 * i,
                    "red",
                    resources,
                    resourceValues,
                    resourceOccupied, // Предаваме масива тук
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );
            allWorkers.add(blueWorkers[i]);
            allWorkers.add(redWorkers[i]);
        }

        blueDefenders = new Defender[3];
        redDefenders = new Defender[3];
        initializeDefenders();

        scheduleWorkerStarts();
        startTime = System.currentTimeMillis();

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int shieldRadius = (int) (baseWidth * 2.9);

                drawBasesAndResources(g2d, shieldRadius);
                drawWorkers(g2d);

                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
                int hours = (int) (elapsedTime / (1000 * 60 * 60));

                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                String timeText = String.format("Time: %02d:%02d:%02d", hours, minutes, seconds);
                g2d.setColor(Color.WHITE);
                g2d.drawString(timeText, 10, 30);

                // Изчисляване на ширината на текста за времето
                FontMetrics fm = g2d.getFontMetrics();
                int timeTextWidth = fm.stringWidth(timeText);

                int xPosition = 10 + timeTextWidth + 50; // Първа позиция след времето

                // Показване на точките за синята база
                g2d.setColor(Color.BLUE);
                g2d.drawString(String.valueOf(blueBaseHealth), xPosition, 30);

                // Преместване с 50 пиксела за следващата стойност
                xPosition += 150;

                // Показване на точките за червената база
                g2d.setColor(Color.RED);
                g2d.drawString(String.valueOf(redBaseHealth), xPosition, 30);

                // Показване на победителя, ако играта е приключила
                if (ScoutGame.this.gameOver) {
                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                    g2d.setColor(Color.YELLOW);
                    FontMetrics fmWinner = g2d.getFontMetrics();
                    int winnerTextWidth = fmWinner.stringWidth(ScoutGame.this.winner);
                    int winnerX = (getWidth() - winnerTextWidth) / 2;
                    int winnerY = getHeight() / 2;
                    g2d.drawString(ScoutGame.this.winner, winnerX, winnerY);
                }
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(Color.DARK_GRAY);

        // Бутон за минимизиране
        JButton minimizeButton = new JButton("-");
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));
        controlPanel.add(minimizeButton);

        // Бутон за цял екран
        JButton fullscreenButton = new JButton("□");
        fullscreenButton.addActionListener(e -> {
            setUndecorated(!isUndecorated());
            setVisible(true);
            GraphicsDevice gdDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gdDevice.setFullScreenWindow(isUndecorated() ? this : null);
        });
        controlPanel.add(fullscreenButton);

        // Бутон за затваряне
        JButton closeButton = new JButton("X");
        closeButton.addActionListener(e -> System.exit(0));
        controlPanel.add(closeButton);

        add(controlPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        Timer timer = new Timer(150, e -> {
            long elapsedTime = System.currentTimeMillis() - startTime;

            // Проверка дали са минали 5 минути (300000 милисекунди)
            if (elapsedTime >= 300000) {
                if (!blueScout.isActive()) {
                    blueScout.activate(); // Метод за активиране на скаута
                }
                if (!redScout.isActive()) {
                    redScout.activate();
                }
            }

            // Други операции по време на таймера
            moveDefenders();
            moveWorkers();
            mainPanel.repaint();
        });
        timer.start();

        setVisible(true);
    }

    private void initializeResources() {
        resources = new Point[150]; ////////////////////////////////////////////////////////////////////// Брой на ресурсите
        resourceValues = new int[resources.length];
        resourceOccupied = new boolean[resources.length]; // Инициализираме масива тук

        for (int i = 0; i < resources.length; i++) {
            resourceValues[i] = 100; ////////////////////////////////////////////////////////////////////// Начална стойност на ресурсите
            resourceOccupied[i] = false;
        }
    }

    private void generateResources() {
        Random random = new Random();
        int panelWidth = Math.max(getContentPane().getWidth(), 800);
        int panelHeight = Math.max(getContentPane().getHeight(), 600);

        for (int i = 0; i < resources.length; i++) {
            int x, y;
            do {
                x = random.nextInt(panelWidth - 2 * baseWidth) + baseWidth;
                y = random.nextInt(panelHeight - 2 * baseHeight) + baseHeight;
            } while (isNearBase(x, y));

            resources[i] = new Point(x, y);
        }
    }

    private boolean isNearBase(int x, int y) {
        int blueBaseCenterX = blueBaseX + baseWidth / 2;
        int blueBaseCenterY = blueBaseY + baseHeight / 2;
        int redBaseCenterX = redBaseX + baseWidth / 2;
        int redBaseCenterY = redBaseY + baseHeight / 2;
        int minDistance = 200; // Намален за по-равномерно разпределение

        return distance(x, y, blueBaseCenterX, blueBaseCenterY) < minDistance ||
                distance(x, y, redBaseCenterX, redBaseCenterY) < minDistance;
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private void initializeDefenders() {
        for (int i = 0; i < 3; i++) {
            blueDefenders[i] = new Defender(
                    blueBaseX + baseWidth / 2,
                    blueBaseY + baseHeight / 2,
                    "blue",
                    "defender",
                    i * Math.PI / 4
            );
            redDefenders[i] = new Defender(
                    redBaseX + baseWidth / 2,
                    redBaseY + baseHeight / 2,
                    "red",
                    "defender",
                    i * Math.PI / 4
            );
        }
    }

    private void moveDefenders() {
        for (Defender defender : blueDefenders) {
            if (defender != null) {
                defender.patrolAroundBase(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);
            }
        }
        for (Defender defender : redDefenders) {
            if (defender != null) {
                defender.patrolAroundBase(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);
            }
        }
    }

    private void drawBasesAndResources(Graphics2D g2d, int shieldRadius) {
        // Синя база
        g2d.setColor(new Color(0, 100, 200));
        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.BLUE);
        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);

        g2d.setColor(new Color(0, 0, 255, 100));
        g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        // Червена база
        g2d.setColor(new Color(200, 50, 50));
        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);

        g2d.setColor(new Color(255, 0, 0, 100));
        g2d.drawOval(redBaseX - (shieldRadius - baseWidth) / 2, redBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        for (int i = 0; i < resources.length; i++) {
            Point p = resources[i];
            g2d.setColor(resourceValues[i] <= 0 ? new Color(169, 169, 169) : new Color(255, 223, 0));
            g2d.fillOval(p.x - 20, p.y - 20, 40, 40);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(p.x - 20, p.y - 20, 40, 40);

            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(resourceValues[i]), p.x - 10, p.y + 5);
        }
    }

    private void drawWorkers(Graphics2D g2d) {
        drawWorkersWithLine(g2d, blueScout);
        drawWorkersWithLine(g2d, redScout);

        for (Worker worker : blueWorkers) {
            drawWorkersWithLine(g2d, worker);
        }
        for (Worker worker : redWorkers) {
            drawWorkersWithLine(g2d, worker);
        }
        for (Defender defender : blueDefenders) {
            drawWorkersWithLine(g2d, defender);
        }
        for (Defender defender : redDefenders) {
            drawWorkersWithLine(g2d, defender);
        }
    }

    private void drawWorkersWithLine(Graphics2D g2d, Character ant) {
        if (ant == null) return;

        int bodyRadius = 5;
        if (ant instanceof Defender) bodyRadius *= 1.5;

        g2d.setColor(ant.team.equals("blue") ? Color.BLUE : Color.RED);
        g2d.fillOval(ant.getX(), ant.getY(), bodyRadius * 2, bodyRadius * 2);

        int lineLength;
        if (ant instanceof Scout) {
            g2d.setColor(Color.GREEN);
            lineLength = bodyRadius * 2;
        } else if (ant instanceof Defender) {
            g2d.setColor(Color.RED);
            lineLength = bodyRadius;
        } else {
            g2d.setColor(Color.YELLOW);
            lineLength = bodyRadius;
        }

        int x2 = ant.getX() + bodyRadius + (int) (lineLength * Math.cos(Math.toRadians(ant.angle)));
        int y2 = ant.getY() + bodyRadius + (int) (lineLength * Math.sin(Math.toRadians(ant.angle)));

        int x1 = ant.getX() + bodyRadius;
        int y1 = ant.getY() + bodyRadius;

        g2d.drawLine(x1, y1, x2, y2);

        if (ant instanceof Worker) {
            int visionRadius = 10;
            g2d.setColor(new Color(255, 255, 0, 60));
            g2d.drawOval(ant.getX() + bodyRadius - visionRadius, ant.getY() + bodyRadius - visionRadius, visionRadius * 2, visionRadius * 2);
        }
    }

    private void scheduleWorkerStarts() {
        int initialDelay = 30000; // 30 секунди
        int interval = 30000;     // 30 секунди между всеки работник

        for (int i = 0; i < blueWorkers.length; i++) {
            int delay = initialDelay + (i * interval);

            final int workerIndex = i; // Правим копие на i, което е final

            // Активиране на син работник
            Timer blueWorkerTimer = new Timer(delay, e -> {
                blueWorkers[workerIndex].activate(); // Използваме workerIndex вместо i
                System.out.println("Activated blue worker " + (workerIndex + 1) + " after " + delay / 1000 + " seconds.");
                ((Timer) e.getSource()).stop();
            });
            blueWorkerTimer.setRepeats(false);
            blueWorkerTimer.start();

            // Активиране на червен работник
            Timer redWorkerTimer = new Timer(delay, e -> {
                redWorkers[workerIndex].activate(); // Използваме workerIndex вместо i
                System.out.println("Activated red worker " + (workerIndex + 1) + " after " + delay / 1000 + " seconds.");
                ((Timer) e.getSource()).stop();
            });
            redWorkerTimer.setRepeats(false);
            redWorkerTimer.start();
        }
    }

    private void moveWorkers() {
        if (gameOver) return; // Ако играта вече е приключила, не правим нищо

        boolean anyActiveWorkers = false;

        for (Worker worker : blueWorkers) {
            if (worker != null) {
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, redScout);
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        for (Worker worker : redWorkers) {
            if (worker != null) {
                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout);
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        // Проверяваме дали няма активни работници, всички работници са стартирали и всички ресурси са изчерпани
        if (!anyActiveWorkers && allWorkersStarted() && allResourcesDepleted() && !gameOver) {
            determineWinner();
        }
    }

    public boolean allResourcesDepleted() {
        for (int value : resourceValues) {
            if (value > 0) {
                return false; // Има останали ресурси
            }
        }
        return true; // Всички ресурси са изчерпани
    }

    private boolean allWorkersStarted() {
        for (Worker worker : blueWorkers) {
            if (worker != null && !worker.hasStarted()) {
                return false; // Има работник, който не е стартирал
            }
        }
        for (Worker worker : redWorkers) {
            if (worker != null && !worker.hasStarted()) {
                return false; // Има работник, който не е стартирал
            }
        }
        return true; // Всички работници са стартирали
    }

    private void determineWinner() {
        if (blueBaseHealth > redBaseHealth) {
            winner = "Синият отбор печели!";
        } else if (redBaseHealth > blueBaseHealth) {
            winner = "Червеният отбор печели!";
        } else {
            winner = "Равенство!";
        }
        gameOver = true; // Маркираме, че играта е приключила
        System.out.println("Играта приключи. " + winner);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScoutGame::new);
    }

    public List<Worker> getAllWorkers() {
        return allWorkers;
    }

    // Getter и Setter за blueBaseHealth
    public int getBlueBaseHealth() {
        return blueBaseHealth;
    }

    public void setBlueBaseHealth(int health) {
        this.blueBaseHealth = health;
    }

    // Getter и Setter за redBaseHealth
    public int getRedBaseHealth() {
        return redBaseHealth;
    }

    public void setRedBaseHealth(int health) {
        this.redBaseHealth = health;
    }

    public void addPointsToScoutBase(String team, int points) {
        if (team.equals("blue")) {
            blueBaseHealth += points;
        } else if (team.equals("red")) {
            redBaseHealth += points;
        }
    }

    public Worker findClosestEnemyWorker(Scout scout, String scoutTeam) {
        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
        Worker closestWorker = null;
        double closestDistance = Double.MAX_VALUE;

        for (Worker worker : enemyWorkers) {
            double distance = scout.distanceTo(worker);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWorker = worker;
            }
        }

        return closestWorker;
    }
}





























//
//
//package classesSeparated;
//
//import javax.swing.*;
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
//public class ScoutGame extends JFrame {
//    private int blueBaseX, blueBaseY, redBaseX, redBaseY;
//    private int blueBaseHealth = 0; // Променено на 0
//    private int redBaseHealth = 0;  // Променено на 0
//    private final int baseWidth = 75;
//    private final int baseHeight = 75;
//    private Worker[] blueWorkers;
//    private Worker[] redWorkers;
//    private Point[] resources;
//    private int[] resourceValues;
//    private boolean[] resourceOccupied;
//    private Defender[] blueDefenders;
//    private Defender[] redDefenders;
//    private Scout blueScout;
//    private Scout redScout;
//    private List<Worker> allWorkers;
//    private long startTime;
//    private final int DEFENDER_SHIELD_RADIUS = (int) (baseWidth * 1.5);
//
//    public ScoutGame() {
//        allWorkers = new ArrayList<>();
//
//        setTitle("simulacrum");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setResizable(false);
//        setExtendedState(JFrame.MAXIMIZED_BOTH);
//
//        // Listener за възстановяване на цял екран при максимизиране
//        addWindowStateListener(e -> {
//            if ((e.getNewState() & Frame.NORMAL) == Frame.NORMAL) {
//                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//                gd.setFullScreenWindow(this); // Включване на цял екран при възстановяване
//            }
//        });
//
//        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        gd.setFullScreenWindow(this);
//
//        setVisible(true);
//
//        // Задаване на координатите на базите
//        int screenWidth = getWidth();
//        int screenHeight = getHeight();
//        blueBaseX = 100;
//        blueBaseY = screenHeight / 2 - baseHeight / 2;
//        redBaseX = screenWidth - 100 - baseWidth;
//        redBaseY = screenHeight / 2 - baseHeight / 2;
//
//        // Създаване на скаутите след като координатите на базите са зададени
//        blueScout = new Scout(blueBaseX, blueBaseY, "blue", this);
//        redScout = new Scout(redBaseX, redBaseY, "red", this);
//
//        // Инициализиране и генериране на ресурсите
//        initializeResources();
//        generateResources();
//
//        blueWorkers = new Worker[5]; // ////////////////////////////////////////////////////////Брой на сините работници
//        redWorkers = new Worker[5];  // ////////////////////////////////////////////////////////Брой на червените работници
//        for (int i = 0; i < blueWorkers.length; i++) {
//            blueWorkers[i] = new Worker(
//                    blueBaseX + baseWidth / 2,
//                    blueBaseY + baseHeight + 20 + 30 * i,
//                    "blue",
//                    resources,
//                    resourceValues,
//                    resourceOccupied, // Предаваме масива тук
//                    baseWidth,
//                    baseHeight,
//                    this,
//                    i + 1
//            );
//            redWorkers[i] = new Worker(
//                    redBaseX + baseWidth / 2,
//                    redBaseY - 20 - 30 * i,
//                    "red",
//                    resources,
//                    resourceValues,
//                    resourceOccupied, // Предаваме масива тук
//                    baseWidth,
//                    baseHeight,
//                    this,
//                    i + 1
//            );
//            allWorkers.add(blueWorkers[i]);
//            allWorkers.add(redWorkers[i]);
//        }
//
//        blueDefenders = new Defender[3];
//        redDefenders = new Defender[3];
//        initializeDefenders();
//
//        scheduleWorkerStarts();
//        startTime = System.currentTimeMillis();
//
//        JPanel mainPanel = new JPanel() {
//            @Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                Graphics2D g2d = (Graphics2D) g;
//                int shieldRadius = (int) (baseWidth * 2.9);
//
//                drawBasesAndResources(g2d, shieldRadius);
//                drawWorkers(g2d);
//
//                long elapsedTime = System.currentTimeMillis() - startTime;
//                int seconds = (int) (elapsedTime / 1000) % 60;
//                int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
//                int hours = (int) (elapsedTime / (1000 * 60 * 60));
//
//                g2d.setFont(new Font("Arial", Font.BOLD, 18));
//                String timeText = String.format("Time: %02d:%02d:%02d", hours, minutes, seconds);
//                g2d.setColor(Color.WHITE);
//                g2d.drawString(timeText, 10, 30);
//
//                // Изчисляване на ширината на текста за времето
//                FontMetrics fm = g2d.getFontMetrics();
//                int timeTextWidth = fm.stringWidth(timeText);
//
//                int xPosition = 10 + timeTextWidth + 50; // Първа позиция след времето
//
//                // Показване на точките за синята база
//                g2d.setColor(Color.BLUE);
//                g2d.drawString(String.valueOf(blueBaseHealth), xPosition, 30);
//
//                // Преместване с 50 пиксела за следващата стойност
//                xPosition += 50;
//
//                // Показване на точките за червената база
//                g2d.setColor(Color.RED);
//                g2d.drawString(String.valueOf(redBaseHealth), xPosition, 30);
//            }
//        };
//        mainPanel.setLayout(new BorderLayout());
//        mainPanel.setBackground(Color.BLACK);
//
//        JPanel controlPanel = new JPanel();
//        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
//        controlPanel.setBackground(Color.DARK_GRAY);
//
//        // Бутон за минимизиране
//        JButton minimizeButton = new JButton("-");
//        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));
//        controlPanel.add(minimizeButton);
//
//        // Бутон за цял екран
//        JButton fullscreenButton = new JButton("□");
//        fullscreenButton.addActionListener(e -> {
//            setUndecorated(!isUndecorated());
//            setVisible(true);
//            GraphicsDevice gdDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//            gdDevice.setFullScreenWindow(isUndecorated() ? this : null);
//        });
//        controlPanel.add(fullscreenButton);
//
//        // Бутон за затваряне
//        JButton closeButton = new JButton("X");
//        closeButton.addActionListener(e -> System.exit(0));
//        controlPanel.add(closeButton);
//
//        add(controlPanel, BorderLayout.NORTH);
//        add(mainPanel, BorderLayout.CENTER);
//
//        Timer timer = new Timer(150, e -> {
//            long elapsedTime = System.currentTimeMillis() - startTime;
//
//            // Проверка дали са минали 5 минути (300000 милисекунди)
//            if (elapsedTime >= 300000) {
//                if (!blueScout.isActive()) {
//                    blueScout.activate(); // Метод за активиране на скаута
//                }
//                if (!redScout.isActive()) {
//                    redScout.activate();
//                }
//            }
//
//            // Други операции по време на таймера
//            moveDefenders();
//            moveWorkers();
//            mainPanel.repaint();
//        });
//        timer.start();
//
//        setVisible(true);
//    }
//
//    private void initializeResources() {
//        resources = new Point[11]; //////////////////////////////////////////////// Брой на ресурсите
//        resourceValues = new int[resources.length];
//        resourceOccupied = new boolean[resources.length]; // Инициализираме масива тук
//
//        for (int i = 0; i < resources.length; i++) {
//            resourceValues[i] = 5; ////////////////////////////////////////////////////// Начална стойност на ресурсите
//            resourceOccupied[i] = false;
//        }
//    }
//
//    private void generateResources() {
//        Random random = new Random();
//        int panelWidth = Math.max(getContentPane().getWidth(), 800);
//        int panelHeight = Math.max(getContentPane().getHeight(), 600);
//
//        for (int i = 0; i < resources.length; i++) {
//            int x, y;
//            do {
//                x = random.nextInt(panelWidth - 2 * baseWidth) + baseWidth;
//                y = random.nextInt(panelHeight - 2 * baseHeight) + baseHeight;
//            } while (isNearBase(x, y));
//
//            resources[i] = new Point(x, y);
//        }
//    }
//
//    private boolean isNearBase(int x, int y) {
//        int blueBaseCenterX = blueBaseX + baseWidth / 2;
//        int blueBaseCenterY = blueBaseY + baseHeight / 2;
//        int redBaseCenterX = redBaseX + baseWidth / 2;
//        int redBaseCenterY = redBaseY + baseHeight / 2;
//        int minDistance = 200; // Намален за по-равномерно разпределение
//
//        return distance(x, y, blueBaseCenterX, blueBaseCenterY) < minDistance ||
//                distance(x, y, redBaseCenterX, redBaseCenterY) < minDistance;
//    }
//
//    private double distance(int x1, int y1, int x2, int y2) {
//        return Math.hypot(x1 - x2, y1 - y2);
//    }
//
//    private void initializeDefenders() {
//        for (int i = 0; i < 3; i++) {
//            blueDefenders[i] = new Defender(
//                    blueBaseX + baseWidth / 2,
//                    blueBaseY + baseHeight / 2,
//                    "blue",
//                    "defender",
//                    i * Math.PI / 4
//            );
//            redDefenders[i] = new Defender(
//                    redBaseX + baseWidth / 2,
//                    redBaseY + baseHeight / 2,
//                    "red",
//                    "defender",
//                    i * Math.PI / 4
//            );
//        }
//    }
//
//    private void moveDefenders() {
//        for (Defender defender : blueDefenders) {
//            if (defender != null) {
//                defender.patrolAroundBase(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);
//            }
//        }
//        for (Defender defender : redDefenders) {
//            if (defender != null) {
//                defender.patrolAroundBase(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);
//            }
//        }
//    }
//
//    private void drawBasesAndResources(Graphics2D g2d, int shieldRadius) {
//        // Синя база
//        g2d.setColor(new Color(0, 100, 200));
//        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
//        g2d.setColor(Color.BLUE);
//        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
//        // Премахнахме показването на точките до базата
//
//        g2d.setColor(new Color(0, 0, 255, 100));
//        g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);
//
//        // Червена база
//        g2d.setColor(new Color(200, 50, 50));
//        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
//        g2d.setColor(Color.RED);
//        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
//        // Премахнахме показването на точките до базата
//
//        g2d.setColor(new Color(255, 0, 0, 100));
//        g2d.drawOval(redBaseX - (shieldRadius - baseWidth) / 2, redBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);
//
//        for (int i = 0; i < resources.length; i++) {
//            Point p = resources[i];
//            g2d.setColor(resourceValues[i] <= 0 ? new Color(169, 169, 169) : new Color(255, 223, 0));
//            g2d.fillOval(p.x - 20, p.y - 20, 40, 40);
//            g2d.setColor(Color.BLACK);
//            g2d.drawOval(p.x - 20, p.y - 20, 40, 40);
//
//            g2d.setFont(new Font("Arial", Font.BOLD, 10));
//            g2d.setColor(Color.BLACK);
//            g2d.drawString(String.valueOf(resourceValues[i]), p.x - 10, p.y + 5);
//        }
//    }
//
//    private void drawWorkers(Graphics2D g2d) {
//        drawWorkersWithLine(g2d, blueScout);
//        drawWorkersWithLine(g2d, redScout);
//
//        for (Worker worker : blueWorkers) {
//            drawWorkersWithLine(g2d, worker);
//        }
//        for (Worker worker : redWorkers) {
//            drawWorkersWithLine(g2d, worker);
//        }
//        for (Defender defender : blueDefenders) {
//            drawWorkersWithLine(g2d, defender);
//        }
//        for (Defender defender : redDefenders) {
//            drawWorkersWithLine(g2d, defender);
//        }
//    }
//
//    private void drawWorkersWithLine(Graphics2D g2d, Character ant) {
//        if (ant == null) return;
//
//        int bodyRadius = 5;
//        if (ant instanceof Defender) bodyRadius *= 1.5;
//
//        g2d.setColor(ant.team.equals("blue") ? Color.BLUE : Color.RED);
//        g2d.fillOval(ant.getX(), ant.getY(), bodyRadius * 2, bodyRadius * 2);
//
//        int lineLength;
//        if (ant instanceof Scout) {
//            g2d.setColor(Color.GREEN);
//            lineLength = bodyRadius * 2;
//        } else if (ant instanceof Defender) {
//            g2d.setColor(Color.RED);
//            lineLength = bodyRadius;
//        } else {
//            g2d.setColor(Color.YELLOW);
//            lineLength = bodyRadius;
//        }
//
//        int x2 = ant.getX() + bodyRadius + (int) (lineLength * Math.cos(Math.toRadians(ant.angle)));
//        int y2 = ant.getY() + bodyRadius + (int) (lineLength * Math.sin(Math.toRadians(ant.angle)));
//
//        int x1 = ant.getX() + bodyRadius;
//        int y1 = ant.getY() + bodyRadius;
//
//        g2d.drawLine(x1, y1, x2, y2);
//
//        if (ant instanceof Worker) {
//            int visionRadius = 10;
//            g2d.setColor(new Color(255, 255, 0, 60));
//            g2d.drawOval(ant.getX() + bodyRadius - visionRadius, ant.getY() + bodyRadius - visionRadius, visionRadius * 2, visionRadius * 2);
//        }
//    }
//
//    private void scheduleWorkerStarts() {
//        int initialDelay = 30000; // 30 секунди
//        int interval = 30000;     // 30 секунди между всеки работник
//
//        for (int i = 0; i < allWorkers.size(); i++) {
//            Worker worker = allWorkers.get(i);
//            int delay = initialDelay + (i * interval);
//
//            int workerIndex = i + 1;
//            Timer timer = new Timer(delay, e -> {
//                worker.activate(); // Активиране на работника
//                System.out.println("Activated worker " + workerIndex + " after " + delay / 1000 + " seconds.");
//                ((Timer) e.getSource()).stop();
//            });
//            timer.setRepeats(false);
//            timer.start();
//        }
//    }
//
//
//
//    private void moveWorkers() {
//        for (Worker worker : blueWorkers) {
//            if (worker != null) {
//                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, redScout);
//            }
//        }
//        for (Worker worker : redWorkers) {
//            if (worker != null) {
//                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout);
//            }
//        }
//        checkGameStatus();
//    }
//
//    private void checkGameStatus() {
//        // Добавете логика за проверка на състоянието на играта
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(ScoutGame::new);
//    }
//
//    public List<Worker> getAllWorkers() {
//        return allWorkers;
//    }
//
//    // Getter и Setter за blueBaseHealth
//    public int getBlueBaseHealth() {
//        return blueBaseHealth;
//    }
//
//    public void setBlueBaseHealth(int health) {
//        this.blueBaseHealth = health;
//    }
//
//    // Getter и Setter за redBaseHealth
//    public int getRedBaseHealth() {
//        return redBaseHealth;
//    }
//
//    public void setRedBaseHealth(int health) {
//        this.redBaseHealth = health;
//    }
//
//    public void addPointsToScoutBase(String team, int points) {
//        if (team.equals("blue")) {
//            blueBaseHealth += points;
//        } else if (team.equals("red")) {
//            redBaseHealth += points;
//        }
//    }
//
//    public Worker findClosestEnemyWorker(Scout scout, String scoutTeam) {
//        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
//        Worker closestWorker = null;
//        double closestDistance = Double.MAX_VALUE;
//
//        for (Worker worker : enemyWorkers) {
//            double distance = scout.distanceTo(worker);
//            if (distance < closestDistance) {
//                closestDistance = distance;
//                closestWorker = worker;
//            }
//        }
//
//        return closestWorker;
//    }
//}

//
//









