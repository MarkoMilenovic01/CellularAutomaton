package com.marko.rain.elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class CaveSimulation extends JFrame {
    private static final int CELL_SIZE = 10;
    private static final int GRID_WIDTH = 80;
    private static final int GRID_HEIGHT = 60;
    private static final double INITIAL_WALL_PROBABILITY = 0.45;
    private static final int SMOKE_LIFESPAN = 20;

    private Element[][] grid = new Element[GRID_HEIGHT][GRID_WIDTH];
    private double[][] waterVolume = new double[GRID_HEIGHT][GRID_WIDTH];
    private int[][] smokeLife = new int[GRID_HEIGHT][GRID_WIDTH];
    private boolean[][] darkSmoke = new boolean[GRID_HEIGHT][GRID_WIDTH];
    private int generationCount = 0;

    private SimulationPanel simulationPanel;
    private JComboBox<Element> elementSelector;
    private JLabel generationLabel;
    private final Random rand = new Random();

    public enum Element { EMPTY, SAND, WOOD, FIRE, SMOKE, WATER, WALL }

    public CaveSimulation() {
        setTitle("Cave Simulation - Water Overfill with Sideways Push");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        initializeGrid();

        simulationPanel = new SimulationPanel();
        simulationPanel.setPreferredSize(new Dimension(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE));
        simulationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;
                if (row >= 0 && row < GRID_HEIGHT && col >= 0 && col < GRID_WIDTH && grid[row][col] == Element.EMPTY) {
                    Element chosen = (Element) elementSelector.getSelectedItem();
                    grid[row][col] = chosen;
                    if (chosen == Element.WATER) {
                        waterVolume[row][col] = 1.0;
                    } else if (chosen == Element.SMOKE) {
                        smokeLife[row][col] = SMOKE_LIFESPAN;
                        darkSmoke[row][col] = false;
                    }
                    simulationPanel.repaint();
                }
            }
        });

        elementSelector = new JComboBox<>(new Element[]{
                Element.SAND, Element.WOOD, Element.FIRE, Element.SMOKE, Element.WATER
        });
        generationLabel = new JLabel("Generation: 0");
        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Select Element:"));
        controlPanel.add(elementSelector);
        controlPanel.add(generationLabel);

        add(simulationPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Timer update order: sand -> water -> wood -> fire -> smoke
        Timer timer = new Timer(100, e -> {
            generationCount++;
            moveSand();         // sand falls and displaces water
            simulateWater();    // water flows down/sideways/up based on rules
            moveWood();         // wood falls if empty below, floats up if water is above
            moveFire();         // fire spreads and converts wood to smoke
            moveSmoke();        // smoke moves upward and sideways
            simulationPanel.repaint();
            generationLabel.setText("Generation: " + generationCount);
        });
        timer.start();
    }

    private void initializeGrid() {
        for (int r = 0; r < GRID_HEIGHT; r++) {
            for (int c = 0; c < GRID_WIDTH; c++) {
                grid[r][c] = (rand.nextDouble() < INITIAL_WALL_PROBABILITY) ? Element.WALL : Element.EMPTY;
                waterVolume[r][c] = 0.0;
                smokeLife[r][c] = 0;
                darkSmoke[r][c] = false;
            }
        }
        for (int c = 0; c < GRID_WIDTH; c++) {
            grid[0][c] = Element.WALL;
            grid[GRID_HEIGHT - 1][c] = Element.WALL;
        }
        for (int r = 0; r < GRID_HEIGHT; r++) {
            grid[r][0] = Element.WALL;
            grid[r][GRID_WIDTH - 1] = Element.WALL;
        }
        for (int i = 0; i < 5; i++) {
            applyCellularAutomaton();
        }
    }

    private void applyCellularAutomaton() {
        Element[][] newGrid = new Element[GRID_HEIGHT][GRID_WIDTH];
        for (int r = 0; r < GRID_HEIGHT; r++) {
            for (int c = 0; c < GRID_WIDTH; c++) {
                int neighbors = countWallNeighbors(r, c);
                if (grid[r][c] == Element.WALL) {
                    newGrid[r][c] = (neighbors >= 2 && neighbors <= 8) ? Element.WALL : Element.EMPTY;
                } else {
                    newGrid[r][c] = (neighbors >= 6) ? Element.WALL : Element.EMPTY;
                }
            }
        }
        grid = newGrid;
    }

    private int countWallNeighbors(int r, int c) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int rr = r + dr, cc = c + dc;
                if (rr >= 0 && rr < GRID_HEIGHT && cc >= 0 && cc < GRID_WIDTH && grid[rr][cc] == Element.WALL) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- SAND: falls down, displaces water if necessary ---
    private void moveSand() {
        for (int row = GRID_HEIGHT - 2; row >= 1; row--) {
            for (int col = 1; col < GRID_WIDTH - 1; col++) {
                if (grid[row][col] == Element.SAND) {
                    if (grid[row + 1][col] == Element.WATER) {  // displace water
                        grid[row + 1][col] = Element.SAND;
                        grid[row][col] = Element.WATER;
                        waterVolume[row][col] = waterVolume[row + 1][col];
                        waterVolume[row + 1][col] = 0.0;
                    } else if (grid[row + 1][col] == Element.EMPTY) {  // normal falling
                        grid[row + 1][col] = Element.SAND;
                        grid[row][col] = Element.EMPTY;
                    } else {  // diagonal movement
                        boolean leftEmpty = (grid[row + 1][col - 1] == Element.EMPTY);
                        boolean rightEmpty = (grid[row + 1][col + 1] == Element.EMPTY);
                        if (leftEmpty && rightEmpty) {
                            if (rand.nextBoolean()) {
                                grid[row + 1][col - 1] = Element.SAND;
                            } else {
                                grid[row + 1][col + 1] = Element.SAND;
                            }
                            grid[row][col] = Element.EMPTY;
                        } else if (leftEmpty) {
                            grid[row + 1][col - 1] = Element.SAND;
                            grid[row][col] = Element.EMPTY;
                        } else if (rightEmpty) {
                            grid[row + 1][col + 1] = Element.SAND;
                            grid[row][col] = Element.EMPTY;
                        }
                    }
                }
            }
        }
    }

    // --- WATER: flows down, splits sideways, and overflows upward ---
    private void simulateWater() {
        double[][] newVol = new double[GRID_HEIGHT][GRID_WIDTH];
        for (int r = 1; r < GRID_HEIGHT - 1; r++) {
            for (int c = 1; c < GRID_WIDTH - 1; c++) {
                newVol[r][c] = waterVolume[r][c];
            }
        }
        for (int row = GRID_HEIGHT - 2; row >= 1; row--) {
            for (int col = 1; col < GRID_WIDTH - 1; col++) {
                double vol = newVol[row][col];
                if (vol <= 0) continue;
                if ((grid[row + 1][col] == Element.EMPTY || grid[row + 1][col] == Element.WATER)) {
                    double capacityBelow = 1.0 - newVol[row + 1][col];
                    if (capacityBelow > 0) {
                        double moveDown = Math.min(vol, capacityBelow);
                        newVol[row + 1][col] += moveDown;
                        newVol[row][col] -= moveDown;
                        vol -= moveDown;
                    }
                }
                vol = newVol[row][col];
                if (vol > 0) {
                    boolean downBlockedOrFull = false;
                    if (!(grid[row + 1][col] == Element.EMPTY || grid[row + 1][col] == Element.WATER)) {
                        downBlockedOrFull = true;
                    } else {
                        if (newVol[row + 1][col] >= 1.0) {
                            downBlockedOrFull = true;
                        }
                    }
                    if (downBlockedOrFull) {
                        boolean leftOpen = (grid[row + 1][col - 1] == Element.EMPTY || grid[row + 1][col - 1] == Element.WATER);
                        boolean rightOpen = (grid[row + 1][col + 1] == Element.EMPTY || grid[row + 1][col + 1] == Element.WATER);
                        if (leftOpen && rightOpen) {
                            double half = vol / 2.0;
                            double capLeft = 1.0 - newVol[row + 1][col - 1];
                            double moveLeft = Math.min(half, Math.max(capLeft, 0));
                            double capRight = 1.0 - newVol[row + 1][col + 1];
                            double moveRight = Math.min(half, Math.max(capRight, 0));
                            newVol[row + 1][col - 1] += moveLeft;
                            newVol[row + 1][col + 1] += moveRight;
                            double movedTotal = moveLeft + moveRight;
                            newVol[row][col] -= movedTotal;
                        } else if (leftOpen) {
                            double capLeft = 1.0 - newVol[row + 1][col - 1];
                            double moveLeft = Math.min(vol, Math.max(capLeft, 0));
                            newVol[row + 1][col - 1] += moveLeft;
                            newVol[row][col] -= moveLeft;
                        } else if (rightOpen) {
                            double capRight = 1.0 - newVol[row + 1][col + 1];
                            double moveRight = Math.min(vol, Math.max(capRight, 0));
                            newVol[row + 1][col + 1] += moveRight;
                            newVol[row][col] -= moveRight;
                        }
                    }
                }
                vol = newVol[row][col];
                if (vol > 1.0) {
                    double excess = vol - 1.0;
                    newVol[row][col] = 1.0;
                    if (grid[row - 1][col] == Element.EMPTY || grid[row - 1][col] == Element.WATER) {
                        double capUp = 1.0 - newVol[row - 1][col];
                        if (capUp > 0) {
                            double moveUp = Math.min(excess, capUp);
                            newVol[row - 1][col] += moveUp;
                        }
                    }
                }
            }
        }
        for (int r = 1; r < GRID_HEIGHT - 1; r++) {
            for (int c = 1; c < GRID_WIDTH - 1; c++) {
                waterVolume[r][c] = newVol[r][c];
                if (waterVolume[r][c] > 0) {
                    grid[r][c] = Element.WATER;
                } else if (grid[r][c] == Element.WATER) {
                    grid[r][c] = Element.EMPTY;
                }
            }
        }
    }

    // --- WOOD: falls if empty below, floats up if water is directly above ---
    private void moveWood() {
        for (int row = GRID_HEIGHT - 2; row >= 1; row--) {
            for (int col = 1; col < GRID_WIDTH - 1; col++) {
                if (grid[row][col] == Element.WOOD) {
                    if (grid[row + 1][col] == Element.EMPTY) {  // Wood falls normally
                        grid[row + 1][col] = Element.WOOD;
                        grid[row][col] = Element.EMPTY;
                    }
                    else if (grid[row - 1][col] == Element.WATER) {  // Wood floats up if water is above
                        grid[row - 1][col] = Element.WOOD;
                        grid[row][col] = Element.WATER;
                        double tmpVol = waterVolume[row - 1][col];
                        waterVolume[row - 1][col] = waterVolume[row][col];
                        waterVolume[row][col] = tmpVol;
                    }
                    // Check for nearby fire (if fire is adjacent, wood turns to fire)
                    boolean fireNearby = false;
                    for (int dr = -1; dr <= 1 && !fireNearby; dr++) {
                        for (int dc = -1; dc <= 1 && !fireNearby; dc++) {
                            int nr = row + dr, nc = col + dc;
                            if (nr >= 0 && nr < GRID_HEIGHT && nc >= 0 && nc < GRID_WIDTH) {
                                if (grid[nr][nc] == Element.FIRE) {
                                    fireNearby = true;
                                }
                            }
                        }
                    }
                    if (fireNearby) {
                        grid[row][col] = Element.FIRE;
                    }
                }
            }
        }
    }

    // --- FIRE: spreads to empty cells and ignites wood ---
    private void moveFire() {
        for (int row = GRID_HEIGHT - 2; row >= 1; row--) {
            for (int col = 1; col < GRID_WIDTH - 1; col++) {
                if (grid[row][col] == Element.FIRE) {
                    int[][] candidates = { { row + 1, col - 1 }, { row + 1, col }, { row + 1, col + 1 } };
                    for (int i = 0; i < candidates.length; i++) {
                        int j = rand.nextInt(candidates.length);
                        int[] tmp = candidates[i];
                        candidates[i] = candidates[j];
                        candidates[j] = tmp;
                    }
                    boolean moved = false;
                    for (int[] cand : candidates) {
                        int nr = cand[0], nc = cand[1];
                        if (grid[nr][nc] == Element.EMPTY) {
                            grid[nr][nc] = Element.FIRE;
                            grid[row][col] = Element.EMPTY;
                            moved = true;
                            break;
                        } else if (grid[nr][nc] == Element.WOOD) {
                            grid[nr][nc] = Element.SMOKE;
                            smokeLife[nr][nc] = SMOKE_LIFESPAN;
                            darkSmoke[nr][nc] = true;
                            grid[row][col] = Element.EMPTY;
                            moved = true;
                            break;
                        }
                    }
                    if (!moved) {
                        grid[row][col] = Element.SMOKE;
                        smokeLife[row][col] = SMOKE_LIFESPAN;
                        darkSmoke[row][col] = false;
                    }
                }
            }
        }
    }

    // --- SMOKE: moves upward and sideways ---
    private void moveSmoke() {
        for (int row = 1; row < GRID_HEIGHT - 1; row++) {
            for (int col = 1; col < GRID_WIDTH - 1; col++) {
                if (grid[row][col] == Element.SMOKE) {
                    smokeLife[row][col]--;
                    if (smokeLife[row][col] <= 0) {
                        grid[row][col] = Element.EMPTY;
                        continue;
                    }
                    if (grid[row - 1][col] == Element.EMPTY) {
                        grid[row - 1][col] = Element.SMOKE;
                        smokeLife[row - 1][col] = smokeLife[row][col];
                        darkSmoke[row - 1][col] = darkSmoke[row][col];
                        grid[row][col] = Element.EMPTY;
                    } else {
                        boolean leftEmpty = (grid[row][col - 1] == Element.EMPTY);
                        boolean rightEmpty = (grid[row][col + 1] == Element.EMPTY);
                        if (leftEmpty && rightEmpty) {
                            if (rand.nextBoolean()) {
                                grid[row][col - 1] = Element.SMOKE;
                                smokeLife[row][col - 1] = smokeLife[row][col];
                                darkSmoke[row][col - 1] = darkSmoke[row][col];
                            } else {
                                grid[row][col + 1] = Element.SMOKE;
                                smokeLife[row][col + 1] = smokeLife[row][col];
                                darkSmoke[row][col + 1] = darkSmoke[row][col];
                            }
                            grid[row][col] = Element.EMPTY;
                        } else if (leftEmpty) {
                            grid[row][col - 1] = Element.SMOKE;
                            smokeLife[row][col - 1] = smokeLife[row][col];
                            darkSmoke[row][col - 1] = darkSmoke[row][col];
                            grid[row][col] = Element.EMPTY;
                        } else if (rightEmpty) {
                            grid[row][col + 1] = Element.SMOKE;
                            smokeLife[row][col + 1] = smokeLife[row][col];
                            darkSmoke[row][col + 1] = darkSmoke[row][col];
                            grid[row][col] = Element.EMPTY;
                        }
                    }
                }
            }
        }
    }

    // --- RENDERING ---
    private Color waterColor(int r, int c) {
        double vol = waterVolume[r][c];
        if (vol < 0) vol = 0;
        if (vol > 2.0) vol = 2.0;
        float ratio = (float) (vol / 2.0);
        int red = (int) (180 - 180 * ratio);
        int green = (int) (220 - 220 * ratio);
        int blue = (int) (255 - 75 * ratio);
        return new Color(red, green, blue);
    }

    private class SimulationPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int r = 0; r < GRID_HEIGHT; r++) {
                for (int c = 0; c < GRID_WIDTH; c++) {
                    Color color;
                    switch (grid[r][c]) {
                        case EMPTY: color = Color.WHITE; break;
                        case WALL:  color = Color.GRAY; break;
                        case SAND:  color = new Color(194, 178, 128); break;
                        case WOOD:  color = new Color(139, 69, 19); break;
                        case FIRE:  color = Color.RED; break;
                        case SMOKE: color = darkSmoke[r][c] ? new Color(64, 64, 64) : new Color(192, 192, 192); break;
                        case WATER: color = waterColor(r, c); break;
                        default:    color = Color.WHITE; break;
                    }
                    g.setColor(color);
                    g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    g.setColor(Color.BLACK);
                    g.drawRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CaveSimulation::new);
    }
}
