package com.example.demo.view;

import com.example.demo.model.Player;
import com.example.demo.model.Enemy;
import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    public final Player player;
    public boolean isBattleMode = false;
    // Используем Rectangle из java.awt для кнопки
    public Rectangle endTurnBtn = new Rectangle(600, 500, 150, 40);

    public GamePanel(Player player) {
        this.player = player;
        this.setFocusable(true);
        this.setBackground(Color.DARK_GRAY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Включаем сглаживание текста, чтобы цифры были красивыми
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isBattleMode) {
            drawBattle(g2);
        } else {
            drawWorld(g2);
        }
    }

    private void drawWorld(Graphics2D g) {
        // Рисуем портал
        g.setColor(Color.MAGENTA);
        g.fillRect(600, 400, 60, 60);

        // Рисуем игрока
        g.setColor(Color.CYAN);
        g.fillRect(player.x, player.y, player.size, player.size);

        g.setColor(Color.WHITE);
        g.drawString("Дойди до розового квадрата (портала)", 20, 20);
    }

    private void drawBattle(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("ВАШ HP: " + player.hp, 50, 50);

        // Рисуем врагов (Критерий 2.6: NPC со значениями)
        for (int i = 0; i < player.currentEnemies.size(); i++) {
            g.setColor(Color.RED);
            g.fillRoundRect(100 + i * 150, 150, 80, 80, 15, 15);
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(player.currentEnemies.get(i).value), 130 + i * 150, 200);
        }

        // Рисуем карты игрока (Критерий 2.1: Механика карт)
        for (int i = 0; i < player.hand.size(); i++) {
            // Подсветка выбранной карты
            if (i == player.selectedCardIndex) {
                g.setColor(Color.YELLOW);
                g.setStroke(new BasicStroke(4));
                g.drawRoundRect(98 + i * 80, 398, 64, 94, 10, 10);
            }

            g.setColor(Color.ORANGE);
            g.fillRoundRect(100 + i * 80, 400, 60, 90, 10, 10);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString(String.valueOf(player.hand.get(i)), 122 + i * 80, 455);
        }

        // Кнопка завершения хода (Критерий 2.2: Взаимодействие мышью)
        g.setColor(Color.LIGHT_GRAY);
        g.fill(endTurnBtn);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("ЗАКОНЧИТЬ ХОД", endTurnBtn.x + 20, endTurnBtn.y + 25);
    }
}