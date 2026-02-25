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
    private Random rnd = new Random();
    private boolean running = true;
    private boolean wasAttackUsed = false;
    private boolean isEnemyTurning = false;
    private boolean isIntroActive = false;

    public GameController(Player player, GamePanel panel) {
        this.player = player;
        this.panel = panel;
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        double scaleX = (double) panel.getWidth() / 800;
        double scaleY = (double) panel.getHeight() / 600;
        panel.mouseX = (int) (e.getX() / scaleX);
        panel.mouseY = (int) (e.getY() / scaleY);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        handleMouseInput(e);
    }

    private void handleMouseInput(MouseEvent e) {
        double scaleX = (double) panel.getWidth() / 800;
        double scaleY = (double) panel.getHeight() / 600;
        int mx = (int) (e.getX() / scaleX);
        int my = (int) (e.getY() / scaleY);

        // --- МЕНЮ ---
        if (panel.state == GamePanel.GameState.MENU) {
            if (new Rectangle(300, 220, 200, 70).contains(mx, my)) {
                panel.currentTextIndex = 0;
                panel.startTyping();
            } else if (new Rectangle(300, 320, 200, 70).contains(mx, my)) {
                panel.state = GamePanel.GameState.SETTINGS;
            } else if (new Rectangle(300, 420, 200, 70).contains(mx, my)) {
                System.exit(0);
            }
            panel.repaint();
            return;
        }

        // --- БОЙ ---
        if (panel.state == GamePanel.GameState.BATTLE) {
            if (isEnemyTurning || isIntroActive) return;

            // Кнопка завершения хода
            if (panel.endTurnBtn.contains(mx, my)) {
                endPlayerTurn();
                return;
            }

            // Выбор карты в руке
            for (int i = 0; i < player.hand.size(); i++) {
                if (player.hand.get(i) != null && new Rectangle(70 + i * 110, 470, 95, 130).contains(mx, my)) {
                    player.selectedCardIndex = i;
                    panel.repaint();
                    return;
                }
            }

            // Если карта выбрана, проверяем цель
            if (player.selectedCardIndex != -1) {
                int cardVal = player.hand.get(player.selectedCardIndex);

                // --- МЕХАНИКА ХИЛА: Клик по игроку ---
                // Координаты игрока (px=80, py=180, size=130) из GamePanel
                if (new Rectangle(80, 180, 130, 130).contains(mx, my)) {
                    player.hp += cardVal;
                    if (player.hp > 100) player.hp = 100; // Ограничение здоровья

                    panel.spawnHealParticles(80, 180); // Запуск визуального эффекта

                    player.hand.set(player.selectedCardIndex, null);
                    player.selectedCardIndex = -1;
                    wasAttackUsed = true;
                    panel.repaint();
                    return;
                }

                // --- МЕХАНИКА АТАКИ: Клик по врагам ---
                for (int i = 0; i < player.currentEnemies.size(); i++) {
                    Enemy target = player.currentEnemies.get(i);
                    if (target == null || target.value < 0) continue;

                    int ex, ey;
                    switch(i) {
                        case 0 -> { ex = 420; ey = 70; }
                        case 1 -> { ex = 420; ey = 210; }
                        case 2 -> { ex = 420; ey = 350; }
                        case 3 -> { ex = 600; ey = 70; }
                        case 4 -> { ex = 600; ey = 210; }
                        case 5 -> { ex = 600; ey = 350; }
                        default -> { ex = -1000; ey = -1000; }
                    }

                    if (target.value > 0 && new Rectangle(ex, ey, 105, 105).contains(mx, my)) {
                        // Особенность 4: Разделение
                        if (target.value == 4 && cardVal != 0) {
                            target.value = 2;
                            if (player.currentEnemies.size() < 6) {
                                player.currentEnemies.add(new Enemy(2));
                                panel.enemyEntryOffsets[player.currentEnemies.size() - 1] = 0;
                            }
                        }
                        // Заморозка (карта 0)
                        else if (cardVal == 0) {
                            target.freezeTurns = 2;
                        }
                        // Обычный урон
                        else {
                            int dmg = (target.value == 9) ? Math.min(cardVal, 3) : cardVal;
                            target.value -= dmg;
                            panel.animateEnemyHurt(i);
                        }

                        player.hand.set(player.selectedCardIndex, null);
                        wasAttackUsed = true;
                        player.selectedCardIndex = -1;

                        if (target.value <= 0) {
                            target.value = 0;
                            final int deadIdx = i;
                            Timer deathTimer = new Timer(400, ev -> {
                                if (deadIdx < player.currentEnemies.size())
                                    player.currentEnemies.get(deadIdx).value = -1;
                                updateGlobalEnemyEffects();
                                checkBattleVictory();
                                panel.repaint();
                                ((Timer)ev.getSource()).stop();
                            });
                            deathTimer.start();
                        }
                        updateGlobalEnemyEffects();
                        panel.repaint();
                        return;
                    }
                }
            }
        }
    }

    private void updateGlobalEnemyEffects() {
        int count1 = 0; int count7 = 0;
        for (Enemy e : player.currentEnemies) {
            if (e.value > 0) {
                if (e.value == 1) count1++;
                if (e.value == 7) count7++;
            }
        }
        for (Enemy e : player.currentEnemies) {
            if (e.value == 1) e.isSuperOne = (count1 == 1);
        }
        if (count7 >= 2 && !player.sevenEffectUsedInThisRound) {
            player.invulnerableTurns = 3;
            player.sevenEffectUsedInThisRound = true;
        }
    }

    private void endPlayerTurn() {
        if (isEnemyTurning || isIntroActive) return;
        isEnemyTurning = true;

        // Урон от яда (враги с числом 2)
        int poisonDmg = 0;
        for (Enemy e : player.currentEnemies) if (e.value == 2) poisonDmg += 2;
        if (poisonDmg > 0 && player.invulnerableTurns <= 0) player.hp -= poisonDmg;

        player.hand.removeIf(val -> val == null);
        if (!wasAttackUsed) {
            for (int i = 0; i < 2; i++) {
                if (player.hand.size() < 6) player.hand.add(rnd.nextInt(10));
            }
        }

        new Thread(() -> {
            try {
                int count6 = 0;
                for (Enemy e : player.currentEnemies) if (e.value == 6) count6++;
                boolean cursed = (count6 >= 2);

                for (int i = 0; i < player.currentEnemies.size(); i++) {
                    Enemy en = player.currentEnemies.get(i);
                    if (en == null || en.value <= 0) continue;
                    if (en.freezeTurns > 0) { en.freezeTurns--; continue; }

                    panel.animateEnemyAttack(i);
                    int dmg = (en.value == 1 && en.isSuperOne) ? rnd.nextInt(10) + 1 : rnd.nextInt(en.value) + 1;
                    if (cursed) dmg *= 2;

                    if (player.invulnerableTurns <= 0) player.hp -= dmg;
                    Thread.sleep(600);
                    panel.repaint();
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            finally {
                if (player.invulnerableTurns > 0) player.invulnerableTurns--;
                wasAttackUsed = false;
                isEnemyTurning = false;
                panel.repaint();
            }
        }).start();
    }

    public void startBattle() {
        panel.state = GamePanel.GameState.BATTLE;
        isIntroActive = true;
        player.isRunningToBattle = true;
        player.hand.clear();
        player.currentEnemies.clear();
        player.sevenEffectUsedInThisRound = false;

        for (int i = 0; i < 5; i++) player.currentEnemies.add(new Enemy(rnd.nextInt(9) + 1));
        for (int i = 0; i < 5; i++) player.hand.add(rnd.nextInt(10));

        updateGlobalEnemyEffects();
        panel.animateEnemiesEntry();

        Timer t = new Timer(2000, e -> {
            player.isRunningToBattle = false;
            isIntroActive = false;
            panel.repaint();
            ((Timer)e.getSource()).stop();
        });
        t.start();
    }

    private void checkBattleVictory() {
        boolean anyAlive = false;
        for (Enemy en : player.currentEnemies) if (en.value > 0) anyAlive = true;
        if (!anyAlive) {
            Timer winDelay = new Timer(800, e -> { startBattle(); ((Timer)e.getSource()).stop(); });
            winDelay.start();
        }
    }

    public void update() {
        player.animationTick++;
        if (player.animationTick > 10) {
            player.animationFrame = (player.animationFrame + 1) % 3;
            player.animationTick = 0;
        }
    }

    @Override public void run() {
        while (running) {
            update();
            panel.repaint();
            try { Thread.sleep(16); } catch (Exception ex) { break; }
        }
    }

    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && panel.state == GamePanel.GameState.INTRO) {
                    handleIntroProgress();
                }
            }
        };
    }

    private void handleIntroProgress() {
        if (panel.isTyping) return;
        if (panel.currentTextIndex < 2) {
            panel.currentTextIndex++;
            panel.startTyping();
        } else {
            startBattle();
        }
    }
}