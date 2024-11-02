package classesSeparated;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static java.awt.geom.Point2D.distance;

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

    private int bulletStartX = -1;
    private int bulletStartY = -1;
    private int bulletEndX = -1;
    private int bulletEndY = -1;


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

                // Рисуване на базите и ресурсите
                drawBasesAndResources(g2d, shieldRadius);

                // Рисуване на работниците и скаутите
                drawWorkers(g2d);

                // Показване на времето
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

                // Показване на резултатите за базите
                g2d.setColor(Color.BLUE);
                g2d.drawString("Base: " + blueBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("Base: " + blueBaseHealth) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("Base: " + redBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("Base: " + redBaseHealth) + 50;

                // Показване на резултатите за точките и убийствата на скаутите
                g2d.setColor(Color.BLUE);
                g2d.drawString("Scout: " + blueScout.getPoints() + "-" + blueScout.getKills(), xPosition, 30);
                xPosition += fm.stringWidth("Scout: " + blueScout.getPoints() + "-" + blueScout.getKills()) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("Scout: " + redScout.getPoints() + "-" + redScout.getKills(), xPosition, 30);

                // Рисуване на патрона, ако е активен
                if (bulletStartX != -1 && bulletStartY != -1) {
                    g2d.setColor(Color.GREEN);  // Зеленият цвят за патрона
                    g2d.drawLine(bulletStartX, bulletStartY, bulletEndX, bulletEndY);
                }

                // Показване на съобщението за победа, ако играта е приключила
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
                String blueScoreText = blueScout.getPoints() + "- " + blueScout.getKills();
                g2d.drawString(blueScoreText, xPosition, 30);
                xPosition += fm.stringWidth(blueScoreText) + 50;

                g2d.setColor(Color.RED);
                String redScoreText = redScout.getPoints() + "- " + redScout.getKills();
                g2d.drawString(redScoreText, xPosition, 30);
                xPosition += fm.stringWidth(redScoreText) + 50;
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
        resources = new Resource[121];/////////////////////////////////////////////////////////////////////////////////
        resourceValues = new int[resources.length];
        resourceOccupied = new boolean[resources.length];

        for (int i = 0; i < resources.length; i++) {
            resources[i] = new Resource(0, 0, 5000);
            resourceValues[i] = 5000;//////////////////////////////////////////////////////////////////////////////////
            resourceOccupied[i] = false;
        }
    }

    private void generateResources() {
        Random random = new Random();
        int panelWidth = Math.max(getContentPane().getWidth(), 800);
        int panelHeight = Math.max(getContentPane().getHeight(), 600);

        List<Point2D.Double> workerPositions = new ArrayList<>();
        for (Worker worker : blueWorkers) {
            workerPositions.add(new Point2D.Double(worker.getX(), worker.getY()));
        }
        for (Worker worker : redWorkers) {
            workerPositions.add(new Point2D.Double(worker.getX(), worker.getY()));
        }

        for (int i = 0; i < resources.length; i++) {
            int x, y;
            boolean positionIsValid;
            do {
                x = (int) (Math.random() * (panelWidth - 2 * baseWidth)) + baseWidth;
                y = (int) (Math.random() * (panelHeight - 2 * baseHeight)) + baseHeight;
                positionIsValid = !isNearBase(x, y) && !isNearWorkers(x, y, workerPositions);
            } while (!positionIsValid);

            resources[i] = new Resource(x, y, 100);
        }
    }

    private boolean isNearWorkers(double x, double y, List<Point2D.Double> workerPositions) {
        int minDistance = 50;
        for (Point2D.Double workerPos : workerPositions) {
            if (distance(x, y, workerPos.x, workerPos.y) < minDistance) {
                return true;
            }
        }
        return false;
    }

    private void initializeWorkers() {
        int totalWorkers = 50;///////////////////////////////////////////////////////////////////////////////////////
        int workersPerColumn = 10;

        blueWorkers = new Worker[totalWorkers];
        redWorkers = new Worker[totalWorkers];

        int columnSpacing = 25;
        int rowSpacing = 25;

        for (int i = 0; i < totalWorkers; i++) {
            int columnIndex = i / workersPerColumn;
            int rowIndex = i % workersPerColumn;

            blueWorkers[i] = new Worker(
                    blueBaseX + baseWidth / 2 + columnIndex * columnSpacing,
                    blueBaseY + baseHeight + 100 + rowIndex * rowSpacing,
                    "blue",
                    resources,
                    resourceValues,
                    resourceOccupied,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );

            redWorkers[i] = new Worker(
                    redBaseX + baseWidth / 2 - columnIndex * columnSpacing,
                    redBaseY + baseHeight + 100 + rowIndex * rowSpacing,
                    "red",
                    resources,
                    resourceValues,
                    resourceOccupied,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );

            redWorkers[i].setAngle(180);
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
                    Math.PI + i * Math.PI / 4
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

        int bodyRadius = 5;
        int lineLength;

        if (ant instanceof Scout) {
            lineLength = bodyRadius * 2;
            g2d.setColor(ant.team.equals("blue") ? Color.BLUE : Color.RED);
        } else if (ant instanceof Worker) {
            lineLength = bodyRadius;
            g2d.setColor(ant.team.equals("blue") ? new Color(0, 100, 255) : new Color(200, 50, 50));
        } else if (ant instanceof Defender) {
            bodyRadius *= 1.5;
            lineLength = bodyRadius;
            g2d.setColor(ant.team.equals("blue") ? new Color(0, 0, 180) : new Color(180, 0, 0));
        } else {
            return;
        }

        g2d.fillOval((int) (ant.getX() - bodyRadius), (int) (ant.getY() - bodyRadius), bodyRadius * 2, bodyRadius * 2);

        double angle = ant.getCurrentAngle();
        if (ant instanceof Defender && ant.team.equals("red") && angle == 0) {
            angle = 180;
        }

        int x1 = (int) ant.getX();
        int y1 = (int) ant.getY();
        int x2 = x1 + (int) (lineLength * Math.cos(Math.toRadians(angle)));
        int y2 = y1 + (int) (lineLength * Math.sin(Math.toRadians(angle)));

        if (ant instanceof Scout) {
            g2d.setColor(Color.GREEN);
        } else {
            g2d.setColor(Color.YELLOW);
        }

        g2d.drawLine(x1, y1, x2, y2);

        if (ant instanceof Worker) {
            Worker worker = (Worker) ant;
            if (worker.shouldDisplayPoints()) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(String.valueOf(worker.getHealth()), x1 - 10, y1 - 10);
            }
        }
    }

    private void scheduleWorkerStarts() {
        int initialDelay = 30000;
        int interval = 30000;

        for (int i = 0; i < blueWorkers.length; i++) {
            int delay = initialDelay + (i * interval);
            final int workerIndex = i;

            Timer blueWorkerTimer = new Timer(delay, e -> {
                blueWorkers[workerIndex].activate();
                ((Timer) e.getSource()).stop();
            });
            blueWorkerTimer.setRepeats(false);
            blueWorkerTimer.start();

            Timer redWorkerTimer = new Timer(delay, e -> {
                redWorkers[workerIndex].activate();
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
            winner = "Blue team wins!";
        } else if (redBaseHealth > blueBaseHealth) {
            winner = "Red team wins!";
        } else {
            winner = "No Winner!";
        }
        gameOver = true;
        System.out.println("Game Over. " + winner);
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
                continue;
            }

            double distance = scout.distanceTo(worker);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWorker = worker;
            }
        }

        return closestWorker;
    }

    public List<Worker> getWorkersOnResource(Resource resource) {
        List<Worker> workersOnResource = new ArrayList<>();
        for (Worker worker : allWorkers) {
            if (worker.isWorkingOn(resource)) {
                workersOnResource.add(worker);
            }
        }
        return workersOnResource;
    }

    public void drawShot(int startX, int startY, int endX, int endY) {
        this.bulletStartX = startX;
        this.bulletStartY = startY;
        this.bulletEndX = endX;
        this.bulletEndY = endY;
        repaint(); // Извиква `paintComponent`, за да се нарисува патронът

        // Изчистване на патрона след кратко време
        Timer timer = new Timer(50, e -> {
            bulletStartX = bulletStartY = bulletEndX = bulletEndY = -1;
            repaint(); // Принуждава екрана да се опресни, за да се премахне патрона
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

}

