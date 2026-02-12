package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.view.GamePanel;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.Random;

public class GameController extends MouseAdapter implements Runnable {
    private Player player;
    private GamePanel panel;
    private boolean[] keys = new boolean[256];
    private Random rnd = new Random();
    private boolean running = true;
    private boolean wasAttackUsed = false;

    public GameController(Player player, GamePanel panel) {
        this.player = player;
        this.panel = panel;
        panel.addMouseListener(this);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!panel.isBattleMode) return;

        int mx = e.getX();
        int my = e.getY();

        // 1. Кнопка "Закончить ход"
        if (panel.endTurnBtn.contains(mx, my)) {
            endPlayerTurn();
            return;
        }

        // 2. Выбор карты (Rectangle создаем прямо в цикле для проверки клика)
        for (int i = 0; i < player.hand.size(); i++) {
            if (new Rectangle(100 + i * 80, 400, 60, 90).contains(mx, my)) {
                player.selectedCardIndex = i;
                return;
            }
        }

        // 3. Удар по врагу
        if (player.selectedCardIndex != -1) {
            for (int i = 0; i < player.currentEnemies.size(); i++) {
                if (new Rectangle(100 + i * 150, 150, 80, 80).contains(mx, my)) {
                    int dmg = player.hand.remove(player.selectedCardIndex);
                    player.currentEnemies.get(i).value -= dmg;
                    wasAttackUsed = true;
                    if (player.currentEnemies.get(i).value <= 0) player.currentEnemies.remove(i);
                    player.selectedCardIndex = -1;
                    if (player.currentEnemies.isEmpty()) panel.isBattleMode = false;
                    return;
                }
            }
        }
    }

    private void endPlayerTurn() {
        if (!wasAttackUsed) {
            for(int i = 0; i < 2; i++) player.hand.add(rnd.nextInt(9) + 1);
        }
        for (Enemy en : player.currentEnemies) {
            player.hp -= (rnd.nextInt(en.value) + 1);
        }
        wasAttackUsed = false;
        player.selectedCardIndex = -1;

        // Проверка на смерть (Game Over)
        if (player.hp <= 0) {
            System.out.println("Вы проиграли!");
            // Тут можно добавить сброс игры или переход в меню
        }
    }

    public void update() {
        if (panel.isBattleMode) return; // В бою ходить нельзя

        boolean movingNow = false;
        if (keys[KeyEvent.VK_W]) { player.y -= player.speed; movingNow = true; }
        if (keys[KeyEvent.VK_S]) { player.y += player.speed; movingNow = true; }
        if (keys[KeyEvent.VK_A]) { player.x -= player.speed; movingNow = true; player.faceRight = false; }
        if (keys[KeyEvent.VK_D]) { player.x += player.speed; movingNow = true; player.faceRight = true; }

        player.isMoving = movingNow;

        // Анимация
        if (player.isMoving) {
            player.animationTick++;
            if (player.animationTick > 10) {
                player.animationFrame = (player.animationFrame + 1) % 3;
                player.animationTick = 0;
            }
        } else {
            player.animationFrame = 0;
        }

        // ПРОВЕРКА ВХОДА В ПОРТАЛ (Коллизия)
        // Если игрок подошел к порталу (600, 400)
        if (player.x > 550 && player.x < 650 && player.y > 350 && player.y < 450) {
            startBattle();
        }
    }

    private void startBattle() {
        panel.isBattleMode = true;
        wasAttackUsed = false;
        player.hand.clear();
        for(int i=0; i<5; i++) player.hand.add(rnd.nextInt(9) + 1);
        player.currentEnemies.clear();
        for(int i=0; i<4; i++) player.currentEnemies.add(new Enemy(rnd.nextInt(9) + 1));

        // Смещаем игрока от портала, чтобы не зайти в него снова мгновенно
        player.x -= 100;
    }

    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { if(e.getKeyCode() < 256) keys[e.getKeyCode()] = true; }
            @Override
            public void keyReleased(KeyEvent e) { if(e.getKeyCode() < 256) keys[e.getKeyCode()] = false; }
        };
    }

    @Override
    public void run() {
        while (running) {
            update();
            panel.repaint();
            try { Thread.sleep(16); } catch (Exception ex) { break; }
        }
    }
}