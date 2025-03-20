package com.marko.rain.cellularautomatontwod;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class CellularAutomaton2D extends JPanel {
    private static final int GRID_SIZE = 50;
    private static final int CELL_SIZE = 10;
    private static final int DELAY = 200;

    private int[][] grid;
    private boolean isCaveMode = false;
    private Timer timer;
    private final JLabel ruleLabel;
    private final JLabel generationLabel;
    private int generationCount;

    public CellularAutomaton2D(JLabel ruleLabel, JLabel generationLabel) {
        this.ruleLabel = ruleLabel;
        this.generationLabel = generationLabel;
        updateRuleLabel();
        updateGenerationLabel();

        grid = new int[GRID_SIZE][GRID_SIZE];
        initializeRandomGrid();

        timer = new Timer(DELAY, e -> {
            grid = getNextGeneration();
            generationCount++;
            updateGenerationLabel();
            repaint();
        });
    }

    private void updateRuleLabel() {
        if (isCaveMode) {
            ruleLabel.setText("Current Rule: B678/S2345678 (Cave Generation)");
        } else {
            ruleLabel.setText("Current Rule: B3/S23 (Conway's Game of Life)");
        }
    }

    private void updateGenerationLabel() {
        generationLabel.setText("Generation: " + generationCount);
    }

    private void initializeRandomGrid() {
        Random rand = new Random();
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                grid[row][col] = (rand.nextDouble() < 0.45) ? 1 : 0;
            }
        }
        generationCount = 0;
        updateGenerationLabel();
    }

    private int[][] getNextGeneration() {
        int[][] newGrid = new int[GRID_SIZE][GRID_SIZE];

        for (int row = 1; row < GRID_SIZE - 1; row++) {
            for (int col = 1; col < GRID_SIZE - 1; col++) {
                int aliveNeighbors = countAliveNeighbors(row, col);

                if (isCaveMode) { // B678/S2345678
                    if (grid[row][col] == 1) {
                        newGrid[row][col] = (aliveNeighbors >= 2 && aliveNeighbors <= 8) ? 1 : 0;
                    } else {
                        newGrid[row][col] = (aliveNeighbors >= 6) ? 1 : 0;
                    }
                } else { // B3/S23
                    if (grid[row][col] == 1) {
                        newGrid[row][col] = (aliveNeighbors == 2 || aliveNeighbors == 3) ? 1 : 0;
                    } else {
                        newGrid[row][col] = (aliveNeighbors == 3) ? 1 : 0;
                    }
                }
            }
        }
        return newGrid;
    }

    private int countAliveNeighbors(int row, int col) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!(i == 0 && j == 0)) {
                    count += grid[row + i][col + j];
                }
            }
        }
        return count;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] == 1) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                g.setColor(Color.GRAY);
                g.drawRect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    public void startSimulation() {
        timer.start();
    }

    public void stopSimulation() {
        timer.stop();
    }

    public void resetGrid() {
        initializeRandomGrid();
        repaint();
    }

    public void toggleRuleSet() {
        isCaveMode = !isCaveMode;
        updateRuleLabel();
        resetGrid();
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("2D Cellular Automaton - Conway & Caves");

        JLabel ruleLabel = new JLabel("Current Rule: B3/S23 (Conway's Game of Life)", SwingConstants.CENTER);
        ruleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel generationLabel = new JLabel("Generation: 0", SwingConstants.CENTER);
        generationLabel.setFont(new Font("Arial", Font.BOLD, 14));

        CellularAutomaton2D panel = new CellularAutomaton2D(ruleLabel, generationLabel);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton resetButton = new JButton("Reset");
        JButton toggleRuleButton = new JButton("Toggle Rules");

        startButton.addActionListener(e -> panel.startSimulation());
        stopButton.addActionListener(e -> panel.stopSimulation());
        resetButton.addActionListener(e -> panel.resetGrid());
        toggleRuleButton.addActionListener(e -> panel.toggleRuleSet());

        JPanel controlPanel = new JPanel();
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(resetButton);
        controlPanel.add(toggleRuleButton);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(ruleLabel);
        infoPanel.add(generationLabel);

        frame.setLayout(new BorderLayout());
        frame.add(infoPanel, BorderLayout.NORTH);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(panel, BorderLayout.CENTER);

        frame.setSize(GRID_SIZE * CELL_SIZE + 20, GRID_SIZE * CELL_SIZE + 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
