package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.view.GamePanel;

import javax.swing.Timer;
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
    private boolean isEnemyTurning = false;

    public GameController(Player player, GamePanel panel) {
        this.player = player;
        this.panel = panel;
        panel.addMouseListener(this);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!panel.isBattleMode || isEnemyTurning) return;

        int mx = e.getX();
        int my = e.getY();

        // 1. Кнопка "Закончить ход"
        if (panel.endTurnBtn.contains(mx, my)) {
            endPlayerTurn();
            return;
        }

        // 2. Выбор карты (Проверяем на null, чтобы не тыкать в пустое место)
        for (int i = 0; i < player.hand.size(); i++) {
            if (player.hand.get(i) != null && new Rectangle(80 + i * 110, 450, 100, 145).contains(mx, my)) {
                player.selectedCardIndex = i;
                panel.repaint();
                return;
            }
        }

        // 3. Удар по врагу
        if (player.selectedCardIndex != -1) {
            for (int i = 0; i < player.currentEnemies.size(); i++) {
                Enemy target = player.currentEnemies.get(i);

                // Бьем только если враг еще жив (HP > 0)
                if (target.value > 0 && new Rectangle(80 + i * 180, 130, 120, 120).contains(mx, my)) {

                    panel.animateEnemyHurt(i);

                    // Вместо удаления — ставим null. Карта исчезнет, но зазор останется
                    int dmg = player.hand.get(player.selectedCardIndex);
                    player.hand.set(player.selectedCardIndex, null);

                    target.value -= dmg;
                    wasAttackUsed = true;
                    player.selectedCardIndex = -1;

                    // Проверка на победу (если все враги повержены)
                    boolean allDead = true;
                    for (Enemy en : player.currentEnemies) {
                        if (en.value > 0) {
                            allDead = false;
                            break;
                        }
                    }

                    if (allDead) {
                        Timer winDelay = new Timer(600, ev -> {
                            panel.isBattleMode = false;
                            ((Timer)ev.getSource()).stop();
                            panel.repaint();
                        });
                        winDelay.start();
                    }

                    panel.repaint();
                    return;
                }
            }
        }
    }

    private void endPlayerTurn() {
        if (isEnemyTurning) return;
        isEnemyTurning = true;

        // ТОЛЬКО СЕЙЧАС убираем пустые места (null) и сдвигаем карты влево
        player.hand.removeIf(val -> val == null);

        // Добор карт (лимит 6)
        if (!wasAttackUsed) {
            for (int i = 0; i < 2; i++) {
                if (player.hand.size() < 6) {
                    player.hand.add(rnd.nextInt(9) + 1);
                }
            }
        }

        new Thread(() -> {
            try {
                for (int i = 0; i < player.currentEnemies.size(); i++) {
                    Enemy en = player.currentEnemies.get(i);

                    // Мертвые враги не атакуют
                    if (en.value <= 0) continue;

                    panel.animateEnemyAttack(i);
                    player.hp -= (rnd.nextInt(en.value) + 1);

                    Thread.sleep(700);
                    panel.repaint();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                wasAttackUsed = false;
                player.selectedCardIndex = -1;
                isEnemyTurning = false;

                if (player.hp <= 0) {
                    System.out.println("Игра окончена!");
                }
                panel.repaint();
            }
        }).start();
    }

    public void update() {
        if (panel.isBattleMode) return;

        boolean movingNow = false;
        if (keys[KeyEvent.VK_W]) { player.y -= player.speed; movingNow = true; }
        if (keys[KeyEvent.VK_S]) { player.y += player.speed; movingNow = true; }
        if (keys[KeyEvent.VK_A]) { player.x -= player.speed; movingNow = true; player.faceRight = false; }
        if (keys[KeyEvent.VK_D]) { player.x += player.speed; movingNow = true; player.faceRight = true; }

        player.isMoving = movingNow;

        if (player.isMoving) {
            player.animationTick++;
            if (player.animationTick > 10) {
                player.animationFrame = (player.animationFrame + 1) % 3;
                player.animationTick = 0;
            }
        } else {
            player.animationFrame = 0;
        }

        if (player.x > 550 && player.x < 650 && player.y > 350 && player.y < 450) {
            startBattle();
        }
    }

    private void startBattle() {
        panel.isBattleMode = true;
        wasAttackUsed = false;
        isEnemyTurning = false;

        player.hand.clear();
        for(int i = 0; i < 5; i++) {
            player.hand.add(rnd.nextInt(9) + 1);
        }

        player.currentEnemies.clear();
        for(int i = 0; i < 4; i++) {
            player.currentEnemies.add(new Enemy(rnd.nextInt(9) + 1));
        }

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