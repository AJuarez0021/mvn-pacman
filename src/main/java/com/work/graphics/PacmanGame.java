package com.work.graphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PacmanGame extends JPanel implements KeyListener, Runnable {

    private static final int UNIT_SIZE = 30;
    private static final int DELAY = 300; // Velocidad más lenta para mejor control

    // Mapa del juego (1 = pared, 0 = punto, 2 = power pellet, 3 = vacío)
    private int[][] gameMap;

    private Player pacman;
    private List<Ghost> ghosts;
    private int score;
    private int totalDots;
    private int dotsEaten;
    private boolean gameOver;
    private boolean gameWon;
    private boolean powerMode;
    private long powerModeStartTime;
    private static final long POWER_MODE_DURATION = 10000; // 10 segundos
    private Thread gameThread;
    private final Random random;
    private boolean running;

    public PacmanGame() {
        gameMap = createMap();
        random = new Random();

        // Obtener dimensiones de la pantalla
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setPreferredSize(screenSize);
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);

        initializeGame();
    }

    private void initializeGame() {
        pacman = new Player(1, 1);
        ghosts = new ArrayList<>();
        ghosts.add(new Ghost(11, 8, Color.RED));
        ghosts.add(new Ghost(12, 8, Color.PINK));
        ghosts.add(new Ghost(11, 9, Color.CYAN));
        ghosts.add(new Ghost(12, 9, Color.ORANGE));

        score = 0;
        dotsEaten = 0;
        gameOver = false;
        gameWon = false;
        powerMode = false;
        running = true;

        // Contar total de puntos
        totalDots = 0;
        for (int[] gameMap1 : gameMap) {
            for (int j = 0; j < gameMap1.length; j++) {
                if (gameMap1[j] == 0 || gameMap1[j] == 2) {
                    totalDots++;
                }
            }
        }

        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    private int[][] createMap() {
        int[][] map = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 2, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 2, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1},
            {3, 3, 3, 3, 3, 1, 0, 1, 1, 1, 1, 3, 3, 1, 1, 1, 1, 0, 1, 3, 3, 3, 3, 3},
            {3, 3, 3, 3, 3, 1, 0, 1, 1, 1, 1, 3, 3, 1, 1, 1, 1, 0, 1, 3, 3, 3, 3, 3},
            {1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 2, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 2, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        return map;
    }

    private void draw(Graphics g) {
        // Obtener dimensiones actuales del panel
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // Calcular offset para centrar el juego
        int gameWidth = gameMap[0].length * UNIT_SIZE;
        int gameHeight = gameMap.length * UNIT_SIZE;
        int offsetX = (panelWidth - gameWidth) / 2;
        int offsetY = (panelHeight - gameHeight) / 2;

        // Dibujar mapa
        for (int i = 0; i < gameMap.length; i++) {
            for (int j = 0; j < gameMap[i].length; j++) {
                int x = j * UNIT_SIZE + offsetX;
                int y = i * UNIT_SIZE + offsetY;

                switch (gameMap[i][j]) {
                    case 1 -> {
                        // Pared
                        g.setColor(Color.BLUE);
                        g.fillRect(x, y, UNIT_SIZE, UNIT_SIZE);
                    }
                    case 0 -> {
                        // Punto normal
                        g.setColor(Color.YELLOW);
                        g.fillOval(x + UNIT_SIZE / 2 - 3, y + UNIT_SIZE / 2 - 3, 6, 6);
                    }
                    case 2 -> {
                        // Power pellet
                        g.setColor(Color.YELLOW);
                        g.fillOval(x + UNIT_SIZE / 2 - 8, y + UNIT_SIZE / 2 - 8, 16, 16);
                    }
                }
            }
        }

        // Dibujar Pacman con fillArc para forma más realista
        g.setColor(Color.YELLOW);
        int pacmanX = pacman.x * UNIT_SIZE + offsetX + 2;
        int pacmanY = pacman.y * UNIT_SIZE + offsetY + 2;
        int pacmanSize = UNIT_SIZE - 4;

        // Ángulo de inicio y amplitud según la dirección
        int startAngle = 0;
        int arcAngle = 300; // Deja 60 grados para la "boca"

        switch (pacman.direction) {
            case 'R' -> // Derecha
                startAngle = 30;
            case 'L' -> // Izquierda
                startAngle = 210;
            case 'U' -> // Arriba
                startAngle = 120;
            case 'D' -> // Abajo
                startAngle = 300;
        }

        g.fillArc(pacmanX, pacmanY, pacmanSize, pacmanSize, startAngle, arcAngle);

        // Dibujar fantasmas con forma clásica ondulada
        for (Ghost ghost : ghosts) {
            if (powerMode && !ghost.eaten) {
                g.setColor(Color.BLUE);
            } else if (ghost.eaten) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(ghost.color);
            }

            int ghostX = ghost.x * UNIT_SIZE + offsetX + 2;
            int ghostY = ghost.y * UNIT_SIZE + offsetY + 2;
            int ghostSize = UNIT_SIZE - 4;

            // Cuerpo principal del fantasma (rectángulo + semicírculo superior)
            // Parte rectangular
            g.fillRect(ghostX, ghostY + ghostSize / 2, ghostSize, ghostSize / 2);

            // Parte superior redondeada (semicírculo)
            g.fillArc(ghostX, ghostY, ghostSize, ghostSize, 0, 180);

            // Crear la parte inferior ondulada característica
            int waveWidth = ghostSize / 4;
            int waveHeight = 6;

            // Primera onda (izquierda)
            g.fillArc(ghostX, ghostY + ghostSize - waveHeight, waveWidth, waveHeight * 2, 180, 180);

            // Segunda onda (centro-izquierda) 
            g.fillArc(ghostX + waveWidth, ghostY + ghostSize - waveHeight, waveWidth, waveHeight * 2, 0, 180);

            // Tercera onda (centro-derecha)
            g.fillArc(ghostX + waveWidth * 2, ghostY + ghostSize - waveHeight, waveWidth, waveHeight * 2, 180, 180);

            // Cuarta onda (derecha)
            g.fillArc(ghostX + waveWidth * 3, ghostY + ghostSize - waveHeight, waveWidth, waveHeight * 2, 0, 180);

            // Ojos blancos grandes y redondos
            g.setColor(Color.WHITE);
            int eyeSize = ghostSize / 4;
            int eyeY = ghostY + ghostSize / 3;
            int eyeSpacing = ghostSize / 3;

            // Ojo izquierdo
            g.fillOval(ghostX + eyeSpacing - eyeSize / 2, eyeY, eyeSize, eyeSize);
            // Ojo derecho  
            g.fillOval(ghostX + ghostSize - eyeSpacing - eyeSize / 2, eyeY, eyeSize, eyeSize);

            // Pupilas negras
            g.setColor(Color.BLACK);
            int pupilSize = eyeSize / 2;

            // Pupilas se mueven según la dirección del fantasma
            int pupilOffsetX = 0;
            int pupilOffsetY = 0;

            switch (ghost.direction) {
                case 'L' ->
                    pupilOffsetX = -2;
                case 'R' ->
                    pupilOffsetX = 2;
                case 'U' ->
                    pupilOffsetY = -1;
                case 'D' ->
                    pupilOffsetY = 1;
            }

            // Pupila izquierda
            g.fillOval(ghostX + eyeSpacing - pupilSize / 2 + pupilOffsetX,
                    eyeY + (eyeSize - pupilSize) / 2 + pupilOffsetY, pupilSize, pupilSize);
            // Pupila derecha
            g.fillOval(ghostX + ghostSize - eyeSpacing - pupilSize / 2 + pupilOffsetX,
                    eyeY + (eyeSize - pupilSize) / 2 + pupilOffsetY, pupilSize, pupilSize);
        }

        // Dibujar puntuación
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 20, 40);

        // Mostrar modo power
        if (powerMode) {
            g.setColor(Color.RED);
            g.drawString("POWER MODE!", 20, 70);
        }

        // Mostrar controles
        if (!gameOver && !gameWon) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Controls: Arrow Keys/WASD | ESC to exit", 20, panelHeight - 20);
        }

        FontMetrics metrics;
        // Pantalla de game over
        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            metrics = getFontMetrics(g.getFont());
            g.drawString("Game Over", (panelWidth - metrics.stringWidth("Game Over")) / 2, panelHeight / 2);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            metrics = getFontMetrics(g.getFont());
            g.drawString("Press R to restart | Press ESC to exit", (panelWidth - metrics.stringWidth("Press R to restart | Press ESC to exit")) / 2, panelHeight / 2 + 80);
        }

        // Pantalla de victoria
        if (gameWon) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 60));
            metrics = getFontMetrics(g.getFont());
            g.drawString("You Win!", (panelWidth - metrics.stringWidth("You Win!")) / 2, panelHeight / 2);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            metrics = getFontMetrics(g.getFont());
            g.drawString("Press R to restart | Press ESC to exit", (panelWidth - metrics.stringWidth("Press R to restart | Press ESC to exit")) / 2, panelHeight / 2 + 80);
        }
    }

    private void move() {
        if (gameOver || gameWon) {
            return;
        }

        // Mover Pacman
        pacman.move();

        // Verificar colisiones con paredes
        if (gameMap[pacman.y][pacman.x] == 1) {
            pacman.x = pacman.prevX;
            pacman.y = pacman.prevY;
        }

        // Túnel horizontal
        if (pacman.x < 0) {
            pacman.x = gameMap[0].length - 1;
        }
        if (pacman.x >= gameMap[0].length) {
            pacman.x = 0;
        }

        // Comer puntos
        if (gameMap[pacman.y][pacman.x] == 0) {
            gameMap[pacman.y][pacman.x] = 3;
            score += 10;
            dotsEaten++;
        } else if (gameMap[pacman.y][pacman.x] == 2) {
            gameMap[pacman.y][pacman.x] = 3;
            score += 50;
            dotsEaten++;
            activatePowerMode();
        }

        // Verificar victoria
        if (dotsEaten >= totalDots) {
            gameWon = true;
            return;
        }

        // Mover fantasmas
        for (Ghost ghost : ghosts) {
            ghost.move(gameMap, pacman);
        }

        // Verificar colisiones con fantasmas
        checkGhostCollisions();

        // Verificar si el modo power ha terminado
        if (powerMode && System.currentTimeMillis() - powerModeStartTime > POWER_MODE_DURATION) {
            deactivatePowerMode();
        }
    }

    private void activatePowerMode() {
        powerMode = true;
        powerModeStartTime = System.currentTimeMillis();
        for (Ghost ghost : ghosts) {
            ghost.vulnerable = true;
        }
    }

    private void deactivatePowerMode() {
        powerMode = false;
        for (Ghost ghost : ghosts) {
            ghost.vulnerable = false;
            if (ghost.eaten) {
                ghost.eaten = false;
                ghost.x = 11;
                ghost.y = 8;
            }
        }
    }

    private void checkGhostCollisions() {
        for (Ghost ghost : ghosts) {
            if (ghost.x == pacman.x && ghost.y == pacman.y) {
                if (ghost.vulnerable && !ghost.eaten) {
                    ghost.eaten = true;
                    score += 200;
                } else if (!ghost.eaten) {
                    gameOver = true;
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A ->
                pacman.setDirection('L');
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D ->
                pacman.setDirection('R');
            case KeyEvent.VK_UP, KeyEvent.VK_W ->
                pacman.setDirection('U');
            case KeyEvent.VK_DOWN, KeyEvent.VK_S ->
                pacman.setDirection('D');
            case KeyEvent.VK_R -> {
                if (gameOver || gameWon) {
                    restart();
                }
            }
            case KeyEvent.VK_ESCAPE ->
                System.exit(0);
        }
    }

    private void restart() {
        running = false;
        // Reinicializar el mapa
        gameMap = createMap();
        initializeGame();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void run() {
        while (running) {
            move();
            repaint();
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    // Clase Player
    class Player {

        int x, y;
        int prevX, prevY;
        char direction;
        char nextDirection;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.direction = 'R';
            this.nextDirection = 'R';
        }

        void move() {
            prevX = x;
            prevY = y;

            // Intentar cambiar de dirección
            int nextX = x;
            int nextY = y;

            switch (nextDirection) {
                case 'U' ->
                    nextY--;
                case 'D' ->
                    nextY++;
                case 'L' ->
                    nextX--;
                case 'R' ->
                    nextX++;
            }

            // Verificar túnel horizontal
            if (nextX < 0) {
                nextX = gameMap[0].length - 1;
            }
            if (nextX >= gameMap[0].length) {
                nextX = 0;
            }

            // Si el siguiente movimiento es válido, cambiar dirección
            if (nextY >= 0 && nextY < gameMap.length && gameMap[nextY][nextX] != 1) {
                direction = nextDirection;
            }

            // Mover en la dirección actual
            switch (direction) {
                case 'U' ->
                    y--;
                case 'D' ->
                    y++;
                case 'L' ->
                    x--;
                case 'R' ->
                    x++;
            }
        }

        void setDirection(char dir) {
            nextDirection = dir;
        }
    }

    // Clase Ghost
    class Ghost {

        int x, y;
        Color color;
        char direction;
        boolean vulnerable;
        boolean eaten;

        Ghost(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.direction = 'U';
            this.vulnerable = false;
            this.eaten = false;
        }

        void move(int[][] map, Player pacman) {
            if (eaten) {
                return;
            }

            // IA simple: perseguir a Pacman o huir en modo vulnerable
            int targetX = pacman.x;
            int targetY = pacman.y;

            if (vulnerable) {
                // Huir de Pacman
                targetX = x + (x - pacman.x);
                targetY = y + (y - pacman.y);
            }

            // Encontrar la mejor dirección
            char[] directions = {'U', 'D', 'L', 'R'};
            char bestDirection = direction;
            double bestDistance = Double.MAX_VALUE;

            for (char dir : directions) {
                int nextX = x;
                int nextY = y;

                switch (dir) {
                    case 'U' ->
                        nextY--;
                    case 'D' ->
                        nextY++;
                    case 'L' ->
                        nextX--;
                    case 'R' ->
                        nextX++;
                }

                // Verificar túnel horizontal
                if (nextX < 0) {
                    nextX = map[0].length - 1;
                }
                if (nextX >= map[0].length) {
                    nextX = 0;
                }

                // Verificar si es una posición válida
                if (nextY >= 0 && nextY < map.length && map[nextY][nextX] != 1) {
                    double distance = Math.sqrt(Math.pow(nextX - targetX, 2) + Math.pow(nextY - targetY, 2));

                    if (vulnerable) {
                        // En modo vulnerable, elegir la mayor distancia
                        if (distance > bestDistance) {
                            bestDistance = distance;
                            bestDirection = dir;
                        }
                    } else {
                        // En modo normal, elegir la menor distancia
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestDirection = dir;
                        }
                    }
                }
            }

            // Movimiento aleatorio ocasional para evitar quedarse atascado
            if (random.nextInt(10) == 0) {
                char[] validDirections = {'U', 'D', 'L', 'R'};
                bestDirection = validDirections[random.nextInt(validDirections.length)];
            }

            direction = bestDirection;

            // Mover en la dirección elegida
            switch (direction) {
                case 'U' ->
                    y--;
                case 'D' ->
                    y++;
                case 'L' ->
                    x--;
                case 'R' ->
                    x++;
            }

            // Verificar túnel horizontal
            if (x < 0) {
                x = map[0].length - 1;
            }
            if (x >= map[0].length) {
                x = 0;
            }

            // Verificar colisión con paredes
            if (y >= 0 && y < map.length && map[y][x] == 1) {
                // Retroceder si hay colisión
                switch (direction) {
                    case 'U' ->
                        y++;
                    case 'D' ->
                        y--;
                    case 'L' ->
                        x++;
                    case 'R' ->
                        x--;
                }
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pacman Game");
        PacmanGame game = new PacmanGame();

        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true); // Quitar barra de título para pantalla completa
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
