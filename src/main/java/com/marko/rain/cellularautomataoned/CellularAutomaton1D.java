package com.marko.rain.cellularautomataoned;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CellularAutomaton1D extends JPanel {
    private static final int CELL_SIZE = 10;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 400;
    private static final int CELLS = WIDTH / CELL_SIZE;
    private int[] cells = new int[CELLS];
    private int rule;

    public CellularAutomaton1D(int rule) {
        this.rule = rule;
        cells[CELLS / 2] = 1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int y = 0;
        for (int row = 0; row < HEIGHT / CELL_SIZE; row++) {
            for (int x = 0; x < CELLS; x++) {
                if (cells[x] == 1) {
                    g.fillRect(x * CELL_SIZE, y, CELL_SIZE, CELL_SIZE);
                }
            }
            evolve();
            y += CELL_SIZE;
        }
    }

    private void evolve() {
        int[] newCells = new int[CELLS];
        for (int i = 1; i < CELLS - 1; i++) {
            int left = cells[i - 1];
            int center = cells[i];
            int right = cells[i + 1];

            if (left == 1 && center == 1 && right == 1) newCells[i] = (rule / 128) % 2;
            else if (left == 1 && center == 1 && right == 0) newCells[i] = (rule / 64) % 2;
            else if (left == 1 && center == 0 && right == 1) newCells[i] = (rule / 32) % 2;
            else if (left == 1 && center == 0 && right == 0) newCells[i] = (rule / 16) % 2;
            else if (left == 0 && center == 1 && right == 1) newCells[i] = (rule / 8) % 2;
            else if (left == 0 && center == 1 && right == 0) newCells[i] = (rule / 4) % 2;
            else if (left == 0 && center == 0 && right == 1) newCells[i] = (rule / 2) % 2;
            else if (left == 0 && center == 0 && right == 0) newCells[i] = rule % 2;
        }
        cells = newCells;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1D Cellular Automaton");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(WIDTH, HEIGHT + 50);

            JPanel inputPanel = new JPanel();
            JTextField ruleInput = new JTextField("110", 5);
            JButton startButton = new JButton("Start");
            inputPanel.add(new JLabel("Enter Rule (0-255):"));
            inputPanel.add(ruleInput);
            inputPanel.add(startButton);

            frame.setLayout(new BorderLayout());
            frame.add(inputPanel, BorderLayout.NORTH);

            CellularAutomaton1D automaton = new CellularAutomaton1D(110);
            frame.add(automaton, BorderLayout.CENTER);

            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        int rule = Integer.parseInt(ruleInput.getText());
                        if (rule < 0 || rule > 255) throw new NumberFormatException();
                        automaton.rule = rule;
                        automaton.cells = new int[CELLS];
                        automaton.cells[CELLS / 2] = 1;
                        automaton.repaint();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Please enter a number between 0 and 255.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            frame.setVisible(true);
        });
    }
}
