package classesSeparated;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

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
    private List<ExplosionEffect> explosionEffects = new ArrayList<>();
    private Soldier[] blueSoldiers;
    private Soldier[] redSoldiers;
    private boolean artilleryCalled = false;
    private Artillery artillery;
    private int baseShieldPointsRed = 100000;/////////////////////////////////////////////////////////////////////////////
    private int baseShieldPointsBlue = 100000;////////////////////////////////////////////////////////////////////////////
    private boolean blueShieldBlinking = false;
    private boolean redShieldBlinking = false;
    private int blueShieldBlinkState = 0;
    private int redShieldBlinkState = 0;



    public ScoutGame() {
        allWorkers = new ArrayList<>();

        redSoldiers = new Soldier[0];
        blueSoldiers = new Soldier[0];

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

        blueScout = new Scout(blueBaseX, blueBaseY, "blue", this, 1);
        blueScout.activate();

        redScout = new Scout(redBaseX + baseWidth - 2 * bodyRadius, redBaseY, "red", this, 1);
        redScout.activate();

        initializeResources();
        initializeWorkers();
        generateResources();

        blueDefenders = new Defender[3];
        redDefenders = new Defender[3];
        initializeDefenders();

        initializeSoldiers("blue", blueBaseX, blueBaseY, blueBaseHealth);
        initializeSoldiers("red", redBaseX, redBaseY, redBaseHealth);

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
                drawExplosions(g2d);

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

                g2d.setColor(Color.BLUE);
                g2d.drawString("Base: " + blueBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("Base: " + blueBaseHealth) + 50;

                g2d.setColor(Color.RED);
                g2d.drawString("Base: " + redBaseHealth, xPosition, 30);
                xPosition += fm.stringWidth("Base: " + redBaseHealth) + 50;

                if (blueScout != null) {
                    g2d.setColor(Color.BLUE);
                    g2d.drawString("Scout: " + blueScout.getPoints() + "-" + blueScout.getKills(), xPosition, 30);
                    xPosition += fm.stringWidth("Scout: " + blueScout.getPoints() + "-" + blueScout.getKills()) + 50;
                }

                if (redScout != null) {
                    g2d.setColor(Color.RED);
                    g2d.drawString("Scout: " + redScout.getPoints() + "-" + redScout.getKills(), xPosition, 30);
                }

                if (blueScout != null) {
                    blueScout.draw(g2d);
                }

                if (redScout != null) {
                    redScout.draw(g2d);
                }

                for (Worker worker : blueWorkers) {
                    if (worker != null && worker.isActive()) {
                        worker.drawWorker(g2d);
                    }
                }

                for (Worker worker : redWorkers) {
                    if (worker != null && worker.isActive()) {
                        worker.drawWorker(g2d);
                    }
                }

                for (Defender defender : blueDefenders) {
                    if (defender != null && defender.isActive()) {
                        defender.drawDefender(g);
                        defender.drawDefenderProjectiles(g2d);
                        defender.drawDefenderWeaponDirection(g2d);
                    }
                }

                for (Defender defender : redDefenders) {
                    if (defender != null && defender.isActive()) {
                        defender.drawDefender(g);
                        defender.drawDefenderProjectiles(g2d);
                        defender.drawDefenderWeaponDirection(g2d);
                    }
                }


                if (blueSoldiers != null) {
                    for (Soldier soldier : blueSoldiers) {
                        if (soldier != null && soldier.isActive()) {
                            soldier.drawSoldier(g2d);
                        }
                    }
                }

                if (redSoldiers != null) {
                    for (Soldier soldier : redSoldiers) {
                        if (soldier != null && soldier.isActive()) {
                            soldier.drawSoldier(g2d);
                        }
                    }
                }




                if (artillery != null && artillery.isActive()) {
                    artillery.
                            drawArtillery(g2d);
                }

                if (bulletStartX != -1 && bulletStartY != -1) {
                    g2d.setColor(Color.RED);
                    g2d.drawLine(bulletStartX, bulletStartY, bulletEndX, bulletEndY);
                }

                if (ScoutGame.this.gameOver) {
                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                    g2d.setColor(Color.YELLOW);
                    String winnerText = ScoutGame.this.winner;
                    int winnerX = (getWidth() - fm.stringWidth(winnerText)) / 2;
                    int winnerY = getHeight() / 2;
                    g2d.drawString(winnerText, winnerX, winnerY);
                }
            }

            private void drawExplosions(Graphics2D g2d) {
                long currentTime = System.currentTimeMillis();
                explosionEffects.removeIf(effect -> effect.isExpired(currentTime));
                for (ExplosionEffect effect : explosionEffects) {
                    effect.draw(g2d);
                }
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
            if (blueScout != null && blueScout.isActive()) {
                Point blueBaseCenter = new Point(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2);
                blueScout.update(blueBaseCenter, resources);
                blueScout.updatePosition();
            }

            if (redScout != null && redScout.isActive()) {
                Point redBaseCenter = new Point(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2);
                redScout.update(redBaseCenter, resources);
                redScout.updatePosition();
            }

            if (blueSoldiers != null) {
                for (Soldier soldier : blueSoldiers) {
                    if (soldier != null) {
                        updateSoldier(soldier, redWorkers, redScout, redDefenders, blueSoldiers);
                    }
                }
            }

            if (redSoldiers != null) {
                for (Soldier soldier : redSoldiers) {
                    if (soldier != null) {
                        updateSoldier(soldier, blueWorkers, blueScout, blueDefenders, redSoldiers);
                    }
                }
            }


            moveDefenders();

            removeDeadDefenders();

            checkForAvailableResources();

            moveWorkers();

            removeDeadSoldiers();

            if (artillery != null && artillery.isActive()) {
                artillery.updateArtillery();
            }

            boolean redHasSoldiers = areSoldiersLeft("red");
            boolean blueHasSoldiers = areSoldiersLeft("blue");

            if (!artilleryCalled) {
                if (!redHasSoldiers && blueHasSoldiers) {
                    Artillery art = new Artillery(
                            blueBaseX + baseWidth / 2,
                            blueBaseY + baseHeight / 2,
                            redBaseX + baseWidth / 2,
                            redBaseY + baseHeight / 2,
                            "blue", this
                    );
                    this.artillery = art;
                    artilleryCalled = true;

                    double angleToEnemyBase = calculateAngleTo(blueBaseX, blueBaseY, redBaseX, redBaseY);
                    double artX = artillery.getX();
                    double artY = artillery.getY();

                    double spacing = 20.0;
                    for (int i = 0; i < blueSoldiers.length; i++) {
                        if (blueSoldiers[i] != null && blueSoldiers[i].isActive()) {
                            double offset = (i + 1) * spacing;
                            double soldierX = artX - offset * Math.cos(Math.toRadians(angleToEnemyBase));
                            double soldierY = artY - offset * Math.sin(Math.toRadians(angleToEnemyBase));

                            blueSoldiers[i].setX(soldierX);
                            blueSoldiers[i].setY(soldierY);
                            blueSoldiers[i].setCurrentAngle(angleToEnemyBase);
                            blueSoldiers[i].setWaiting(true);
                        }
                    }

                } else if (!blueHasSoldiers && redHasSoldiers) {
                    Artillery art = new Artillery(
                            redBaseX + baseWidth / 2,
                            redBaseY + baseHeight / 2,
                            blueBaseX + baseWidth / 2,
                            blueBaseY + baseHeight / 2,
                            "red", this
                    );
                    this.artillery = art;
                    artilleryCalled = true;

                    double angleToEnemyBase = calculateAngleTo(redBaseX, redBaseY, blueBaseX, blueBaseY);
                    double artX = artillery.getX();
                    double artY = artillery.getY();

                    double spacing = 20.0;
                    for (int i = 0; i < redSoldiers.length; i++) {
                        if (redSoldiers[i] != null && redSoldiers[i].isActive()) {
                            double offset = (i + 1) * spacing;
                            double soldierX = artX - offset * Math.cos(Math.toRadians(angleToEnemyBase));
                            double soldierY = artY - offset * Math.sin(Math.toRadians(angleToEnemyBase));

                            redSoldiers[i].setX(soldierX);
                            redSoldiers[i].setY(soldierY);
                            redSoldiers[i].setCurrentAngle(angleToEnemyBase);
                            redSoldiers[i].setWaiting(true);
                        }
                    }
                }
            }

            mainPanel.repaint();

        });

        timer.start();

        setVisible(true);
    }

    private void removeDeadDefenders() {
        for (int i = 0; i < blueDefenders.length; i++) {
            if (blueDefenders[i] != null && !blueDefenders[i].isActive()) {
                blueDefenders[i] = null; // Премахване на мъртвия защитник
            }
        }

        for (int i = 0; i < redDefenders.length; i++) {
            if (redDefenders[i] != null && !redDefenders[i].isActive()) {
                redDefenders[i] = null; // Премахване на мъртвия защитник
            }
        }
    }


    private double calculateAngleTo(double x1, double y1, double x2, double y2) {
        return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    private boolean areSoldiersLeft(String team) {
        for (Character c : getCharacters()) {
            if (c instanceof Soldier && c.getTeam().equals(team) && c.isActive()) {
                return true;
            }
        }
        return false;
    }

    private void updateSoldier(Soldier soldier, Worker[] enemyWorkers, Scout enemyScout, Defender[] enemyDefenders, Soldier[] teammates) {
        if (soldier.isWaiting()) {
            return;
        }

        // Обновяване на състоянието на войника с предадените съотборници
        soldier.updateSoldier(teammates);

        boolean targetFound = false;

        // Проверка за работници на врага
        for (Worker enemy : enemyWorkers) {
            if (enemy != null && enemy.isActive() &&
                    distance(soldier.getX(), soldier.getY(), enemy.getX(), enemy.getY()) <= soldier.getWeaponLength()) {
                soldier.soldierShoot(enemy);
                targetFound = true;
                break;
            }
        }

        // Проверка за скаути на врага
        if (!targetFound && enemyScout != null && enemyScout.isActive() &&
                distance(soldier.getX(), soldier.getY(), enemyScout.getX(), enemyScout.getY()) <= soldier.getWeaponLength()) {
            soldier.soldierShoot(enemyScout);
            targetFound = true;
        }

        // Проверка за защитници на врага
        if (!targetFound && enemyDefenders != null) {
            for (Defender defender : enemyDefenders) {
                if (defender != null && defender.isActive() &&
                        distance(soldier.getX(), soldier.getY(), defender.getX(), defender.getY()) <= soldier.getWeaponLength()) {
                    soldier.soldierShoot(defender);
                    targetFound = true;
                    break;
                }
            }
        }

        // Ако няма цел, войникът се придвижва към центъра
        if (!targetFound) {
            soldier.soldierMoveTowardsCenter(teammates);
        }
    }

    private void checkForAvailableResources() {
        for (Worker worker : allWorkers) {
            if (worker.hasStarted() && !worker.isActive()) {
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, null);
            }
        }
    }

    private void initializeResources() {
        resources = new Resource[21];//////////////////////////////////////////////////////////////////////////////////
        resourceValues = new int[resources.length];
        resourceOccupied = new boolean[resources.length];

        for (int i = 0; i < resources.length; i++) {
            resources[i] = new Resource(0, 0, 5000);
            resourceValues[i] = 100;
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
                y = (int) (Math.random() * (panelHeight -2  * baseHeight)) + baseHeight;
                positionIsValid = !isNearBase(x, y) && !isNearWorkers(x, y, workerPositions);
            } while (!positionIsValid);

            resources[i] = new Resource(x, y, 15);////////////////////////////////////////////////////////////////
        }
    }

    private void initializeWorkers() {
        int totalWorkers = 10;///////////////////////////////////////////////////////////////////////////////////////////
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

    private boolean isNearWorkers(double x, double y, List<Point2D.Double> workerPositions) {
        int minDistance = 50;
        for (Point2D.Double workerPos : workerPositions) {
            if (distance(x, y, workerPos.x, workerPos.y) < minDistance) {
                return true;
            }
        }
        return false;
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
                    this,
                    i * Math.PI / 4
            );

            redDefenders[i] = new Defender(
                    redBaseX + baseWidth / 2,
                    redBaseY + baseHeight / 2,
                    "red",
                    "defender",
                    this,
                    i * Math.PI / 4
            );
        }
    }

    private void moveDefenders() {
        updateDefenders(blueDefenders, redScout, redSoldiers, blueBaseX, blueBaseY);
        updateDefenders(redDefenders, blueScout, blueSoldiers, redBaseX, redBaseY);
    }

    private void updateDefenders(Defender[] defenders, Scout enemyScout, Soldier[] enemySoldiers, int baseX, int baseY) {
        if (defenders == null || defenders.length == 0) return;

        for (Defender defender : defenders) {
            if (defender != null) {
                // Update defender patrolling around the base
                defender.patrolAroundBase(baseX + baseWidth / 2, baseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);

                // Check and shoot at the enemy scout if in range
                if (enemyScout != null && enemyScout.isActive()) {
                    defender.checkAndShootIfScoutInRange(enemyScout);
                    defender.updateProjectiles(enemyScout); // Update projectiles targeting the scout
                }

                // Check and shoot at active enemy soldiers if any are in range
                if (enemySoldiers != null && enemySoldiers.length > 0) {
                    ArrayList<Soldier> activeSoldiers = new ArrayList<>();

                    // Filter active enemy soldiers
                    for (Soldier soldier : enemySoldiers) {
                        if (soldier != null && soldier.isActive()) {
                            activeSoldiers.add(soldier);
                        }
                    }

                    // If there are active enemy soldiers, handle shooting and projectile updates
                    if (!activeSoldiers.isEmpty()) {
                        defender.checkAndShootIfSoldiersInRange(activeSoldiers);
                        defender.updateProjectilesForSoldier(activeSoldiers); // Update projectiles targeting soldiers
                    }
                }
            }
        }
    }

    private void drawBasesAndResources(Graphics2D g2d, int shieldRadius) {
        // Рисуване на синята база
        g2d.setColor(new Color(0, 100, 200));
        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.BLUE);
        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);

        // Рисуване на щита на синята база, ако има останали точки
        if (baseShieldPointsBlue > 0) {
            if (blueShieldBlinking && blueShieldBlinkState % 2 == 0) {
                g2d.setColor(new Color(0, 255, 255, 200)); // По-ярък цвят при премигване
            } else {
                g2d.setColor(new Color(0, 0, 255, 200));
            }
            g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

            // Показване на точките на щита на синята база
            g2d.setColor(Color.BLUE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("" + baseShieldPointsBlue, blueBaseX, blueBaseY - 25);
        }

        // Рисуване на червената база
        g2d.setColor(new Color(236, 8, 8));
        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);

        // Рисуване на щита на червената база, ако има останали точки
        if (baseShieldPointsRed > 0) {
            if (redShieldBlinking && redShieldBlinkState % 2 == 0) {
                g2d.setColor(new Color(227, 35, 18, 200)); // По-ярък цвят при премигване
            } else {
                g2d.setColor(new Color(234, 5, 5, 229));
            }
            g2d.drawOval(redBaseX - (shieldRadius - baseWidth) / 2, redBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);

            // Показване на точките на щита на червената база
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("" + baseShieldPointsRed, redBaseX, redBaseY - 25);
        }

        // Рисуване на ресурсите
        for (Resource resource : resources) {
            g2d.setColor(resource.getValue() <= 0 ? new Color(105, 105, 105) : new Color(150, 135, 10)); // Darker gray for empty resources
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

            if (ant.team.equals("blue")) {
                g2d.setColor(Color.BLUE);
            } else {
                g2d.setColor(Color.RED);
            }

            g2d.fillOval((int) (ant.getX() - bodyRadius), (int) (ant.getY() - bodyRadius), bodyRadius * 2, bodyRadius * 2);

            Worker worker = (Worker) ant;
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.valueOf(worker.getId()), (int) worker.getX() - 5, (int) worker.getY() - 10);

            if (ant.team.equals("blue")) {
                g2d.setColor(Color.BLUE);
            } else {
                g2d.setColor(Color.RED);
            }
        } else if (ant instanceof Defender) {
            bodyRadius *= 1.5;
            lineLength = bodyRadius;
            g2d.setColor(ant.team.equals("blue") ? new Color(0, 0, 180) : new Color(180, 0, 0)); // По-тъмен син за "blue" защитници, по-тъмен червен за "red"
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
        } else if (ant instanceof Worker || ant instanceof Defender) {
            g2d.setColor(Color.YELLOW);
        }

        g2d.drawLine(x1, y1, x2, y2);
    }

    private void scheduleWorkerStarts() {
        int initialDelay = 30000;
        int interval = 30000;

        Timer timer = new Timer(interval, new AbstractAction() {
            int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (index < blueWorkers.length && blueWorkers[index] != null) {
                    blueWorkers[index].activate();
                }

                if (index < redWorkers.length && redWorkers[index] != null) {
                    redWorkers[index].activate();
                }

                index++;

                if (index >= Math.max(blueWorkers.length, redWorkers.length)) {
                    ((Timer) e.getSource()).stop();
                }
            }
        });

        timer.setInitialDelay(initialDelay);
        timer.start();
    }

    private void moveWorkers() {
        boolean anyActiveWorkers = false;

        boolean resourcesDepleted = allResourcesDepleted();
        boolean allBlueWorkersAtStart = allWorkersAtStartPosition(blueWorkers, blueBaseX, blueBaseY);
        boolean allRedWorkersAtStart = allWorkersAtStartPosition(redWorkers, redBaseX, redBaseY);

        for (Worker worker : blueWorkers) {
            if (worker != null) {
                if (resourcesDepleted && !worker.isAtStartPosition(blueBaseX, blueBaseY)) {
                    worker.moveToStartPosition(blueBaseX, blueBaseY);
                }
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, redScout);
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        for (Worker worker : redWorkers) {
            if (worker != null) {
                if (resourcesDepleted && !worker.isAtStartPosition(redBaseX, redBaseY)) {
                    worker.moveToStartPosition(redBaseX, redBaseY);
                }
                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout);
                if (worker.isActive()) {
                    anyActiveWorkers = true;
                }
            }
        }

        if (!anyActiveWorkers) {
            //System.out.println("No active workers remaining.");
        }

        if (resourcesDepleted && allBlueWorkersAtStart && allRedWorkersAtStart) {
            //System.out.println("Resources depleted and workers returned. Creating soldiers...");

            boolean soldiersCreated = false;

            if (blueBaseHealth >= 5) {
                startSoldierCreation("blue", blueBaseX, blueBaseY);
                soldiersCreated = true;
            } else {
                //System.out.println("Not enough points to create soldiers for team blue.");
            }

            if (redBaseHealth >= 5) {
                startSoldierCreation("red", redBaseX, redBaseY);
                soldiersCreated = true;
            } else {
                //System.out.println("Not enough points to create soldiers for team red.");
            }

            if (soldiersCreated) {
                removeWorkers(blueWorkers);
                removeWorkers(redWorkers);
            }
        }
    }

    private boolean allWorkersAtStartPosition(Worker[] workers, int baseX, int baseY) {
        for (Worker worker : workers) {
            if (worker != null && !worker.isAtStartPosition(baseX, baseY)) {
                return false;
            }
        }
        return true;
    }

    private Soldier[] addSoldierToArray(Soldier[] array, Soldier soldier) {
        if (array == null) {
            array = new Soldier[0];
        }
        Soldier[] newArray = new Soldier[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = soldier;
        return newArray;
    }

    private void initializeSoldiers(String team, int baseX, int baseY, int baseHealth) {
        final int soldierHealthCost = 5;
        final int maxRowsPerColumn = 10;
        final int columnSpacing = 30;
        final int rowSpacing = 30;
        final int targetY = baseY - 200;

        int maxSoldiers = baseHealth / soldierHealthCost;
        if (maxSoldiers <= 0) {
            return;
        }

        Soldier[] soldiers = new Soldier[maxSoldiers];

        for (int i = 0; i < maxSoldiers; i++) {
            int columnIndex = i / maxRowsPerColumn;
            int rowIndex = i % maxRowsPerColumn;
            int x = baseX + (team.equals("blue") ? columnIndex * columnSpacing : -columnIndex * columnSpacing);
            int y = targetY + rowIndex * rowSpacing;

            soldiers[i] = new Soldier(
                    x,
                    y,
                    team,
                    baseX,
                    baseY,
                    team.equals("blue") ? redBaseX : blueBaseX,
                    team.equals("blue") ? redBaseY : blueBaseY,
                    this,
                    i + 1
            );

        }

        int pointsUsed = maxSoldiers * soldierHealthCost;
        if (team.equals("blue")) {
            blueSoldiers = soldiers;
            blueBaseHealth -= pointsUsed;
        } else {
            redSoldiers = soldiers;
            redBaseHealth -= pointsUsed;
        }

    }

    private void startSoldierCreation(String team, int baseX, int baseY) {
        final int soldierCost = 1; /////////////////////////////////////////////////////////////////////////////////////
        final int maxRowsPerColumn = 12;
        final int columnSpacing = 30;
        final int rowSpacing = 30;
        final int targetY = baseY - 200;
        int baseHealth = team.equals("blue") ? blueBaseHealth : redBaseHealth;
        int maxSoldiers = baseHealth / soldierCost;

        if (maxSoldiers <= 0) {
            return;
        }

        for (int soldiersCreated = 0; soldiersCreated < maxSoldiers; soldiersCreated++) {
            int columnIndex = soldiersCreated / maxRowsPerColumn;
            int rowIndex = soldiersCreated % maxRowsPerColumn;

            int x = baseX + (team.equals("blue") ? columnIndex * columnSpacing : -columnIndex * columnSpacing);
            int y = targetY + rowIndex * rowSpacing;

            int enemyBaseX = team.equals("blue") ? redBaseX : blueBaseX;
            int enemyBaseY = team.equals("blue") ? redBaseY : blueBaseY;

            Soldier soldier = new Soldier(
                    x,
                    y,
                    team,
                    baseX,
                    baseY,
                    enemyBaseX,
                    enemyBaseY,
                    ScoutGame.this,
                    soldiersCreated + 1
            );

            if (team.equals("blue")) {
                blueSoldiers = addSoldierToArray(blueSoldiers, soldier);
                blueBaseHealth -= soldierCost;
            } else {
                redSoldiers = addSoldierToArray(redSoldiers, soldier);
                redBaseHealth -= soldierCost;
            }
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

    public boolean allWorkersAtBase(Worker[] workers, int baseX, int baseY) {
        for (Worker worker : workers) {
            if (worker != null) {
                //System.out.println("Worker " + worker.getId() + " position: (" + worker.getX() + ", " + worker.getY() + ")");
                //System.out.println("Checking if worker is at base (" + baseX + ", " + baseY + ")");
                if (!worker.isAtBase(baseX, baseY)) {
                    //System.out.println("Worker " + worker.getId() + " is NOT at base.");
                    return false;
                }
            }
        }

        return true;
    }

    private void removeDeadSoldiers() {
        blueSoldiers = Arrays.stream(blueSoldiers)
                .filter(soldier -> soldier != null && soldier.isActive())
                .toArray(Soldier[]::new);

        redSoldiers = Arrays.stream(redSoldiers)
                .filter(soldier -> soldier != null && soldier.isActive())
                .toArray(Soldier[]::new);
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

        if (enemyWorkers == null) {
            return null;
        }

        for (Worker worker : enemyWorkers) {
            if (worker == null || !worker.isActive()) {
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

    public List<Worker> getEnemyWorkersInRange(Scout scout, String scoutTeam, double range) {
        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
        List<Worker> nearbyWorkers = new ArrayList<>();

        if (enemyWorkers == null) {
            return nearbyWorkers;
        }

        for (Worker worker : enemyWorkers) {
            if (worker == null || !worker.isActive()) {
                continue;
            }
            if (scout.distanceTo(worker) <= range) {
                nearbyWorkers.add(worker);
            }
        }
        return nearbyWorkers;
    }

    public void addExplosionEffect(double x, double y, int radius, Color color, int duration) {
        explosionEffects.add(new ExplosionEffect(x, y, radius, color, duration));
    }

    public void drawShot(int startX, int startY, int endX, int endY) {
        this.bulletStartX = startX;
        this.bulletStartY = startY;
        this.bulletEndX = endX;
        this.bulletEndY = endY;
        repaint();

        Timer timer = new Timer(150, e -> {
            bulletStartX = bulletStartY = bulletEndX = bulletEndY = -1;
            repaint();
            ((Timer) e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void removeWorkers(Worker[] workers) {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = null;
        }

        if (redScout != null) {
            redScout = null;
        }
        if (blueScout != null) {
            blueScout = null;
        }
    }

    public List<Character> getCharacters() {
        List<Character> characters = new ArrayList<>();

        if (blueSoldiers != null) {
            for (Soldier soldier : blueSoldiers) {
                if (soldier != null && soldier.isActive()) {
                    characters.add(soldier);
                }
            }
        }

        if (redSoldiers != null) {
            for (Soldier soldier : redSoldiers) {
                if (soldier != null && soldier.isActive()) {
                    characters.add(soldier);
                }
            }
        }

        for (Worker worker : blueWorkers) {
            if (worker != null) {
                characters.add(worker);
            }
        }

        for (Worker worker : redWorkers) {
            if (worker != null) {
                characters.add(worker);
            }
        }

        for (Defender defender : blueDefenders) {
            if (defender != null) {
                characters.add(defender);
            }
        }

        for (Defender defender : redDefenders) {
            if (defender != null) {
                characters.add(defender);
            }
        }

        if (blueScout != null) {
            characters.add(blueScout);
        }

        if (redScout != null) {
            characters.add(redScout);
        }

        return characters;
    }

    public double getBaseShieldRadius() {
        return Math.max(baseWidth, baseHeight) * 1.5;
    }

    public void reduceShieldPoints(String team, int points) {
        if ("red".equalsIgnoreCase(team)) {
            baseShieldPointsRed -= points;
            if (baseShieldPointsRed < 0) baseShieldPointsRed = 0;

            // Стартиране на премигване за червения щит
            redShieldBlinking = true;
            redShieldBlinkState = 0;

            Timer redBlinkTimer = new Timer(200, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    redShieldBlinkState++;
                    repaint(); // Задължително извикване за обновяване на екрана
                    if (redShieldBlinkState >= 6) { // 3 премигвания (6 състояния)
                        redShieldBlinking = false;
                        ((Timer) e.getSource()).stop();
                    }
                }
            });
            redBlinkTimer.start();
        } else if ("blue".equalsIgnoreCase(team)) {
            baseShieldPointsBlue -= points;
            if (baseShieldPointsBlue < 0) baseShieldPointsBlue = 0;

            // Стартиране на премигване за синия щит
            blueShieldBlinking = true;
            blueShieldBlinkState = 0;

            Timer blueBlinkTimer = new Timer(500, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    blueShieldBlinkState++;
                    repaint(); // Задължително извикване за обновяване на екрана
                    if (blueShieldBlinkState >= 6) { // 3 премигвания (6 състояния)
                        blueShieldBlinking = false;
                        ((Timer) e.getSource()).stop();
                    }
                }
            });
            blueBlinkTimer.start();
        }
    }

    public Defender[] getDefenders() {
        // Обединяване на двата масива
        Defender[] allDefenders = new Defender[blueDefenders.length + redDefenders.length];
        System.arraycopy(blueDefenders, 0, allDefenders, 0, blueDefenders.length);
        System.arraycopy(redDefenders, 0, allDefenders, blueDefenders.length, redDefenders.length);

        // Филтриране на null стойности
        return Arrays.stream(allDefenders)
                .filter(Objects::nonNull) // Премахване на null елементи
                .toArray(Defender[]::new); // Преобразуване обратно към масив
    }

    public int getBaseShieldPoints(String team) {
        if ("blue".equalsIgnoreCase(team)) {
            return baseShieldPointsBlue;
        } else if ("red".equalsIgnoreCase(team)) {
            return baseShieldPointsRed;
        }
        return 0;
    }

}