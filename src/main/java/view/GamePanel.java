package com.example.demo.view;

import com.example.demo.model.Player;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.awt.Rectangle;

public class GamePanel extends JPanel {
    public final Player player;
    public boolean isBattleMode = false;

    public Rectangle endTurnBtn = new Rectangle(600, 500, 160, 45);

    // Спрайты персонажа и портала
    private BufferedImage idleRight, idleLeft;
    private BufferedImage[] walkRight = new BufferedImage[3];
    private BufferedImage[] walkLeft = new BufferedImage[3];
    private BufferedImage[] portalFrames = new BufferedImage[2];

    // Хранилище для спрайтов карт: ключ - число на карте, значение - картинка
    private Map<Integer, BufferedImage> cardSprites = new HashMap<>();

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
                // Убираем чисто белый фон
                if (rgb == 0xFFFFFFFF || (rgb & 0x00FFFFFF) == 0x00FFFFFF) {
                    nit.setRGB(x, y, 0x00FFFFFF);
                }
            }
        }
        return nit;
    }

    private void loadSprites() {
        // Загрузка игрока и портала
        idleRight = makeTransparent(ResourceLoader.loadImage("player_idle_right.png"));
        idleLeft = makeTransparent(ResourceLoader.loadImage("player_idle_left.png"));
        for (int i = 0; i < 3; i++) {
            walkRight[i] = makeTransparent(ResourceLoader.loadImage("player_walk_right_" + (i + 1) + ".png"));
            walkLeft[i] = makeTransparent(ResourceLoader.loadImage("player_walk_left_" + (i + 1) + ".png"));
        }
        portalFrames[0] = makeTransparent(ResourceLoader.loadImage("portal_1.png"));
        portalFrames[1] = makeTransparent(ResourceLoader.loadImage("portal_2.png"));

        // ЗАГРУЗКА КАРТ (от 1 до 10, можно увеличить)
        for (int i = 1; i <= 10; i++) {
            BufferedImage img = ResourceLoader.loadImage("card_" + i + ".png");
            if (img != null) {
                cardSprites.put(i, makeTransparent(img));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
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
        // Анимация портала
        int portalFrame = (int)(System.currentTimeMillis() / 500 % 2);
        if (portalFrames[portalFrame] != null) {
            g2.drawImage(portalFrames[portalFrame], 600, 400, 64, 64, null);
        }

        // Выбор спрайта игрока
        BufferedImage currentSprite = (!player.isMoving) ?
                (player.faceRight ? idleRight : idleLeft) :
                (player.faceRight ? walkRight[player.animationFrame] : walkLeft[player.animationFrame]);

        if (currentSprite != null) {
            g2.drawImage(currentSprite, player.x, player.y, player.size, player.size, null);
        }
    }

    private void drawBattle(Graphics2D g2) {
        // Отрисовка HP
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("ВАШ HP: " + player.hp, 50, 50);

        // Враги
        for (int i = 0; i < player.currentEnemies.size(); i++) {
            g2.setColor(Color.RED);
            g2.fillRoundRect(100 + i * 150, 150, 80, 80, 15, 15);
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf(player.currentEnemies.get(i).value), 135 + i * 150, 200);
        }

        // ОТРИСОВКА ВАШИХ КАРТ
        for (int i = 0; i < player.hand.size(); i++) {
            int cardValue = player.hand.get(i);
            int cardX = 100 + i * 85;
            int cardY = 400;
            int cardW = 75;
            int cardH = 110;

            // Если карта выбрана, она "подпрыгивает" на 20 пикселей вверх
            if (i == player.selectedCardIndex) {
                cardY -= 20;
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(cardX - 2, cardY - 2, cardW + 4, cardH + 4, 10, 10);
            }

            BufferedImage img = cardSprites.get(cardValue);
            if (img != null) {
                g2.drawImage(img, cardX, cardY, cardW, cardH, null);
            } else {
                // Если забыли нарисовать спрайт для карты - рисуем заглушку
                g2.setColor(Color.ORANGE);
                g2.fillRoundRect(cardX, cardY, cardW, cardH, 10, 10);
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(cardValue), cardX + 25, cardY + 60);
            }
        }

        // Кнопочка
        g2.setColor(Color.LIGHT_GRAY);
        g2.fill(endTurnBtn);
        g2.setColor(Color.BLACK);
        g2.drawString("ЗАКОНЧИТЬ ХОД", endTurnBtn.x + 20, endTurnBtn.y + 28);
    }
}