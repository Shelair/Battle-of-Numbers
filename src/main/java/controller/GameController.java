package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.view.GamePanel;
import java.awt.Rectangle; // ВОТ ЭТОГО НЕ ХВАТАЛО
import java.awt.event.*;
import java.util.Random;

public class GameController extends MouseAdapter implements Runnable {
    private Player player;
    private GamePanel panel;
    private boolean[] keys = new boolean[256];
    private Random rnd = new Random();
    private boolean running = true;

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

        if (panel.endTurnBtn.contains(mx, my)) {
            endPlayerTurn();
            return;
        }

        for (int i = 0; i < player.hand.size(); i++) {
            Rectangle cardRect = new Rectangle(100 + i * 80, 400, 60, 90);
            if (cardRect.contains(mx, my)) {
                player.selectedCardIndex = i;
                return;
            }
        }

        if (player.selectedCardIndex != -1) {
            for (int i = 0; i < player.currentEnemies.size(); i++) {
                Rectangle enemyRect = new Rectangle(100 + i * 150, 150, 80, 80);
                if (enemyRect.contains(mx, my)) {
                    int dmg = player.hand.remove(player.selectedCardIndex);
                    player.currentEnemies.get(i).value -= dmg;
                    if (player.currentEnemies.get(i).value <= 0) player.currentEnemies.remove(i);
                    player.selectedCardIndex = -1;
                    if (player.currentEnemies.isEmpty()) panel.isBattleMode = false;
                    return;
                }
            }
        }
    }

    private void endPlayerTurn() {
        for(int i=0; i<2; i++) player.hand.add(rnd.nextInt(9) + 1);
        for (Enemy en : player.currentEnemies) {
            player.hp -= (rnd.nextInt(en.value) + 1);
        }
        player.selectedCardIndex = -1;
    }

    public void update() {
        if (!panel.isBattleMode) {
            if (keys[KeyEvent.VK_W]) player.y -= player.speed;
            if (keys[KeyEvent.VK_S]) player.y += player.speed;
            if (keys[KeyEvent.VK_A]) player.x -= player.speed;
            if (keys[KeyEvent.VK_D]) player.x += player.speed;

            // Коллизия с порталом
            if (player.x > 550 && player.x < 650 && player.y > 350 && player.y < 450) {
                startBattle();
            }
        }
    }

    private void startBattle() {
        panel.isBattleMode = true;
        player.hand.clear();
        for(int i=0; i<5; i++) player.hand.add(rnd.nextInt(9) + 1);
        player.currentEnemies.clear();
        for(int i=0; i<4; i++) player.currentEnemies.add(new Enemy(rnd.nextInt(9) + 1));
    }

    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { if(e.getKeyCode()<256) keys[e.getKeyCode()] = true; }
            @Override
            public void keyReleased(KeyEvent e) { if(e.getKeyCode()<256) keys[e.getKeyCode()] = false; }
        };
    }

    @Override
    public void run() {
        while (running) {
            update();
            panel.repaint();
            try { Thread.sleep(16); } catch (Exception ex) {}
        }
    }
}