package classesSeparated;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScoutGame extends JFrame {
    private int blueBaseX, blueBaseY, redBaseX, redBaseY;
    private int blueBaseHealth = 0;
    private int redBaseHealth = 0;
    private final int baseWidth = 75;
    private final int baseHeight = 75;
    private Worker[] blueWorkers;
    private Worker[] redWorkers;
    private Resource[] resources;
    private Defender[] blueDefenders;
    private Defender[] redDefenders;
    private Scout blueScout;
    private Scout redScout;
    private List<Worker> allWorkers;
    private long startTime;
    private final int DEFENDER_SHIELD_RADIUS = (int) (baseWidth * 1.5);
    private boolean gameOver = false;
    private String winner = "";
    private int[] resourceValues;
    private boolean[] resourceOccupied;

    public ScoutGame() {
        allWorkers = new ArrayList<>();

        setTitle("simulacrum");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gd.setFullScreenWindow(this);

        setVisible(true);

        int screenWidth = getWidth();
        int screenHeight = getHeight();
        blueBaseX = 100;
        blueBaseY = screenHeight / 2 - baseHeight / 2;
        redBaseX = screenWidth - 100 - baseWidth;
        redBaseY = screenHeight / 2 - baseHeight / 2;

        int bodyRadius = 5;

        blueScout = new Scout(blueBaseX, blueBaseY, "blue", this);
        blueScout.activate();

        redScout = new Scout(redBaseX + baseWidth - 2 * bodyRadius, redBaseY, "red", this);
        redScout.angle = 180;  // Задаваме ъгъла на червения скаут наляво (180 градуса)
        redScout.activate();

        initializeResources();
        initializeWorkers();
        generateResources();

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

                // Draw bases and resources
                drawBasesAndResources(g2d, shieldRadius);
                drawWorkers(g2d);

                // Display time
                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
                int hours = (int) (elapsedTime / (1000 * 60 * 60));

                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                g2d.setColor(Color.WHITE);
                g2d.drawString(timeText, 10, 30);

                FontMetrics fm = g2d.getFontMetrics();
                int xPosition = 10 + fm.stringWidth(timeText) + 50;

                displayScores(g2d, fm, xPosition);

                if (ScoutGame.this.gameOver) {
                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                    g2d.setColor(Color.YELLOW);
                    String winnerText = ScoutGame.this.winner;
                    int winnerX = (getWidth() - fm.stringWidth(winnerText)) / 2;
                    int winnerY = getHeight() / 2;
                    g2d.drawString(winnerText, winnerX, winnerY);
                }
            }

            private void displayScores(Graphics2D g2d, FontMetrics fm, int xPosition) {
                g2d.setColor(Color.BLUE);
                g2d.drawString("" + blueBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("" + blueBaseHealth) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("" + redBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("" + redBaseHealth) + 50;

                g2d.setColor(Color.BLUE);
                g2d.drawString("" + blueScout.getPoints(), xPosition, 30);
                xPosition += fm.stringWidth("" + blueScout.getPoints()) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("" + redScout.getPoints(), xPosition, 30);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(Color.DARK_GRAY);

        JButton minimizeButton = new JButton("-");
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));
        controlPanel.add(minimizeButton);

        JButton fullscreenButton = new JButton("□");
        fullscreenButton.addActionListener(e -> {
            setUndecorated(!isUndecorated());
            setVisible(true);
            GraphicsDevice gdDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gdDevice.setFullScreenWindow(isUndecorated() ? this : null);
        });
        controlPanel.add(fullscreenButton);

        JButton closeButton = new JButton("X");
        closeButton.addActionListener(e -> System.exit(0));
        controlPanel.add(closeButton);

        add(controlPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        Timer timer = new Timer(150, e -> {
            if (blueScout.isActive()) {
                Point blueBaseCenter = new Point(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2);
                blueScout.update(blueBaseCenter, resources);
            }
            if (redScout.isActive()) {
                Point redBaseCenter = new Point(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2);
                redScout.update(redBaseCenter, resources);
            }

            moveDefenders();
            moveWorkers();
            mainPanel.repaint();
        });
        timer.start();

        setVisible(true);
    }

    private void initializeResources() {
        resources = new Resource[301];////////////////////////////////////////////////////////
        resourceValues = new int[resources.length];
        resourceOccupied = new boolean[resources.length];

        for (int i = 0; i < resources.length; i++) {
            resources[i] = new Resource(0, 0, 100);
            resourceValues[i] = 10;
            resourceOccupied[i] = false;
        }
    }

    public Point[] convertResourcesToPoints(Resource[] resources) {
        Point[] points = new Point[resources.length];
        for (int i = 0; i < resources.length; i++) {
            points[i] = new Point((int) resources[i].getX(), (int) resources[i].getY());
        }
        return points;
    }

    private void generateResources() {
        Random random = new Random();
        int panelWidth = Math.max(getContentPane().getWidth(), 800);
        int panelHeight = Math.max(getContentPane().getHeight(), 600);

        List<Point> workerPositions = new ArrayList<>();
        for (Worker worker : blueWorkers) {
            workerPositions.add(new Point(worker.getX(), worker.getY()));
        }
        for (Worker worker : redWorkers) {
            workerPositions.add(new Point(worker.getX(), worker.getY()));
        }

        for (int i = 0; i < resources.length; i++) {
            int x, y;
            boolean positionIsValid;
            do {
                x = (int) (Math.random() * (panelWidth - 2 * baseWidth)) + baseWidth;
                y = (int) (Math.random() * (panelHeight - 2 * baseHeight)) + baseHeight;
                positionIsValid = !isNearBase(x, y) && !isNearWorkers(x, y, workerPositions);
            } while (!positionIsValid);

            resources[i] = new Resource(x, y, 10000);/////////////////////////////////////////////////////
        }
    }

    private boolean isNearWorkers(int x, int y, List<Point> workerPositions) {
        int minDistance = 50;
        for (Point workerPos : workerPositions) {
            if (distance(x, y, workerPos.x, workerPos.y) < minDistance) {
                return true;
            }
        }
        return false;
    }

    private void initializeWorkers() {
        int totalWorkers = 100;/////////////////////////////////////////////////////////////////////
        int workersPerColumn = 10;

        blueWorkers = new Worker[totalWorkers];
        redWorkers = new Worker[totalWorkers];

        int columnSpacing = 25;
        int rowSpacing = 25;

        for (int i = 0; i < totalWorkers; i++) {
            int columnIndex = i / workersPerColumn;
            int rowIndex = i % workersPerColumn;

            // Пример за инициализиране на сините работници с директно подаване на Resource[]
            blueWorkers[i] = new Worker(
                    blueBaseX + baseWidth / 2 + columnIndex * columnSpacing,
                    blueBaseY + baseHeight + 100 + rowIndex * rowSpacing,
                    "blue",
                    resources, // Подаване на Resource[] директно вместо Point[]
                    resourceValues,
                    resourceOccupied,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );

// И същото за червените работници
            redWorkers[i] = new Worker(
                    redBaseX + baseWidth / 2 - columnIndex * columnSpacing,
                    redBaseY + baseHeight + 100 + rowIndex * rowSpacing,
                    "red",
                    resources, // Подаване на Resource[] директно вместо Point[]
                    resourceValues,
                    resourceOccupied,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );


            redWorkers[i].angle = 180;

            allWorkers.add(blueWorkers[i]);
            allWorkers.add(redWorkers[i]);
        }
    }

    private boolean isNearBase(int x, int y) {
        int blueBaseCenterX = blueBaseX + baseWidth / 2;
        int blueBaseCenterY = blueBaseY + baseHeight / 2;
        int redBaseCenterX = redBaseX + baseWidth / 2;
        int redBaseCenterY = redBaseY + baseHeight / 2;
        int minDistance = 200;

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
        g2d.setColor(new Color(0, 100, 200));
        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.BLUE);
        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);

        g2d.setColor(new Color(0, 0, 255, 100));
        g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        g2d.setColor(new Color(200, 50, 50));
        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);

        g2d.setColor(new Color(255, 0, 0, 100));
        g2d.drawOval(redBaseX - (shieldRadius - baseWidth) / 2, redBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        for (Resource resource : resources) {
            g2d.setColor(resource.getValue() <= 0 ? new Color(169, 169, 169) : new Color(255, 223, 0));
            g2d.fillOval((int) resource.getX() - 20, (int) resource.getY() - 20, 40, 40);
            g2d.setColor(Color.BLACK);
            g2d.drawOval((int) resource.getX() - 20, (int) resource.getY() - 20, 40, 40);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString(String.valueOf(resource.getValue()), (int) resource.getX() - 10, (int) resource.getY() + 5);
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

        int bodyRadius = 5;  // Стандартен радиус за работници и скаути
        int lineLength;

        // Определяме типа на обекта и променяме радиуса и цвета съответно
        if (ant instanceof Scout) {
            lineLength = bodyRadius * 2;  // За скаутите дължината на чертичката е диаметърът
            g2d.setColor(ant.team.equals("blue") ? Color.BLUE : Color.RED);  // Скаутите са сини или червени, в зависимост от отбора
        } else if (ant instanceof Worker) {
            lineLength = bodyRadius;  // За работниците чертичката е колкото радиуса
            g2d.setColor(ant.team.equals("blue") ? new Color(0, 100, 255) : new Color(200, 50, 50));  // Работниците съответстват на синия или червения цвят на базата си
        } else if (ant instanceof Defender) {
            bodyRadius *= 1.5;  // Охранителите са два пъти по-големи
            lineLength = bodyRadius;  // Чертичката на охранителите е колкото радиуса им
            g2d.setColor(ant.team.equals("blue") ? new Color(0, 0, 180) : new Color(180, 0, 0));  // Охранителите са тъмно сини или тъмно червени
        } else {
            return;  // Ако обектът не е от разпознат тип, не го рисуваме
        }

        // Рисуване на тялото на обекта
        g2d.fillOval(ant.getX() - bodyRadius, ant.getY() - bodyRadius, bodyRadius * 2, bodyRadius * 2);

        // Начална точка за чертичката (центърът на обекта)
        int x1 = ant.getX();
        int y1 = ant.getY();

        // Изчисляване на крайна точка на чертичката на база на текущия ъгъл
        int x2 = x1 + (int) (lineLength * Math.cos(Math.toRadians(ant.angle)));
        int y2 = y1 + (int) (lineLength * Math.sin(Math.toRadians(ant.angle)));

        // Рисуване на чертичката - жълта за работници и охранители, зелена за скаути
        if (ant instanceof Scout) {
            g2d.setColor(Color.GREEN);
        } else {
            g2d.setColor(Color.YELLOW);  // Жълта чертичка за работници и охранители
        }
        g2d.drawLine(x1, y1, x2, y2);
    }







    private void scheduleWorkerStarts() {
        int initialDelay = 30000;
        int interval = 30000;

        for (int i = 0; i < blueWorkers.length; i++) {
            int delay = initialDelay + (i * interval);
            final int workerIndex = i;

            Timer blueWorkerTimer = new Timer(delay, e -> {
                blueWorkers[workerIndex].activate();
                System.out.println("Activated blue worker " + (workerIndex + 1) + " after " + delay / 1000 + " seconds.");
                ((Timer) e.getSource()).stop();
            });
            blueWorkerTimer.setRepeats(false);
            blueWorkerTimer.start();

            Timer redWorkerTimer = new Timer(delay, e -> {
                redWorkers[workerIndex].activate();
                System.out.println("Activated red worker " + (workerIndex + 1) + " after " + delay / 1000 + " seconds.");
                ((Timer) e.getSource()).stop();
            });
            redWorkerTimer.setRepeats(false);
            redWorkerTimer.start();
        }
    }

    private void moveWorkers() {
        if (gameOver) return;

        boolean anyActiveWorkers = false;

        for (Worker worker : blueWorkers) {
            if (worker != null) {
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, redScout); // подаваме resources директно
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        for (Worker worker : redWorkers) {
            if (worker != null) {
                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout); // подаваме resources директно
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }


        if (!anyActiveWorkers && allWorkersStarted() && allResourcesDepleted() && !gameOver) {
            gameOver = true;
            determineWinner();
        }
    }

    public boolean allResourcesDepleted() {
        for (Resource resource : resources) {
            if (resource.getValue() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean allWorkersStarted() {
        for (Worker worker : blueWorkers) {
            if (worker != null && !worker.hasStarted()) {
                return false;
            }
        }
        for (Worker worker : redWorkers) {
            if (worker != null && !worker.hasStarted()) {
                return false;
            }
        }
        return true;
    }

    private void determineWinner() {
        if (blueBaseHealth > redBaseHealth) {
            winner = "Синият отбор печели!";
        } else if (redBaseHealth > blueBaseHealth) {
            winner = "Червеният отбор печели!";
        } else {
            winner = "Равенство!";
        }
        gameOver = true;
        System.out.println("Играта приключи. " + winner);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScoutGame::new);
    }

    public List<Worker> getAllWorkers() {
        return allWorkers;
    }

    public int getBlueBaseHealth() {
        return blueBaseHealth;
    }

    public void setBlueBaseHealth(int health) {
        this.blueBaseHealth = health;
    }

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

    public Worker findClosestEnemyWorkerWithinRange(Scout scout, String scoutTeam, double maxRange) {
        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
        Worker closestWorker = null;
        double closestDistance = maxRange;

        for (Worker worker : enemyWorkers) {
            if (!worker.isActive()) {
                System.out.println("Skipping inactive worker at (" + worker.getX() + ", " + worker.getY() + ")");
                continue;
            }

            double distance = scout.distanceTo(worker);
            System.out.println("Worker at (" + worker.getX() + ", " + worker.getY() + ") is at distance: " + distance);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestWorker = worker;
                System.out.println("New closest worker found at (" + worker.getX() + ", " + worker.getY() + ") with distance: " + closestDistance);
            }
        }

        if (closestWorker != null) {
            System.out.println("Closest worker to scout is at (" + closestWorker.getX() + ", " + closestWorker.getY() + ") with distance: " + closestDistance);
        } else {
            System.out.println("No workers found within range.");
        }

        return closestWorker;
    }

//    public Worker findClosestEnemyWorker(Scout scout, String scoutTeam) {
//        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
//        Worker closestWorker = null;
//        double closestDistance = Double.MAX_VALUE;
//
//        for (Worker worker : enemyWorkers) {
//            if (!worker.isActive()) continue;
//            double distance = scout.distanceTo(worker);
//            if (distance < closestDistance) {
//                closestDistance = distance;
//                closestWorker = worker;
//            }
//        }
//
//        return closestWorker;
//    }




}
