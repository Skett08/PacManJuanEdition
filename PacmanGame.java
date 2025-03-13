package JuegosHechos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class PacmanGame extends JFrame {
    private GamePanel game;

    public PacmanGame() {
        initUI();
    }

    private void initUI() {
        game = new GamePanel();
        add(game);
        setTitle("Pacman");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            PacmanGame pg = new PacmanGame();
            pg.setVisible(true);
        });
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
    private final int TILE_SIZE = 40;
    private final int ROWS = 15;
    private final int COLS = 15;

    private int[][] maze;
    private Pacman pacman;
    private ArrayList<Ghost> ghosts;
    private int score;
    private javax.swing.Timer timer;
    private boolean gameOver;
    private boolean isInvincible;
    private int invincibleTime;
    private boolean gameWon;
    private boolean paused;
    private JButton restartButton;
    private JButton resumeButton;

    public GamePanel() {
        addKeyListener(this);
        setFocusable(true);
        setBackground(Color.BLACK);
        initGame();
    }

    private void initGame() {
        createMaze();
        pacman = new Pacman(1, 1);
        ghosts = new ArrayList<>();
        ghosts.add(new Ghost(5, 5));
        ghosts.add(new Ghost(5, 10));
        ghosts.add(new Ghost(10, 5));
        ghosts.add(new Ghost(10, 10));
        score = 0;
        gameOver = false;
        gameWon = false;
        isInvincible = false;
        paused = false;
        timer = new javax.swing.Timer(150, this);
        timer.start();

        setupButtons();
    }

    private void setupButtons() {
        restartButton = new JButton("Reiniciar");
        restartButton.setBounds(200, 400, 120, 40);
        restartButton.addActionListener(e -> resetGame());

        resumeButton = new JButton("Continuar");
        resumeButton.setBounds(320, 400, 120, 40);
        resumeButton.addActionListener(e -> togglePause());

        this.setLayout(null);
        this.add(restartButton);
        this.add(resumeButton);
        restartButton.setVisible(false);
        resumeButton.setVisible(false);
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            timer.stop();
        } else {
            timer.start();
        }
        updateButtonVisibility();
        repaint();
    }

    private void updateButtonVisibility() {
        restartButton.setVisible(paused || gameOver || gameWon);
        resumeButton.setVisible(paused && !gameOver && !gameWon);
    }

    private void resetGame() {
        gameOver = false;
        gameWon = false;
        paused = false;
        timer.stop();
        removeAll();
        initGame();
        updateButtonVisibility();
        requestFocusInWindow();
        repaint();
    }

    private void createMaze() {
        maze = new int[ROWS][COLS];
        // Inicializar bordes como paredes (0)
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (i == 0 || i == ROWS - 1 || j == 0 || j == COLS - 1) {
                    maze[i][j] = 0;
                } else {
                    maze[i][j] = (Math.random() < 0.7) ? 1 : 0;
                }
            }
        }
        maze[1][1] = 1;
        int[][] ghostPositions = { { 5, 5 }, { 5, 10 }, { 10, 5 }, { 10, 10 } };
        for (int[] pos : ghostPositions) {
            int y = pos[0];
            int x = pos[1];
            maze[y][x] = 1;
        }

        boolean[][] reachable = new boolean[ROWS][COLS];
        floodFill(1, 1, reachable);

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if ((maze[i][j] == 1 || maze[i][j] == 3) && !reachable[i][j]) {
                    maze[i][j] = 0;
                }
            }
        }

        placePowerPellets();
    }

    private void floodFill(int startX, int startY, boolean[][] reachable) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        reachable[startY][startX] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int x = p.x;
            int y = p.y;

            checkAndEnqueue(x + 1, y, reachable, queue);
            checkAndEnqueue(x - 1, y, reachable, queue);
            checkAndEnqueue(x, y + 1, reachable, queue);
            checkAndEnqueue(x, y - 1, reachable, queue);
        }
    }

    private void checkAndEnqueue(int x, int y, boolean[][] reachable, Queue<Point> queue) {
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) {
            if (!reachable[y][x] && maze[y][x] != 0) {
                reachable[y][x] = true;
                queue.add(new Point(x, y));
            }
        }
    }

    private void placePowerPellets() {
        int numPowerPellets = 4;
        ArrayList<Point> candidates = new ArrayList<>();

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (maze[i][j] == 1) {
                    candidates.add(new Point(j, i));
                }
            }
        }

        Collections.shuffle(candidates);

        int placed = 0;
        for (Point p : candidates) {
            if (placed >= numPowerPellets)
                break;
            maze[p.y][p.x] = 3;
            placed++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMaze(g);
        drawPacman(g);
        drawGhosts(g);
        drawScore(g);

        if (gameOver)
            drawGameOver(g);
        if (gameWon)
            drawGameWon(g);
        if (paused && !gameOver && !gameWon)
            drawPauseScreen(g);
    }

    private void drawMaze(Graphics g) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (maze[i][j] == 0) {
                    g.setColor(Color.BLUE);
                    g.fillRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (maze[i][j] == 1) {
                    g.setColor(Color.WHITE);
                    g.fillOval(j * TILE_SIZE + TILE_SIZE / 2 - 3,
                            i * TILE_SIZE + TILE_SIZE / 2 - 3, 6, 6);
                } else if (maze[i][j] == 3) {
                    g.setColor(Color.WHITE);
                    g.fillOval(j * TILE_SIZE + TILE_SIZE / 2 - 8,
                            i * TILE_SIZE + TILE_SIZE / 2 - 8, 16, 16);
                }
            }
        }
    }

    private void drawPacman(Graphics g) {
        g.setColor(isInvincible ? new Color(255, 255, 0, 128) : Color.YELLOW);
        g.fillArc(pacman.x * TILE_SIZE + 5,
                pacman.y * TILE_SIZE + 5,
                TILE_SIZE - 10,
                TILE_SIZE - 10,
                pacman.getDirectionAngle(), 300);
    }

    private void drawGhosts(Graphics g) {
        for (Ghost ghost : ghosts) {
            g.setColor(isInvincible ? Color.CYAN : ghost.color);
            g.fillRect(ghost.x * TILE_SIZE + 5,
                    ghost.y * TILE_SIZE + 5,
                    TILE_SIZE - 10,
                    TILE_SIZE - 10);
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Puntuación: " + score, 10, 20);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("GAME OVER", getWidth() / 2 - 120, getHeight() / 2);
    }

    private void drawGameWon(Graphics g) {
        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("¡VICTORIA!", getWidth() / 2 - 120, getHeight() / 2);
    }

    private void drawPauseScreen(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("PAUSA", getWidth() / 2 - 80, getHeight() / 2 - 50);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !gameWon && !paused) {
            pacman.move(maze);
            for (Ghost ghost : ghosts)
                ghost.move(maze);
            checkCollisions();
            checkPoints();
            updateInvincibility();
            checkWinCondition();
            repaint();
        }
    }

    private void checkPoints() {
        int cellValue = maze[pacman.y][pacman.x];
        if (cellValue == 1 || cellValue == 3) {
            maze[pacman.y][pacman.x] = 2;
            score += (cellValue == 3) ? 50 : 10;
            if (cellValue == 3)
                activateInvincibility();
        }
    }

    private void activateInvincibility() {
        isInvincible = true;
        invincibleTime = 10000;
        for (Ghost ghost : ghosts)
            ghost.setFrightened(true);
    }

    private void updateInvincibility() {
        if (isInvincible && (invincibleTime -= 150) <= 0) {
            isInvincible = false;
            for (Ghost ghost : ghosts)
                ghost.setFrightened(false);
        }
    }

    private void checkCollisions() {
        for (Ghost ghost : ghosts) {
            if (pacman.x == ghost.x && pacman.y == ghost.y) {
                if (isInvincible) {
                    ghost.respawn();
                    score += 200;
                } else {
                    gameOver = true;
                    timer.stop();
                    updateButtonVisibility();
                }
            }
        }
    }

    private void checkWinCondition() {
        for (int[] row : maze) {
            for (int cell : row) {
                if (cell == 1 || cell == 3)
                    return;
            }
        }
        gameWon = true;
        timer.stop();
        updateButtonVisibility();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_LEFT:
                pacman.setNextDirection(Direction.LEFT);
                break;
            case KeyEvent.VK_RIGHT:
                pacman.setNextDirection(Direction.RIGHT);
                break;
            case KeyEvent.VK_UP:
                pacman.setNextDirection(Direction.UP);
                break;
            case KeyEvent.VK_DOWN:
                pacman.setNextDirection(Direction.DOWN);
                break;
            case KeyEvent.VK_ESCAPE:
                if (!gameOver && !gameWon)
                    togglePause();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}

