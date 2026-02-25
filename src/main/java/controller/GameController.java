package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.view.GamePanel;
import com.example.demo.view.SoundManager;
import javax.swing.Timer;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

public class GameController extends MouseAdapter implements Runnable {
    // Инициализация логгера
    private static final Logger logger = Logger.getLogger(GameController.class.getName());

    private Player player;
    private GamePanel panel;
    private Random rnd = new Random();
    private boolean running = true;

    // Состояние хода
    private boolean wasAttackUsed = false;
    private boolean isEnemyTurning = false;
    private boolean isIntroActive = false;

    // Параметры игры
    public enum Difficulty { EASY, MEDIUM, HARD }
    public Difficulty currentDiff = Difficulty.MEDIUM;
    public int currentRound = 1;
    public final int MAX_ROUNDS = 5;
    public boolean hasSavedGame = false;

    public GameController(Player player, GamePanel panel) {
        this.player = player;
        this.panel = panel;
        panel.controller = this;
        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);

        SoundManager.playMusic("battle_music");
        logger.info("Игра запущена. Музыка инициализирована.");
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMouseScale(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateMouseScale(e);
        handleVolumeSliders();
    }

    private void updateMouseScale(MouseEvent e) {
        double scaleX = (double) panel.getWidth() / 800;
        double scaleY = (double) panel.getHeight() / 600;
        panel.mouseX = (int) (e.getX() / scaleX);
        panel.mouseY = (int) (e.getY() / scaleY);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        handleMouseInput();
    }

