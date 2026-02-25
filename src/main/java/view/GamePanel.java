package com.example.demo.view;

import com.example.demo.model.Player;
import com.example.demo.model.Enemy;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class GamePanel extends JPanel {
    public final Player player;
    public boolean isBattleMode = false;

    public Rectangle endTurnBtn = new Rectangle(580, 380, 180, 50);

    private BufferedImage idleRight, idleLeft;
    private BufferedImage[] walkRight = new BufferedImage[3];
    private BufferedImage[] walkLeft = new BufferedImage[3];
    private BufferedImage[] portalFrames = new BufferedImage[2];

    private Map<Integer, BufferedImage> cardSprites = new HashMap<>();
    private Map<Integer, BufferedImage> enemySprites = new HashMap<>();

    public int attackingEnemyIndex = -1;
    public int enemyAnimOffsetY = 0;
    public int enemyAnimOffsetX = 0;
    public int damagedEnemyIndex = -1;
    public int enemyHurtOffsetY = 0;

    public GamePanel(Player player) {
        this.player = player;
        this.setFocusable(true);
        this.setBackground(Color.DARK_GRAY);
        loadSprites();
    }

    private BufferedImage makeTransparent(BufferedImage img) {
        if (img == null) return null;
        BufferedImage nit = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = nit.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        for (int y = 0; y < nit.getHeight(); y++) {
            for (int x = 0; x < nit.getWidth(); x++) {
                int rgb = nit.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0x00FFFFFF) {
                    nit.setRGB(x, y, 0x00FFFFFF);
                }
            }
        }
        return nit;
    }

    private void loadSprites() {
        // Используем правильный путь к папке sprites
        String path = "/sprites/";

        idleRight = makeTransparent(ResourceLoader.loadImage(path + "player_idle_right.png"));
        idleLeft = makeTransparent(ResourceLoader.loadImage(path + "player_idle_left.png"));

        for (int i = 0; i < 3; i++) {
            walkRight[i] = makeTransparent(ResourceLoader.loadImage(path + "player_walk_right_" + (i + 1) + ".png"));
            walkLeft[i] = makeTransparent(ResourceLoader.loadImage(path + "player_walk_left_" + (i + 1) + ".png"));
        }

        portalFrames[0] = makeTransparent(ResourceLoader.loadImage(path + "portal_1.png"));
        portalFrames[1] = makeTransparent(ResourceLoader.loadImage(path + "portal_2.png"));

        // Загружаем строго от 0 до 9
        for (int i = 0; i <= 9; i++) {
            cardSprites.put(i, ResourceLoader.loadImage(path + "card_" + i + ".png"));
            enemySprites.put(i, ResourceLoader.loadImage(path + "enemy_" + i + ".png"));
        }
    }

    public void animateEnemyAttack(int index) {
        this.attackingEnemyIndex = index;
        Timer timer = new Timer(20, null);
        final int[] frame = {0};
        timer.addActionListener(e -> {
            frame[0]++;
            if (frame[0] <= 10) {
                enemyAnimOffsetX -= 6;
                enemyAnimOffsetY -= 2;
            } else if (frame[0] <= 20) {
                enemyAnimOffsetX += 6;
                enemyAnimOffsetY += 2;
            } else {
                attackingEnemyIndex = -1;
                enemyAnimOffsetX = 0;
                enemyAnimOffsetY = 0;
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });
        timer.start();
    }

    public void animateEnemyHurt(int index) {
        this.damagedEnemyIndex = index;
        Timer timer = new Timer(30, null);
        final int[] frame = {0};
        timer.addActionListener(e -> {
            frame[0]++;
            if (frame[0] <= 5) enemyHurtOffsetY = -15;
            else if (frame[0] <= 10) enemyHurtOffsetY = 0;
            else {
                damagedEnemyIndex = -1;
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });
        timer.start();
    }

    private void drawDarkenedImage(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        Composite oldComposite = g2.getComposite();
        g2.drawImage(img, x, y, w, h, null);
        g2.setComposite(AlphaComposite.SrcAtop.derive(0.6f));
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y, w, h);
        g2.setComposite(oldComposite);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isBattleMode) drawBattle(g2);
        else drawWorld(g2);
    }

    private void drawWorld(Graphics2D g2) {
        int portalFrame = (int)(System.currentTimeMillis() / 500 % 2);
        if (portalFrames[portalFrame] != null) {
            g2.drawImage(portalFrames[portalFrame], 600, 400, 64, 64, null);
        }
        BufferedImage currentSprite = (!player.isMoving) ? (player.faceRight ? idleRight : idleLeft) : (player.faceRight ? walkRight[player.animationFrame] : walkLeft[player.animationFrame]);
        if (currentSprite != null) g2.drawImage(currentSprite, player.x, player.y, player.size, player.size, null);
    }

    private void drawBattle(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("HEAVENLY LION HP: " + player.hp, 50, 50);

        // --- ВРАГИ ---
        for (int i = 0; i < player.currentEnemies.size(); i++) {
            Enemy enemy = player.currentEnemies.get(i);

            // Если враг умер, мы его просто не рисуем, но i продолжает идти,
            // поэтому следующие враги не сдвигаются!
            if (enemy.value <= 0) continue;

            int ex = 80 + i * 180;
            int ey = 130;
            int eSize = 120;

            if (i == attackingEnemyIndex) { ex += enemyAnimOffsetX; ey += enemyAnimOffsetY; }
            if (i == damagedEnemyIndex) { ey += enemyHurtOffsetY; }

            BufferedImage es = enemySprites.get(enemy.value);
            if (es != null) {
                if (i == damagedEnemyIndex) drawDarkenedImage(g2, es, ex, ey, eSize, eSize);
                else g2.drawImage(es, ex, ey, eSize, eSize, null);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("HP: " + enemy.value, ex + 35, ey + eSize + 25);
        }

        // --- КАРТЫ ---
        for (int i = 0; i < player.hand.size(); i++) {
            Integer val = player.hand.get(i);

            // Если карта использована (null), рисуем пустоту, слоты не смещаются
            if (val == null) continue;

            int cx = 80 + i * 110;
            int cy = 450;
            int cW = 100;
            int cH = 145;

            if (i == player.selectedCardIndex) cy -= 30;

            BufferedImage cs = cardSprites.get(val);
            if (cs != null) {
                g2.drawImage(cs, cx, cy, cW, cH, null);
            }
        }

        g2.setColor(Color.LIGHT_GRAY);
        g2.fill(endTurnBtn);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("ЗАКОНЧИТЬ ХОД", endTurnBtn.x + 20, endTurnBtn.y + 30);
    }
}