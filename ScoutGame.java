package classesSeparated;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScoutGame extends JFrame {
    private int blueBaseX, blueBaseY, redBaseX, redBaseY;
    private int blueBaseHealth = 500; // Initial health of the blue base
    private int redBaseHealth = 500;  // Initial health of the red base
    private final int baseWidth = 75;
    private final int baseHeight = 75;
    private Worker[] blueWorkers;
    private Worker[] redWorkers;
    private Point[] resources;
    private int[] resourceValues;
    private boolean[] resourceOccupied;
    private int[] resourceOccupancy;
    private Defender[] blueDefenders;
    private Defender[] redDefenders;
    private Scout blueScout;
    private Scout redScout;
    private List<Worker> workers;
    private List<Worker> allWorkers;
    private long startTime;
    private final int DEFENDER_SHIELD_RADIUS = (int) (baseWidth * 1.5); // Adjust as necessary


    public ScoutGame() {
        workers = new ArrayList<>();
        allWorkers = new ArrayList<>();

        blueScout = new Scout(blueBaseX, blueBaseY, "blue", this);
        redScout = new Scout(redBaseX, redBaseY, "red", this);

        // Ensure the frame has size and is visible before generating resources
        setVisible(true);

        initializeResources();
        generateResources();

        int startX = 100;
        int startY = 100;

        for (int i = 0; i < 4; i++) {
            Worker blueWorker = new Worker(startX, startY, "blue", resources, resourceValues, resourceOccupied, resourceOccupancy, baseWidth, baseHeight, this, i + 1);
            workers.add(blueWorker);
            allWorkers.add(blueWorker);
        }

        setTitle("Ant Resources And Wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Single window state listener for restoring fullscreen on maximize
        addWindowStateListener(e -> {
            if ((e.getNewState() & Frame.NORMAL) == Frame.NORMAL) {
                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                gd.setFullScreenWindow(this); // Re-enable full screen on restore
            }
        });

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        gd.setFullScreenWindow(this);

        int screenWidth = getWidth();
        int screenHeight = getHeight();
        blueBaseX = 100;
        blueBaseY = screenHeight - 250;
        redBaseX = screenWidth - 200;
        redBaseY = 100;

        blueWorkers = new Worker[4];
        redWorkers = new Worker[4];
        for (int i = 0; i < blueWorkers.length; i++) {
            blueWorkers[i] = new Worker(
                    blueBaseX + 80 + 30 * i,
                    blueBaseY + 80,
                    "blue",
                    resources,
                    resourceValues,
                    resourceOccupied,
                    resourceOccupancy,
                    baseWidth,
                    baseHeight,
                    this,
                    i + 1
            );
            redWorkers[i] = new Worker(
                    redBaseX - 80 - 30 * i,
                    redBaseY - 80,
                    "red",
                    resources,
                    resourceValues,
                    resourceOccupied,
                    resourceOccupancy,
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
                drawAnts(g2d);

                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
                int hours = (int) (elapsedTime / (1000 * 60 * 60));

                String timeText = String.format("Time: %02d:%02d:%02d", hours, minutes, seconds);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.drawString(timeText, 10, 30);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(Color.DARK_GRAY);

        // Minimize button
        JButton minimizeButton = new JButton("-");
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));
        controlPanel.add(minimizeButton);

        // Fullscreen button
        JButton fullscreenButton = new JButton("□");
        fullscreenButton.addActionListener(e -> {
            setUndecorated(!isUndecorated());
            setVisible(true);
            GraphicsDevice gdDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gdDevice.setFullScreenWindow(isUndecorated() ? this : null);
        });
        controlPanel.add(fullscreenButton);

        // Close button
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
            System.out.println("Calling moveAnts");
            moveDefenders();
            moveAnts();
            mainPanel.repaint();
        });
        timer.start();


        // Други настройки на конструктора...
        setVisible(true);

        setVisible(true);
    }

    private void initializeResources() {
        resources = new Point[8];
        resourceValues = new int[resources.length];
        resourceOccupied = new boolean[resources.length];
        resourceOccupancy = new int[resources.length];

        for (int i = 0; i < resources.length; i++) {
            resourceValues[i] = 500;
            resourceOccupied[i] = false;
            resourceOccupancy[i] = 0;
        }
    }

    private void generateResources() {
        Random random = new Random();
        int panelWidth = Math.max(getContentPane().getWidth(), 800);
        int panelHeight = Math.max(getContentPane().getHeight(), 600);

        for (int i = 0; i < resources.length; i++) {
            int x, y;
            do {
                x = random.nextInt(panelWidth - 100) + 50;
                y = random.nextInt(panelHeight - 100) + 50;
            } while (isNearBase(x, y));

            resources[i] = new Point(x, y);
        }
    }

    private boolean isNearBase(int x, int y) {
        int blueBaseCenterX = blueBaseX + baseWidth / 2;
        int blueBaseCenterY = blueBaseY + baseHeight / 2;
        int redBaseCenterX = redBaseX + baseWidth / 2;
        int redBaseCenterY = redBaseY + baseHeight / 2;
        int minDistance = 250;

        return distance(x, y, blueBaseCenterX, blueBaseCenterY) < minDistance ||
                distance(x, y, redBaseCenterX, redBaseCenterY) < minDistance;
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
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
        // Blue base
        g2d.setColor(new Color(0, 100, 200));
        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.BLUE);
        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.drawString(String.valueOf(blueBaseHealth), blueBaseX + 40, blueBaseY - 10);

        g2d.setColor(new Color(0, 0, 255, 100));
        g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

        // Red base
        g2d.setColor(new Color(200, 50, 50));
        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.drawString(String.valueOf(redBaseHealth), redBaseX + 40, redBaseY + baseHeight + 15);

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

    private void drawAnts(Graphics2D g2d) {
        drawAntWithLine(g2d, blueScout);
        drawAntWithLine(g2d, redScout);

        for (Worker worker : blueWorkers) {
            drawAntWithLine(g2d, worker);
        }
        for (Worker worker : redWorkers) {
            drawAntWithLine(g2d, worker);
        }
        for (Defender defender : blueDefenders) {
            drawAntWithLine(g2d, defender);
        }
        for (Defender defender : redDefenders) {
            drawAntWithLine(g2d, defender);
        }
    }

    private void drawAntWithLine(Graphics2D g2d, Character ant) {
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
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);
            int delay = (i + 1) * 30000;

            int workerIndex = i + 1;
            Timer timer = new Timer(delay, e -> {
                worker.activate();
                System.out.println("Activated worker " + workerIndex);
                ((Timer) e.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void moveAnts() {
        for (Worker worker : blueWorkers) {
            if (worker != null) {
                System.out.println("Updating cycle for Worker " + worker.getWorkerId());
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, redScout);
            }
        }
        for (Worker worker : redWorkers) {
            if (worker != null) {
                System.out.println("Updating cycle for Worker " + worker.getWorkerId());
                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout);
            }
        }
        checkGameStatus();
    }

    private void checkGameStatus() {
        // Add logic to check game status
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScoutGame::new);
    }

    public List<Worker> getAllWorkers() {
        return allWorkers;
    }

    // In ScoutGame class

    // Getter and Setter for blueBaseHealth
    public int getBlueBaseHealth() {
        return blueBaseHealth;
    }

    public void setBlueBaseHealth(int health) {
        this.blueBaseHealth = health;
    }

    // Getter and Setter for redBaseHealth
    public int getRedBaseHealth() {
        return redBaseHealth;
    }

    public void setRedBaseHealth(int health) {
        this.redBaseHealth = health;
    }


    public void addPointsToScoutBase(String team, int points) {
        if (team.equals("blue")) {
            // Проверка дали базата има достатъчно точки
            int requiredPoints = Math.min(points, blueBaseHealth);
            blueBaseHealth -= requiredPoints;  // Намалява точките на базата
        } else if (team.equals("red")) {
            int requiredPoints = Math.min(points, redBaseHealth);
            redBaseHealth -= requiredPoints;
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