    private void handleMouseInput() {
        int mx = panel.mouseX;
        int my = panel.mouseY;

        // --- ЭКРАНЫ ПОБЕДЫ И ПОРАЖЕНИЯ ---
        if (panel.state == GamePanel.GameState.WIN_GAME || panel.state == GamePanel.GameState.GAME_OVER) {
            if (new Rectangle(300, 400, 200, 60).contains(mx, my)) {
                SoundManager.playSound("click");
                logger.info("Возврат в меню из состояния: " + panel.state);
                hasSavedGame = false;
                panel.state = GamePanel.GameState.MENU;
                SoundManager.playMusic("battle_music");
            }
            return;
        }

        // --- МЕНЮ ---
        if (panel.state == GamePanel.GameState.MENU) {
            if (new Rectangle(300, 220, 200, 70).contains(mx, my)) {
                SoundManager.playSound("click");
                if (hasSavedGame) panel.state = GamePanel.GameState.CONTINUE_CHOICE;
                else panel.state = GamePanel.GameState.DIFFICULTY_SELECT;
            } else if (new Rectangle(300, 320, 200, 70).contains(mx, my)) {
                SoundManager.playSound("click");
                panel.state = GamePanel.GameState.SETTINGS;
            } else if (new Rectangle(300, 420, 200, 70).contains(mx, my)) {
                logger.info("Выход из игры через меню.");
                System.exit(0);
            }
            return;
        }

        // --- ВЫБОР ПРОДОЛЖЕНИЯ ---
        if (panel.state == GamePanel.GameState.CONTINUE_CHOICE) {
            if (new Rectangle(200, 280, 180, 60).contains(mx, my)) {
                SoundManager.playSound("click");
                logger.info("Игрок продолжил сохраненную игру.");
                panel.state = GamePanel.GameState.BATTLE;
            } else if (new Rectangle(420, 280, 180, 60).contains(mx, my)) {
                SoundManager.playSound("click");
                logger.info("Игрок сбросил сохранение и начал заново.");
                hasSavedGame = false;
                panel.state = GamePanel.GameState.DIFFICULTY_SELECT;
            }
            return;
        }

        // --- ВЫБОР СЛОЖНОСТИ ---
        if (panel.state == GamePanel.GameState.DIFFICULTY_SELECT) {
            if (new Rectangle(300, 200, 200, 60).contains(mx, my)) startNewGame(Difficulty.EASY);
            if (new Rectangle(300, 280, 200, 60).contains(mx, my)) startNewGame(Difficulty.MEDIUM);
            if (new Rectangle(300, 360, 200, 60).contains(mx, my)) startNewGame(Difficulty.HARD);
            return;
        }

        // --- НАСТРОЙКИ ---
        if (panel.state == GamePanel.GameState.SETTINGS) {
            handleVolumeSliders();
            if (new Rectangle(300, 420, 200, 60).contains(mx, my)) {
                SoundManager.playSound("click");
                panel.state = GamePanel.GameState.MENU;
            }
            return;
        }

        // --- ПАУЗА ---
        if (panel.state == GamePanel.GameState.PAUSE) {
            if (new Rectangle(300, 260, 200, 60).contains(mx, my)) panel.state = GamePanel.GameState.BATTLE;
            if (new Rectangle(300, 340, 200, 60).contains(mx, my)) {
                logger.info("Игра поставлена на паузу и сохранена.");
                hasSavedGame = true;
                panel.state = GamePanel.GameState.MENU;
            }
            return;
        }

        // --- БИТВА ---
        if (panel.state == GamePanel.GameState.BATTLE) {
            if (isEnemyTurning || isIntroActive) return;

            if (panel.endTurnBtn.contains(mx, my)) {
                SoundManager.playSound("click");
                endPlayerTurn();
                return;
            }

            for (int i = 0; i < player.hand.size(); i++) {
                if (player.hand.get(i) != null && new Rectangle(70 + i * 110, 470, 95, 130).contains(mx, my)) {
                    SoundManager.playSound("card_pick");
                    player.selectedCardIndex = i;
                    panel.repaint();
                    return;
                }
            }

            if (player.selectedCardIndex != -1) {
                int cardVal = player.hand.get(player.selectedCardIndex);

                if (new Rectangle(80, 180, 130, 130).contains(mx, my)) {
                    SoundManager.playSound("click");
                    int oldHp = player.hp;
                    player.hp += cardVal;
                    if (player.hp > 100) player.hp = 100;
                    logger.info("Лечение: " + oldHp + " -> " + player.hp + " (Карта: " + cardVal + ")");
                    panel.spawnHealParticles(80, 180);
                    consumeCard();
                    return;
                }

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
                        SoundManager.playSound("enemy_hit");
                        logger.info("Игрок атакует Врага #" + i + " картой " + cardVal);

                        if (target.value == 4 && cardVal != 0) {
                            target.value = 2;
                            if (player.currentEnemies.size() < 6) {
                                player.currentEnemies.add(new Enemy(2));
                                panel.enemyEntryOffsets[player.currentEnemies.size() - 1] = 0;
                                logger.info("Враг-4 разделился на 2 двойки!");
                            }
                        } else if (cardVal == 0) {
                            target.freezeTurns = 2;
                            logger.info("Враг #" + i + " заморожен на 2 хода.");
                        } else {
                            int dmg = (target.value == 9) ? Math.min(cardVal, 3) : cardVal;
                            target.value -= dmg;
                            logger.info("Нанесено " + dmg + " урона Врагу #" + i);
                            panel.animateEnemyHurt(i);
                        }
                        consumeCard();
                        if (target.value <= 0) {
                            target.value = 0;
                            handleEnemyDeath(i);
                        }
                        updateGlobalEnemyEffects();
                        panel.repaint();
                        return;
                    }
                }
            }
        }
    }

    private void handleVolumeSliders() {
        if (panel.state != GamePanel.GameState.SETTINGS) return;

        int mx = panel.mouseX;
        int my = panel.mouseY;

        // Разделяем проверку: каждый ползунок реагирует только на клик в своей области высоты

        // Логика для МУЗЫКИ
        if (panel.musicSlider.contains(mx, my)) {
            SoundManager.musicVolume = (mx - panel.musicSlider.x) * 100 / panel.musicSlider.width;
            SoundManager.updateMusicVolume();
            logger.finest("Громкость музыки изменена: " + SoundManager.musicVolume + "%");
        }

        // Логика для ЗВУКОВ
        if (panel.soundSlider.contains(mx, my)) {
            SoundManager.soundVolume = (mx - panel.soundSlider.x) * 100 / panel.soundSlider.width;
            logger.finest("Громкость звуков изменена: " + SoundManager.soundVolume + "%");
        }
    }

    private void startNewGame(Difficulty diff) {
        SoundManager.playSound("click");
        this.currentDiff = diff;
        this.currentRound = 1;
        player.hp = 100;
        logger.log(Level.INFO, "Начата новая игра. Сложность: {0}", diff);
        panel.currentTextIndex = 0;
        panel.startTyping();
    }

    private void consumeCard() {
        player.hand.set(player.selectedCardIndex, null);
        wasAttackUsed = true;
        player.selectedCardIndex = -1;
    }

    private void handleEnemyDeath(int index) {
        logger.info("Враг #" + index + " окончательно повержен.");
        final int deadIdx = index;
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

    public void startBattle() {
        panel.state = GamePanel.GameState.BATTLE;
        isIntroActive = true;
        player.isRunningToBattle = true;
        SoundManager.playSound("footsteps");
        logger.info("Начало Раунда #" + currentRound);

        player.hand.clear();
        player.currentEnemies.clear();
        player.sevenEffectUsedInThisRound = false;

        int maxVal = switch(currentDiff) {
            case EASY -> 4;
            case MEDIUM -> 7;
            case HARD -> 9;
        };

        for (int i = 0; i < 5; i++) player.currentEnemies.add(new Enemy(rnd.nextInt(maxVal) + 1));
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

    private void endPlayerTurn() {
        if (isEnemyTurning || isIntroActive) return;
        isEnemyTurning = true;
        logger.info("--- Конец хода игрока. Ход монстров ---");

        int poisonDmg = 0;
        for (Enemy e : player.currentEnemies) if (e.value == 2) poisonDmg += 2;
        if (poisonDmg > 0 && player.invulnerableTurns <= 0) {
            player.hp -= poisonDmg;
            logger.warning("Игрок получил " + poisonDmg + " урона от яда (двойки)!");
        }

        player.hand.removeIf(val -> val == null);
        if (!wasAttackUsed) {
            logger.info("Атака не была использована. Добор +2 карт.");
            for (int i = 0; i < 2; i++) if (player.hand.size() < 6) player.hand.add(rnd.nextInt(10));
        }

        new Thread(() -> {
            try {
                int count6 = 0;
                for (Enemy e : player.currentEnemies) if (e.value == 6) count6++;
                boolean cursed = (count6 >= 2);
                if (cursed) logger.warning("ПРОКЛЯТИЕ! Урон врагов удвоен (на поле 2+ шестерки).");

                for (int i = 0; i < player.currentEnemies.size(); i++) {
                    Enemy en = player.currentEnemies.get(i);
                    if (en == null || en.value <= 0) continue;
                    if (en.freezeTurns > 0) {
                        en.freezeTurns--;
                        logger.info("Враг #" + i + " пропустил ход (заморожен).");
                        continue;
                    }

                    panel.animateEnemyAttack(i);
                    SoundManager.playSound("player_hurt");

                    int dmg = (en.value == 1 && en.isSuperOne) ? rnd.nextInt(10) + 1 : rnd.nextInt(en.value) + 1;
                    if (cursed) dmg *= 2;

                    if (player.invulnerableTurns <= 0) {
                        player.hp -= dmg;
                        logger.info("Враг #" + i + " нанес " + dmg + " урона. HP Игрока: " + player.hp);
                    } else {
                        logger.info("Игрок неуязвим! Урон от Врага #" + i + " заблокирован.");
                    }

                    if (player.hp <= 0) break;
                    Thread.sleep(600);
                    panel.repaint();
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Ошибка в потоке битвы", ex);
            }
            finally {
                if (player.invulnerableTurns > 0) {
                    player.invulnerableTurns--;
                    logger.info("Оставшиеся ходы неуязвимости: " + player.invulnerableTurns);
                }
                wasAttackUsed = false;
                isEnemyTurning = false;
                panel.repaint();
            }
        }).start();
    }

    private void updateGlobalEnemyEffects() {
        int c1 = 0; int c7 = 0;
        for (Enemy e : player.currentEnemies) {
            if (e.value > 0) {
                if (e.value == 1) c1++;
                if (e.value == 7) c7++;
            }
        }
        for (Enemy e : player.currentEnemies) if (e.value == 1) e.isSuperOne = (c1 == 1);
        if (c7 >= 2 && !player.sevenEffectUsedInThisRound) {
            player.invulnerableTurns = 3;
            player.sevenEffectUsedInThisRound = true;
            logger.info("СВЯТАЯ ЗАЩИТА! На поле 2 семерки. Игрок неуязвим на 3 хода.");
        }
    }

    private void checkBattleVictory() {
        boolean anyAlive = false;
        for (Enemy en : player.currentEnemies) if (en.value > 0) anyAlive = true;

        if (!anyAlive) {
            if (currentRound < MAX_ROUNDS) {
                logger.info("Раунд #" + currentRound + " пройден!");
                currentRound++;
                Timer nextRound = new Timer(1000, e -> { startBattle(); ((Timer)e.getSource()).stop(); });
                nextRound.start();
            } else {
                logger.info("ФИНАЛЬНАЯ ПОБЕДА! Игра пройдена.");
                panel.state = GamePanel.GameState.WIN_GAME;
                hasSavedGame = false;
            }
        }
    }

    public KeyAdapter getKeyListener() {
        return new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (panel.state == GamePanel.GameState.BATTLE) panel.state = GamePanel.GameState.PAUSE;
                    else if (panel.state == GamePanel.GameState.PAUSE) panel.state = GamePanel.GameState.BATTLE;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && panel.state == GamePanel.GameState.INTRO) {
                    SoundManager.playSound("click");
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
            SoundManager.playSound("start_battle");
            startBattle();
        }
    }

    public void update() {
        player.animationTick++;
        if (player.animationTick > 10) {
            player.animationFrame = (player.animationFrame + 1) % 3;
            player.animationTick = 0;
        }

        // ПРОВЕРКА ПОРАЖЕНИЯ
        if (player.hp <= 0 && panel.state == GamePanel.GameState.BATTLE) {
            player.hp = 0;
            logger.warning("Игрок погиб. Состояние: GAME_OVER");
            panel.state = GamePanel.GameState.GAME_OVER;
            SoundManager.stopMusic();
            SoundManager.playSound("game_over");
        }
    }

    @Override public void run() {
        while (running) {
            update();
            panel.repaint();
            try { Thread.sleep(16); } catch (Exception ex) {}
        }
    }
}