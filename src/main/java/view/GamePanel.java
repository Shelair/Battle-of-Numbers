package com.example.demo.view;

import com.example.demo.model.Player;
import com.example.demo.model.Enemy;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.awt.Rectangle;

public class GamePanel extends JPanel {
    public final Player player;
    public boolean isBattleMode = false;

    public Rectangle endTurnBtn = new Rectangle(600, 500, 160, 45);

    private BufferedImage idleRight, idleLeft;
    private BufferedImage[] walkRight = new BufferedImage[3];
    private BufferedImage[] walkLeft = new BufferedImage[3];
    private BufferedImage[] portalFrames = new BufferedImage[2];

    public GamePanel(Player player) {
        this.player = player;
        this.setFocusable(true);
        this.setBackground(Color.DARK_GRAY);
        loadSprites();
    }

    /**
     * Метод для удаления белого фона, если он есть в файле.
     */
    private BufferedImage makeTransparent(BufferedImage img) {
        if (img == null) return null;

        // Создаем копию изображения с поддержкой прозрачности (ARGB)
        BufferedImage transparentImage = new BufferedImage(
                img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = transparentImage.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        // Проходим по пикселям: если пиксель белый, делаем его прозрачным
        for (int y = 0; y < transparentImage.getHeight(); y++) {
            for (int x = 0; x < transparentImage.getWidth(); x++) {
                int rgb = transparentImage.getRGB(x, y);
                // 0xFFFFFFFF - это чисто белый цвет (Alpha, R, G, B)
                if (rgb == 0xFFFFFFFF || (rgb & 0x00FFFFFF) == 0x00FFFFFF) {
                    transparentImage.setRGB(x, y, 0x00FFFFFF); // Устанавливаем прозрачность
                }
            }
        }
        return transparentImage;
    }

    private void loadSprites() {
        // Оборачиваем загрузку каждого спрайта в makeTransparent
        idleRight = makeTransparent(ResourceLoader.loadImage("player_idle_right.png"));
        idleLeft = makeTransparent(ResourceLoader.loadImage("player_idle_left.png"));

        for (int i = 0; i < 3; i++) {
            walkRight[i] = makeTransparent(ResourceLoader.loadImage("player_walk_right_" + (i + 1) + ".png"));
            walkLeft[i] = makeTransparent(ResourceLoader.loadImage("player_walk_left_" + (i + 1) + ".png"));
        }

        portalFrames[0] = makeTransparent(ResourceLoader.loadImage("portal_1.png"));
        portalFrames[1] = makeTransparent(ResourceLoader.loadImage("portal_2.png"));
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Обязательно вызываем super, чтобы фон панели (DARK_GRAY) отрисовался правильно
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isBattleMode) {
            drawBattle(g2);
        } else {
            drawWorld(g2);
        }
    }

    private void drawWorld(Graphics2D g2) {
        // 1. Анимация портала
        int portalFrame = (int)(System.currentTimeMillis() / 500 % 2);
        if (portalFrames[portalFrame] != null) {
            g2.drawImage(portalFrames[portalFrame], 600, 400, 64, 64, null);
        } else {
            g2.setColor(Color.MAGENTA);
            g2.fillRect(600, 400, 60, 60);
        }

        // 2. Выбор спрайта
        BufferedImage currentSprite;
        if (!player.isMoving) {
            currentSprite = player.faceRight ? idleRight : idleLeft;
        } else {
            currentSprite = player.faceRight ? walkRight[player.animationFrame] : walkLeft[player.animationFrame];
        }

        // 3. Отрисовка игрока
        if (currentSprite != null) {
            g2.drawImage(currentSprite, player.x, player.y, player.size, player.size, null);
        } else {
            g2.setColor(Color.CYAN);
            g2.fillRect(player.x, player.y, player.size, player.size);
        }
    }

    private void drawBattle(Graphics2D g2) {
        // Код битвы остается без изменений  ц
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("ВАШ HP: " + player.hp, 50, 50);

        for (int i = 0; i < player.currentEnemies.size(); i++) {
            g2.setColor(Color.RED);
            g2.fillRoundRect(100 + i * 150, 150, 80, 80, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString(String.valueOf(player.currentEnemies.get(i).value), 130 + i * 150, 200);
        }

        for (int i = 0; i < player.hand.size(); i++) {
            if (i == player.selectedCardIndex) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(98 + i * 80, 398, 64, 94, 10, 10);
            }
            g2.setColor(Color.ORANGE);
            g2.fillRoundRect(100 + i * 80, 400, 60, 90, 10, 10);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString(String.valueOf(player.hand.get(i)), 122 + i * 80, 455);
        }

        g2.setColor(Color.LIGHT_GRAY);
        g2.fill(endTurnBtn);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("ЗАКОНЧИТЬ ХОД", endTurnBtn.x + 20, endTurnBtn.y + 28);
    }
}