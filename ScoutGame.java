


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

    // Край на играта
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

    private int baseShieldPointsRed = 200;////////////////////////////////////////////////////////////////////////////////
    private int baseShieldPointsBlue = 200;///////////////////////////////////////////////////////////////////////////////
    private boolean blueShieldBlinking = false;
    private boolean redShieldBlinking = false;
    private int blueShieldBlinkState = 0;
    private int redShieldBlinkState = 0;
    private Timer gameLoop;


    public ScoutGame() {
        allWorkers = new ArrayList<>();

        redSoldiers = new Soldier[0];
        blueSoldiers = new Soldier[0];

        setTitle("simulacrum_4.0_dev");
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

        Scout.SPEED = 2.4;/////////////////////////////////////////////////////////////////////////////////////////////
        blueScout.applyGlobalSpeed();
        redScout.applyGlobalSpeed();

        initializeResources();
        initializeWorkers();
        generateResources();

        blueDefenders = new Defender[4];////////////////////////////////////////////////////////////////////////////////
        redDefenders = new Defender[4];/////////////////////////////////////////////////////////////////////////////////
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

                if (blueScout != null) blueScout.draw(g2d);
                if (redScout  != null) redScout.draw(g2d);

                for (Worker worker : blueWorkers) if (worker != null && worker.isActive()) worker.drawWorker(g2d);
                for (Worker worker : redWorkers)  if (worker != null && worker.isActive())  worker.drawWorker(g2d);

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
                        if (soldier != null && soldier.isActive()) soldier.drawSoldier(g2d);
                    }
                }
                if (redSoldiers != null) {
                    for (Soldier soldier : redSoldiers) {
                        if (soldier != null && soldier.isActive()) soldier.drawSoldier(g2d);
                    }
                }

                if (artillery != null && artillery.isActive()) artillery.drawArtillery(g2d);

                if (bulletStartX != -1 && bulletStartY != -1) {
                    g2d.setColor(Color.RED);
                    g2d.drawLine(bulletStartX, bulletStartY, bulletEndX, bulletEndY);
                }

                if (ScoutGame.this.gameOver) {
                    // лек тъмен воал
                    g2d.setColor(new Color(0, 0, 0, 140));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                    g2d.setColor(Color.YELLOW);
                    String winnerText = ScoutGame.this.winner;
                    int w = g2d.getFontMetrics().stringWidth(winnerText);
                    int winnerX = (getWidth() - w) / 2;
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

        JButton fullscreenButton = new JButton("□");
        fullscreenButton.addActionListener(e -> {
            setUndecorated(!isUndecorated());
            setVisible(true);
            GraphicsDevice gdDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gdDevice.setFullScreenWindow(isUndecorated() ? this : null);
        });

        JButton closeButton = new JButton("X");
        closeButton.addActionListener(e -> System.exit(0));

        add(controlPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        gameLoop = new Timer(150, e -> {
            if (gameOver) {
                return;
            }

            if (blueScout != null && blueScout.isActive()) {
                Point blueBaseCenter = new Point(blueBaseX + baseWidth / 2, blueBaseY + baseHeight / 2);
                blueScout.update(blueBaseCenter, resources);
            }

            if (redScout != null && redScout.isActive()) {
                Point redBaseCenter = new Point(redBaseX + baseWidth / 2, redBaseY + baseHeight / 2);
                redScout.update(redBaseCenter, resources);
            }

            if (blueSoldiers != null) {
                for (Soldier soldier : blueSoldiers) {
                    if (soldier != null) {
                        updateSoldier(soldier, redWorkers, redScout, redDefenders, blueSoldiers, redSoldiers);
                    }
                }
            }


            if (redSoldiers != null) {
                for (Soldier soldier : redSoldiers) {
                    if (soldier != null) {
                        updateSoldier(soldier, blueWorkers, blueScout, blueDefenders, redSoldiers, blueSoldiers);
                    }
                }
            }

            assignFlankingSoldiersIfNeeded(blueSoldiers, redSoldiers, "blue");
            assignFlankingSoldiersIfNeeded(redSoldiers, blueSoldiers, "red");

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

            if (artillery != null) {
                if (!artillery.isActive()) {
                    if ("blue".equalsIgnoreCase(artillery.getTeam())) releaseWaiting(blueSoldiers);
                    else releaseWaiting(redSoldiers);
                } else {
                    if ("blue".equalsIgnoreCase(artillery.getTeam()) && redHasSoldiers) releaseWaiting(blueSoldiers);
                    if ("red".equalsIgnoreCase(artillery.getTeam()) && blueHasSoldiers) releaseWaiting(redSoldiers);
                }
            }


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

                    // --- формация: 100px зад оръдието, колони по 5, spacing 20px ---
                    final double BEHIND = 100.0;
                    final double SP = 20.0;
                    final int COL_SIZE = 5;

                    double th = Math.toRadians(angleToEnemyBase);
                    double cos = Math.cos(th), sin = Math.sin(th);

                    // котва: точно зад оръдието
                    double bx = artX - BEHIND * cos;
                    double by = artY - BEHIND * sin;

                    // перпендикулар и назад
                    double lx = -sin, ly =  cos;
                    double dx = -cos, dy = -sin;

                    java.util.List<Soldier> alive = new java.util.ArrayList<>();
                    for (Soldier s : blueSoldiers) if (s != null && s.isActive()) alive.add(s);

                    for (int k = 0; k < alive.size(); k++) {
                        Soldier s = alive.get(k);
                        int col = k / COL_SIZE;
                        int row = k % COL_SIZE;

                        double sx = bx + col * SP * lx + row * SP * dx;
                        double sy = by + col * SP * ly + row * SP * dy;

                        s.setX(sx);
                        s.setY(sy);
                        s.setCurrentAngle(angleToEnemyBase);
                        s.setWaiting(true);
                        s.setAnchorY(sy);
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

                    final double BEHIND = 100.0;
                    final double SP = 20.0;
                    final int COL_SIZE = 5;

                    double th = Math.toRadians(angleToEnemyBase);
                    double cos = Math.cos(th), sin = Math.sin(th);

                    double bx = artX - BEHIND * cos;
                    double by = artY - BEHIND * sin;

                    double lx = -sin, ly =  cos;
                    double dx = -cos, dy = -sin;

                    java.util.List<Soldier> alive = new java.util.ArrayList<>();
                    for (Soldier s : redSoldiers) if (s != null && s.isActive()) alive.add(s);

                    for (int k = 0; k < alive.size(); k++) {
                        Soldier s = alive.get(k);
                        int col = k / COL_SIZE;
                        int row = k % COL_SIZE;

                        double sx = bx + col * SP * lx + row * SP * dx;
                        double sy = by + col * SP * ly + row * SP * dy;

                        s.setX(sx);
                        s.setY(sy);
                        s.setCurrentAngle(angleToEnemyBase);
                        s.setWaiting(true);
                        s.setAnchorY(sy);
                    }
                }
            }

            mainPanel.repaint();
        });

        gameLoop.start();
        setVisible(true);
    }

    private void releaseWaiting(Soldier[] soldiers) {
        if (soldiers == null) return;
        for (Soldier s : soldiers) if (s != null) s.setWaiting(false);
    }

    private void checkForAvailableResources() {
        for (Worker worker : allWorkers) {
            if (worker.hasStarted() && !worker.isActive()) {
                worker.updateWorkerCycle(resources, blueBaseX, blueBaseY, null);
            }
        }
    }

    private void initializeResources() {
        resources = new Resource[21];////////////////////////////////////////////////////////////////////////////////////
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
                y = (int) (Math.random() * (panelHeight - 2 * baseHeight)) + baseHeight;
                positionIsValid = !isNearBase(x, y) && !isNearWorkers(x, y, workerPositions);
            } while (!positionIsValid);

            resources[i] = new Resource(x, y, 10);////////////////////////////////////////////////////////////////
        }
    }

    private void initializeWorkers() {
        int totalWorkers = 10;//////////////////////////////////////////////////////////////////////////////////////////
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

    private void initializeSoldiers(String team, int baseX, int baseY, int baseHealth) {
        final int soldierHealthCost = 1;////////////////////////////////////////////////////////////////////////////////
        final int maxRowsPerColumn = 10;
        final int columnSpacing = 30;
        final int rowSpacing = 30;
        final int targetY = baseY - 200;

        int maxSoldiers = baseHealth / soldierHealthCost;
        if (maxSoldiers <= 0) return;

        Soldier[] soldiers = new Soldier[maxSoldiers];

        for (int i = 0; i < maxSoldiers; i++) {
            int columnIndex = i / maxRowsPerColumn;
            int rowIndex = i % maxRowsPerColumn;
            int x = baseX + (team.equals("blue") ? columnIndex * columnSpacing : -columnIndex * columnSpacing);
            int y = targetY + rowIndex * rowSpacing;

            soldiers[i] = new Soldier(
                    x, y, team,
                    baseX, baseY,
                    team.equals("blue") ? redBaseX : blueBaseX,
                    team.equals("blue") ? redBaseY : blueBaseY,
                    this, i + 1
            );
            soldiers[i].setAnchorY(y);
        }

        int pointsUsed = maxSoldiers * soldierHealthCost;
        if (team.equals("blue")) {
            blueSoldiers = soldiers;
            blueBaseHealth -= pointsUsed;
        }
        else {
            redSoldiers = soldiers;
            redBaseHealth -= pointsUsed;
        }
    }

    private void startSoldierCreation(String team, int baseX, int baseY) {
        final int soldierCost = 1;//////////////////////////////////////////////////////////////////////////////////
        final int maxRowsPerColumn = 12;
        final int columnSpacing = 30;
        final int rowSpacing = 30;
        final int targetY = baseY - 200;
        int baseHealth = team.equals("blue") ? blueBaseHealth : redBaseHealth;
        int maxSoldiers = baseHealth / soldierCost;

        if (maxSoldiers <= 0) return;

        for (int soldiersCreated = 0; soldiersCreated < maxSoldiers; soldiersCreated++) {
            int columnIndex = soldiersCreated / maxRowsPerColumn;
            int rowIndex = soldiersCreated % maxRowsPerColumn;

            int x = baseX + (team.equals("blue") ? columnIndex * columnSpacing : -columnIndex * columnSpacing);
            int y = targetY + rowIndex * rowSpacing;

            int enemyBaseX = team.equals("blue") ? redBaseX : blueBaseX;
            int enemyBaseY = team.equals("blue") ? redBaseY : blueBaseY;

            Soldier soldier = new Soldier(
                    x, y, team,
                    baseX, baseY,
                    enemyBaseX, enemyBaseY,
                    this, soldiersCreated + 1
            );
            soldier.setAnchorY(y);

            if (team.equals("blue")) {
                blueSoldiers = addSoldierToArray(blueSoldiers, soldier);
                blueBaseHealth -= soldierCost;
            } else {
                redSoldiers = addSoldierToArray(redSoldiers, soldier);
                redBaseHealth -= soldierCost;
            }
        }
    }

    private void initializeDefenders() {
        for (int i = 0; i < 4; i++) {///////////////////////////////////////////////////////////////////////////////////

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

    private void removeDeadDefenders() {
        for (int i = 0; i < blueDefenders.length; i++) {
            if (blueDefenders[i] != null && !blueDefenders[i].isActive()) {
                blueDefenders[i] = null;
            }
        }
        for (int i = 0; i < redDefenders.length; i++) {
            if (redDefenders[i] != null && !redDefenders[i].isActive()) {
                redDefenders[i] = null;
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

    private void updateSoldier(Soldier soldier,
                               Worker[] enemyWorkers, Scout enemyScout, Defender[] enemyDefenders,
                               Soldier[] teammates, Soldier[] enemyArmy) {
        if (soldier.isWaiting()) return;
        soldier.updateSoldier(teammates, enemyArmy);
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

    private void moveDefenders() {
        if (gameOver) return; // NEW
        updateDefenders(blueDefenders, redScout, redSoldiers, blueBaseX, blueBaseY);
        updateDefenders(redDefenders, blueScout, blueSoldiers, redBaseX, redBaseY);
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

    private void updateDefenders(Defender[] defenders, Scout enemyScout, Soldier[] enemySoldiers, int baseX, int baseY) {
        if (defenders == null || defenders.length == 0) return;

        for (Defender defender : defenders) {
            if (defender != null) {
                defender.patrolAroundBase(baseX + baseWidth / 2, baseY + baseHeight / 2, DEFENDER_SHIELD_RADIUS);

                if (enemyScout != null && enemyScout.isActive()) {
                    defender.checkAndShootIfScoutInRange(enemyScout);
                    defender.updateProjectiles(enemyScout);
                }

                if (enemySoldiers != null && enemySoldiers.length > 0) {
                    ArrayList<Soldier> activeSoldiers = new ArrayList<>();
                    for (Soldier soldier : enemySoldiers) {
                        if (soldier != null && soldier.isActive()) activeSoldiers.add(soldier);
                    }
                    if (!activeSoldiers.isEmpty()) {
                        defender.checkAndShootIfSoldiersInRange(activeSoldiers);
                        defender.updateProjectilesForSoldier(activeSoldiers);
                    }
                }

                if (artillery != null && artillery.isActive()
                        && !defender.getTeam().equalsIgnoreCase(artillery.getTeam())) {
                    defender.checkAndShootIfArtilleryProjectileInRange(artillery);
                    defender.updateProjectilesForArtillery(artillery);
                }
            }
        }
    }

    private void drawBasesAndResources(Graphics2D g2d, int shieldRadius) {
        g2d.setColor(new Color(0, 100, 200));
        g2d.fillRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.BLUE);
        g2d.drawRoundRect(blueBaseX, blueBaseY, baseWidth, baseHeight, 20, 20);

        if (baseShieldPointsBlue > 0) {
            if (blueShieldBlinking && blueShieldBlinkState % 2 == 0) {
                g2d.setColor(new Color(0, 255, 255, 200));
            } else {
                g2d.setColor(new Color(0, 0, 255, 200));
            }
            g2d.drawOval(blueBaseX - (shieldRadius - baseWidth) / 2, blueBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);
            g2d.setColor(Color.BLUE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("" + baseShieldPointsBlue, blueBaseX, blueBaseY - 25);
        }

        g2d.setColor(new Color(236, 8, 8));
        g2d.fillRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);
        g2d.setColor(Color.RED);
        g2d.drawRoundRect(redBaseX, redBaseY, baseWidth, baseHeight, 20, 20);

        if (baseShieldPointsRed > 0) {
            if (redShieldBlinking && redShieldBlinkState % 2 == 0) {
                g2d.setColor(new Color(227, 35, 18, 200));
            } else {
                g2d.setColor(new Color(234, 5, 5, 229));
            }
            g2d.drawOval(redBaseX - (shieldRadius - baseWidth) / 2, redBaseY - (shieldRadius - baseHeight) / 2, shieldRadius, shieldRadius);
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("" + baseShieldPointsRed, redBaseX, redBaseY - 25);
        }

        for (Resource resource : resources) {
            g2d.setColor(resource.getValue() <= 0 ? new Color(35, 33, 33) : new Color(167, 151, 3));
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

            if (ant.team.equals("blue")) g2d.setColor(Color.BLUE);
            else g2d.setColor(Color.RED);

            g2d.fillOval((int) (ant.getX() - bodyRadius), (int) (ant.getY() - bodyRadius), bodyRadius * 2, bodyRadius * 2);

            Worker worker = (Worker) ant;
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.valueOf(worker.getId()), (int) worker.getX() - 5, (int) worker.getY() - 10);

            if (ant.team.equals("blue")) g2d.setColor(Color.BLUE);
            else g2d.setColor(Color.RED);
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

        if (ant instanceof Scout) g2d.setColor(Color.GREEN);
        else if (ant instanceof Worker || ant instanceof Defender) g2d.setColor(Color.YELLOW);

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
                if (worker.isActive()) anyActiveWorkers = true;
            }
        }

        for (Worker worker : redWorkers) {
            if (worker != null) {
                if (resourcesDepleted && !worker.isAtStartPosition(redBaseX, redBaseY)) {
                    worker.moveToStartPosition(redBaseX, redBaseY);
                }
                worker.updateWorkerCycle(resources, redBaseX, redBaseY, blueScout);
                if (worker.isActive()) anyActiveWorkers = true;
            }
        }

        if (resourcesDepleted && allBlueWorkersAtStart && allRedWorkersAtStart) {
            boolean soldiersCreated = false;

            if (blueBaseHealth >= 5) {
                startSoldierCreation("blue", blueBaseX, blueBaseY);
                soldiersCreated = true;
            }
            if (redBaseHealth >= 5) {
                startSoldierCreation("red", redBaseX, redBaseY);
                soldiersCreated = true;
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
        if (array == null) array = new Soldier[0];
        Soldier[] newArray = new Soldier[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = soldier;
        return newArray;
    }

    public boolean allResourcesDepleted() {
        for (Resource resource : resources) {
            if (resource.getValue() > 0) return false;
        }
        return true;
    }

    public boolean allWorkersAtBase(Worker[] workers, int baseX, int baseY) {
        for (Worker worker : workers) {
            if (worker != null) {
                if (!worker.isAtBase(baseX, baseY)) return false;
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

    public List<Worker> getAllWorkers() { return allWorkers; }
    public int getBlueBaseHealth() { return blueBaseHealth; }
    public void setBlueBaseHealth(int health) { this.blueBaseHealth = health; }
    public int getRedBaseHealth() { return redBaseHealth; }
    public void setRedBaseHealth(int health) { this.redBaseHealth = health; }

    public void addPointsToScoutBase(String team, int points) {
        if (team.equals("blue")) blueBaseHealth += points;
        else if (team.equals("red")) redBaseHealth += points;
    }

    public Worker findClosestEnemyWorkerWithinRange(Scout scout, String scoutTeam, double maxRange) {
        Worker[] enemyWorkers = scoutTeam.equals("blue") ? redWorkers : blueWorkers;
        Worker closestWorker = null;
        double closestDistance = maxRange;

        if (enemyWorkers == null) return null;

        for (Worker worker : enemyWorkers) {
            if (worker == null || !worker.isActive()) continue;
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
        if (enemyWorkers == null) return nearbyWorkers;

        for (Worker worker : enemyWorkers) {
            if (worker == null || !worker.isActive()) continue;
            if (scout.distanceTo(worker) <= range) nearbyWorkers.add(worker);
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

        // локален таймер – НЕ пипа gameLoop
        Timer t = new Timer(150, e -> {
            if (gameOver) return;
            bulletStartX = bulletStartY = bulletEndX = bulletEndY = -1;
            repaint();
            ((Timer) e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    private void removeWorkers(Worker[] workers) {
        for (int i = 0; i < workers.length; i++) workers[i] = null;

        if (redScout != null) redScout = null;
        if (blueScout != null) blueScout = null;
    }

    public List<Character> getCharacters() {
        List<Character> characters = new ArrayList<>();

        if (blueSoldiers != null) {
            for (Soldier soldier : blueSoldiers) {
                if (soldier != null && soldier.isActive()) characters.add(soldier);
            }
        }

        if (redSoldiers != null) {
            for (Soldier soldier : redSoldiers) {
                if (soldier != null && soldier.isActive()) characters.add(soldier);
            }
        }

        for (Worker worker : blueWorkers) if (worker != null) characters.add(worker);
        for (Worker worker : redWorkers) if (worker != null) characters.add(worker);
        for (Defender defender : blueDefenders) if (defender != null) characters.add(defender);
        for (Defender defender : redDefenders) if (defender != null) characters.add(defender);
        if (blueScout != null) characters.add(blueScout);
        if (redScout != null) characters.add(redScout);

        return characters;
    }

    public double getBaseShieldRadius() {
        return Math.max(baseWidth, baseHeight) * 1.5;
    }

    public void reduceShieldPoints(String team, int points) {
        if ("red".equalsIgnoreCase(team)) {
            baseShieldPointsRed -= points;
            if (baseShieldPointsRed < 0) baseShieldPointsRed = 0;

            redShieldBlinking = true;
            redShieldBlinkState = 0;

            Timer redBlinkTimer = new Timer(200, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    redShieldBlinkState++;
                    repaint();
                    if (redShieldBlinkState >= 6) {
                        redShieldBlinking = false;
                        ((Timer) e.getSource()).stop();
                    }
                }
            });
            redBlinkTimer.start();
        } else if ("blue".equalsIgnoreCase(team)) {
            baseShieldPointsBlue -= points;
            if (baseShieldPointsBlue < 0) baseShieldPointsBlue = 0;

            blueShieldBlinking = true;
            blueShieldBlinkState = 0;

            Timer blueBlinkTimer = new Timer(500, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    blueShieldBlinkState++;
                    repaint();
                    if (blueShieldBlinkState >= 6) {
                        blueShieldBlinking = false;
                        ((Timer) e.getSource()).stop();
                    }
                }
            });
            blueBlinkTimer.start();
        }
    }

    public Defender[] getDefenders() {
        Defender[] allDefenders = new Defender[blueDefenders.length + redDefenders.length];
        System.arraycopy(blueDefenders, 0, allDefenders, 0, blueDefenders.length);
        System.arraycopy(redDefenders, 0, allDefenders, blueDefenders.length, redDefenders.length);

        return Arrays.stream(allDefenders)
                .filter(Objects::nonNull)
                .toArray(Defender[]::new);
    }

    public int getBaseShieldPoints(String team) {
        if ("blue".equalsIgnoreCase(team)) return baseShieldPointsBlue;
        else if ("red".equalsIgnoreCase(team)) return baseShieldPointsRed;
        return 0;
    }

    private void assignFlankingSoldiersIfNeeded(Soldier[] soldiers, Soldier[] enemyArmy, String team) {
        if (soldiers == null || soldiers.length == 0) return;

        int alive = 0;
        for (Soldier s : soldiers) if (s != null && s.isActive()) alive++;
        int total = soldiers.length;
        int dead  = total - alive;
        if (alive == 0) return;

        int threshold = Math.max(1, (int)Math.floor(total * 0.10));
        if (dead < threshold) return;

        if (isAnyFlankRunning(soldiers)) return;

        // ВЗЕМИ ЛИДЕР, който е НАЙ-ОТЗАД според посоката на настъпление
        Soldier leader = pickBacklineLeader(soldiers, team);
        if (leader == null) return;

        Soldier rearTarget = findRearEnemy(enemyArmy, oppositeTeam(team));

        if (rearTarget == null) return;

        java.util.List<Soldier> candidates = new ArrayList<>();
        for (Soldier s : soldiers) {
            if (s != null && s.isActive() && !s.isWaiting() && !s.getFlankingMode()) {
                candidates.add(s);
            }
        }
        // сортираме по „напредък“ по X: за blue най-малко X е най-отзад; за red най-голямо X е най-отзад
        candidates.sort(Comparator.comparingDouble(s -> forwardProgress(s, team)));
        candidates.remove(leader);

        java.util.List<Soldier> wing = new ArrayList<>();
        wing.add(leader);
        for (Soldier s : candidates) { wing.add(s); if (wing.size() >= 3) break; }

        for (Soldier s : wing) {
            s.setFlankLeader(s == leader);
            s.setFlankLeaderRef(leader);
            s.setFlankTarget(rearTarget);
            s.setFlankingMode(true);
            s.setFlanking(true);
        }
        leader.say("Flank!", 1500);

        System.out.println("[" + team + "] Фланг: лидер #" + leader.getId() +
                " + " + (wing.size()-1) + " последователи → цел #" + rearTarget.getId());
    }

    // проекция на позицията върху вектора от нашата база към вражеската
    private double forwardProgress(Soldier s, String team) {
        double bx = "blue".equalsIgnoreCase(team) ? blueBaseX + baseWidth / 2.0 : redBaseX + baseWidth / 2.0;
        double by = "blue".equalsIgnoreCase(team) ? blueBaseY + baseHeight / 2.0 : redBaseY + baseHeight / 2.0;
        double ex = "blue".equalsIgnoreCase(team) ? redBaseX + baseWidth / 2.0  : blueBaseX + baseWidth / 2.0;
        double ey = "blue".equalsIgnoreCase(team) ? redBaseY + baseHeight / 2.0 : blueBaseY + baseHeight / 2.0;

        double vx = ex - bx, vy = ey - by;
        double len = Math.hypot(vx, vy);
        if (len == 0) return 0;
        vx /= len; vy /= len;

        // по-малко => по-назад
        return (s.getX() - bx) * vx + (s.getY() - by) * vy;
    }


    private Soldier pickBacklineLeader(Soldier[] soldiers, String team) {
        Soldier best = null;
        double bestProgress = Double.POSITIVE_INFINITY; // търсим НАЙ-малък „напредък“ (най-отзад)
        for (Soldier s : soldiers) {
            if (s == null || !s.isActive() || s.isWaiting() || s.getFlankingMode()) continue;
            double p = forwardProgress(s, team);
            if (p < bestProgress) { bestProgress = p; best = s; }
        }
        return best;
    }

    private boolean isAnyFlankRunning(Soldier[] soldiers) {
        for (Soldier s : soldiers) {
            if (s != null && s.isActive() && s.getFlankingMode()) return true;
        }
        return false;
    }

    private Soldier findRearEnemy(Soldier[] enemyArmy, String enemyTeam) {
        Soldier rear = null;
        double best = Double.POSITIVE_INFINITY; // търсим най-малкия напредък = най-отзад
        if (enemyArmy == null) return null;
        for (Soldier s : enemyArmy) {
            if (s != null && s.isActive()) {
                double p = forwardProgress(s, enemyTeam);
                if (p < best) { best = p; rear = s; }
            }
        }
        return rear;
    }

    private String oppositeTeam(String team) {
        return "blue".equalsIgnoreCase(team) ? "red" : "blue";
    }


    public void onBaseDestroyed(String destroyedTeam) {
        if (gameOver) return;

        // край и победител
        gameOver = true;
        winner = "blue".equalsIgnoreCase(destroyedTeam) ? "RED WINS!" : "BLUE WINS!";

        // спри игровия цикъл
        if (gameLoop != null) gameLoop.stop();

        if ("blue".equalsIgnoreCase(destroyedTeam)) baseShieldPointsBlue = 0;
        else baseShieldPointsRed = 0;

        Defender[] arr = "blue".equalsIgnoreCase(destroyedTeam) ? blueDefenders : redDefenders;
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null) arr[i].setActive(false);
            }
        }

        // спри артилерията
        if (artillery != null) artillery.setActive(false);

        // визуални експлозии в центъра на базата
        int cx = "blue".equalsIgnoreCase(destroyedTeam) ? blueBaseX + baseWidth / 2 : redBaseX + baseWidth / 2;
        int cy = "blue".equalsIgnoreCase(destroyedTeam) ? blueBaseY + baseHeight / 2 : redBaseY + baseHeight / 2;
        for (int i = 0; i < 6; i++) {
            addExplosionEffect(cx, cy, 160 - i * 20, new Color(255, 120, 60), 1200);
        }

        repaint();
    }

}