class Pacman {
    int x, y;
    Direction currentDir;
    Direction nextDir;

    public Pacman(int x, int y) {
        this.x = x;
        this.y = y;
        currentDir = Direction.RIGHT;
        nextDir = Direction.RIGHT;
    }

    public void move(int[][] maze) {
        if (canMove(nextDir, maze))
            currentDir = nextDir;
        if (canMove(currentDir, maze)) {
            switch (currentDir) {
                case LEFT:
                    x--;
                    break;
                case RIGHT:
                    x++;
                    break;
                case UP:
                    y--;
                    break;
                case DOWN:
                    y++;
                    break;
            }
        }
    }

    private boolean canMove(Direction dir, int[][] maze) {
        int newX = x, newY = y;
        switch (dir) {
            case LEFT:
                newX--;
                break;
            case RIGHT:
                newX++;
                break;
            case UP:
                newY--;
                break;
            case DOWN:
                newY++;
                break;
        }
        return maze[newY][newX] != 0;
    }

    public int getDirectionAngle() {
        return switch (currentDir) {
            case LEFT -> 180;
            case RIGHT -> 0;
            case UP -> 270;
            case DOWN -> 90;
        };
    }

    public void setNextDirection(Direction dir) {
        nextDir = dir;
    }
}

class Ghost {
    int x, y;
    Direction dir;
    Color color;
    Color originalColor;
    Random random = new Random();
    boolean frightened;
    private static final int COLS = 15;
    private static final int ROWS = 15;

    public Ghost(int x, int y) {
        this.x = x;
        this.y = y;
        this.originalColor = new Color(
                random.nextInt(255),
                random.nextInt(255),
                random.nextInt(255));
        this.color = originalColor;
        dir = Direction.values()[random.nextInt(4)];
        frightened = false;
    }

    public void setFrightened(boolean state) {
        frightened = state;
        color = frightened ? Color.CYAN : originalColor;
    }

    public void respawn() {
        x = COLS / 2;
        y = ROWS / 2;
        setFrightened(false);
    }

    public void move(int[][] maze) {
        if (random.nextInt(100) < (frightened ? 50 : 25)) {
            ArrayList<Direction> possibleDirs = new ArrayList<>();
            for (Direction d : Direction.values()) {
                if (canMove(d, maze))
                    possibleDirs.add(d);
            }
            if (!possibleDirs.isEmpty()) {
                dir = possibleDirs.get(random.nextInt(possibleDirs.size()));
            }
        }
        if (canMove(dir, maze)) {
            switch (dir) {
                case LEFT:
                    x--;
                    break;
                case RIGHT:
                    x++;
                    break;
                case UP:
                    y--;
                    break;
                case DOWN:
                    y++;
                    break;
            }
        }
    }

    private boolean canMove(Direction d, int[][] maze) {
        int newX = x, newY = y;
        switch (d) {
            case LEFT:
                newX--;
                break;
            case RIGHT:
                newX++;
                break;
            case UP:
                newY--;
                break;
            case DOWN:
                newY++;
                break;
        }
        return maze[newY][newX] != 0;
    }
}

enum Direction {
    LEFT, RIGHT, UP, DOWN
}