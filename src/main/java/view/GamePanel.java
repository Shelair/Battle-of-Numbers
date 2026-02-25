package com.example.demo.view;

import com.example.demo.model.Player;
import com.example.demo.model.Enemy;
import com.example.demo.controller.GameController;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel {
    public final Player player;
    public GameController controller;
    private Random rnd = new Random();

    public int mouseX, mouseY;

    public enum GameState {
        MENU, SETTINGS, INTRO, BATTLE,
        PAUSE, DIFFICULTY_SELECT, CONTINUE_CHOICE, WIN_GAME, GAME_OVER // Добавлено GAME_OVER
    }
    public GameState state = GameState.MENU;

    // Ресурсы
    private BufferedImage[] walkRight = new BufferedImage[3];
    private Map<Integer, BufferedImage> cardSprites = new HashMap<>();
    private Map<Integer, BufferedImage> enemySprites = new HashMap<>();
    private BufferedImage talkHead, frozenSprite, healParticle;
    private BufferedImage enemy1Super, iconPoison, iconCurse, iconBless;

    // Тексты интро
    private String[] introTexts = {
            "Мое королевство погрязло во зле, вечные страдания измотали меня...",
            "Но сегодня я очищу этот мир от мусора.",
            "Смерть врагам... Слава мне..."
    };
    public int currentTextIndex = 0;
    private String visibleText = "";
    private int charIndex = 0;
    private Timer typewriterTimer;
    public boolean isTyping = false;

    // Анимации
    public int attackingEnemyIndex = -1;
    public int enemyAnimOffsetY = 0, enemyAnimOffsetX = 0;
    public int damagedEnemyIndex = -1, enemyHurtOffsetY = 0;
    public float[] enemyEntryOffsets = new float[6];
    private List<Point> particles = new ArrayList<>();
    private boolean isHealingFlash = false;

    // UI Элементы
    public Rectangle endTurnBtn = new Rectangle(610, 20, 170, 40);
    public Rectangle musicSlider = new Rectangle(300, 220, 300, 20);
    public Rectangle soundSlider = new Rectangle(300, 320, 300, 20);

    public GamePanel(Player player) {
        this.player = player;
        this.setFocusable(true);
        this.setBackground(Color.BLACK);
        loadSprites();
        for(int i=0; i<6; i++) enemyEntryOffsets[i] = 500f;
    }

    private void loadSprites() {
        String path = "/sprites/";
        talkHead = ResourceLoader.loadImage(path + "talk_head.png");
        frozenSprite = ResourceLoader.loadImage(path + "enemy_frozen.png");
        healParticle = ResourceLoader.loadImage(path + "heal_particle.png");
        enemy1Super = ResourceLoader.loadImage(path + "enemy_1_super.png");
        iconPoison = ResourceLoader.loadImage(path + "icon_poison.png");
        iconCurse = ResourceLoader.loadImage(path + "icon_curse.png");
        iconBless = ResourceLoader.loadImage(path + "icon_bless.png");

        for (int i = 0; i < 3; i++) walkRight[i] = ResourceLoader.loadImage(path + "player_walk_right_" + (i + 1) + ".png");
        for (int i = 0; i <= 9; i++) {
            cardSprites.put(i, ResourceLoader.loadImage(path + "card_" + i + ".png"));
            enemySprites.put(i, ResourceLoader.loadImage(path + "enemy_" + i + ".png"));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scaleX = (double) getWidth() / 800;
        double scaleY = (double) getHeight() / 600;
        g2.scale(scaleX, scaleY);

        switch (state) {
            case MENU -> drawMenu(g2);
            case SETTINGS -> drawSettings(g2);
            case INTRO -> drawIntro(g2);
            case BATTLE -> { drawBattle(g2); drawRoundIndicator(g2); }
            case PAUSE -> { drawBattle(g2); drawPauseOverlay(g2); }
            case DIFFICULTY_SELECT -> drawDifficultySelect(g2);
            case CONTINUE_CHOICE -> drawContinueChoice(g2);
            case WIN_GAME -> drawEndScreen(g2, "ПОБЕДА!", Color.YELLOW, "Мир очищен от мусора...");
            case GAME_OVER -> drawEndScreen(g2, "ВЫ ПРОИГРАЛИ", Color.RED, "Тьма поглотила ваше королевство...");
        }
    }

    // --- УНИВЕРСАЛЬНЫЙ ЭКРАН КОНЦА ИГРЫ ---
    private void drawEndScreen(Graphics2D g2, String title, Color titleColor, String subText) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, 800, 600);

        g2.setFont(new Font("Monospaced", Font.BOLD, 70));
        g2.setColor(titleColor);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (800 - fm.stringWidth(title)) / 2, 250);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 22));
        g2.setColor(Color.WHITE);
        fm = g2.getFontMetrics();
        g2.drawString(subText, (800 - fm.stringWidth(subText)) / 2, 310);

        drawCustomButton(g2, 300, 400, 200, 60, "В МЕНЮ");
    }

    private void drawRoundIndicator(Graphics2D g2) {
        if (controller == null) return;
        g2.setColor(new Color(255, 255, 255, 180));
        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        String roundStr = "КРУГ: " + controller.currentRound + " / " + controller.MAX_ROUNDS;
        g2.drawString(roundStr, 310, 35);

        for (int i = 1; i <= controller.MAX_ROUNDS; i++) {
            if (i < controller.currentRound) g2.setColor(Color.GREEN);
            else if (i == controller.currentRound) g2.setColor(Color.YELLOW);
            else g2.setColor(Color.DARK_GRAY);
            g2.fillOval(330 + (i * 25), 45, 12, 12);
        }
    }

    private void drawPauseOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, 800, 600);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 50));
        g2.drawString("ПАУЗА", 320, 200);
        drawCustomButton(g2, 300, 260, 200, 60, "ПРОДОЛЖИТЬ");
        drawCustomButton(g2, 300, 340, 200, 60, "В МЕНЮ");
    }

    private void drawDifficultySelect(Graphics2D g2) {
        drawChalkBoard(g2, 150, 100, 500, 400);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        g2.drawString("ВЫБЕРИ СЛОЖНОСТЬ", 260, 160);
        drawCustomButton(g2, 300, 200, 200, 60, "ЛЕГКАЯ");
        drawCustomButton(g2, 300, 280, 200, 60, "СРЕДНЯЯ");
        drawCustomButton(g2, 300, 360, 200, 60, "СЛОЖНАЯ");
    }

    private void drawContinueChoice(Graphics2D g2) {
        drawChalkBoard(g2, 150, 150, 500, 300);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 24));
        g2.drawString("НАЙДЕНО СОХРАНЕНИЕ", 270, 220);
        drawCustomButton(g2, 200, 280, 180, 60, "ПРОДОЛЖИТЬ");
        drawCustomButton(g2, 420, 280, 180, 60, "НОВАЯ ИГРА");
    }

    // --- БОЕВАЯ ЛОГИКА ---
    private void drawBattle(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString("HP: " + player.hp, 50, 50);

        drawStatusIcons(g2);
        for(Point p : particles) if(healParticle != null) g2.drawImage(healParticle, p.x, p.y, 25, 25, null);

        int px = 80, py = 180, pSize = 130;
        BufferedImage ps = player.isRunningToBattle ? walkRight[player.animationFrame] : walkRight[0];
        if (ps != null) {
            if (isHealingFlash) drawFlashImage(g2, ps, px, py, pSize, pSize, Color.GREEN);
            else g2.drawImage(ps, px, py, pSize, pSize, null);
        }

        for (int i = 0; i < player.currentEnemies.size(); i++) {
            Enemy enemy = player.currentEnemies.get(i);
            if (enemy == null || enemy.value < 0) continue;
            int ex, ey;
            switch(i) {
                case 0 -> { ex = 420; ey = 70; }
                case 1 -> { ex = 420; ey = 210; }
                case 2 -> { ex = 420; ey = 350; }
                case 3 -> { ex = 600; ey = 70; }
                case 4 -> { ex = 600; ey = 210; }
                case 5 -> { ex = 600; ey = 350; }
                default -> { ex = 900; ey = 0; }
            }
            ex += (int)enemyEntryOffsets[i];
            if (i == attackingEnemyIndex) { ex += enemyAnimOffsetX; ey += enemyAnimOffsetY; }
            if (i == damagedEnemyIndex) { ey += enemyHurtOffsetY; }

            BufferedImage es = (enemy.freezeTurns > 0) ? frozenSprite :
                    (enemy.value == 1 && enemy.isSuperOne) ? enemy1Super : enemySprites.get(enemy.value);

            if (es != null) {
                if (i == damagedEnemyIndex) drawFlashImage(g2, es, ex, ey, 105, 105, Color.RED);
                else g2.drawImage(es, ex, ey, 105, 105, null);
            }
            if (enemy.value > 0 && enemyEntryOffsets[i] < 50) {
                g2.setColor(enemy.freezeTurns > 0 ? Color.CYAN : (enemy.isSuperOne ? Color.RED : Color.WHITE));
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                g2.drawString("HP: " + enemy.value + (enemy.freezeTurns > 0 ? " [ICE]" : ""), ex + 5, ey + 120);
            }
        }

        for (int i = 0; i < player.hand.size(); i++) {
            Integer val = player.hand.get(i);
            if (val == null) continue;
            int cx = 70 + i * 110, cy = 470;
            if (i == player.selectedCardIndex) cy -= 30;
            BufferedImage cs = cardSprites.get(val);
            if (cs != null) g2.drawImage(cs, cx, cy, 95, 130, null);
        }

        g2.setColor(new Color(60, 60, 60, 200));
        g2.fillRoundRect(endTurnBtn.x, endTurnBtn.y, endTurnBtn.width, endTurnBtn.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("ЗАКОНЧИТЬ ХОД", endTurnBtn.x + 25, endTurnBtn.y + 25);
    }

    private void drawIntro(Graphics2D g2) {
        drawChalkBoard(g2, 50, 350, 700, 200);
        if (talkHead != null) g2.drawImage(talkHead, 300, 60, 200, 200, null);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        int tx = 80, ty = 400;
        String[] words = visibleText.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() > 50) {
                g2.drawString(line.toString(), tx, ty);
                ty += 30; line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        g2.drawString(line.toString(), tx, ty);
        if (!isTyping) {
            g2.setFont(new Font("Monospaced", Font.ITALIC, 14));
            g2.drawString("[ Нажми ENTER ]", 580, 530);
        }
    }

    private void drawMenu(Graphics2D g2) {
        drawChalkBoard(g2, 50, 40, 700, 520);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Monospaced", Font.BOLD, 54));
        g2.drawString("Battle of Numbers", 120, 130);
        drawCustomButton(g2, 300, 220, 200, 70, "ИГРАТЬ");
        drawCustomButton(g2, 300, 320, 200, 70, "НАСТРОЙКИ");
        drawCustomButton(g2, 300, 420, 200, 70, "ВЫЙТИ");
    }

    private void drawSettings(Graphics2D g2) {
        drawChalkBoard(g2, 100, 80, 600, 440);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 40));
        g2.drawString("НАСТРОЙКИ", 285, 150);
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2.drawString("МУЗЫКА", 150, 235);
        drawSlider(g2, musicSlider, SoundManager.musicVolume);
        g2.drawString("ЗВУКИ", 150, 335);
        drawSlider(g2, soundSlider, SoundManager.soundVolume);
        drawCustomButton(g2, 300, 420, 200, 60, "НАЗАД");
    }

    private void drawSlider(Graphics2D g2, Rectangle rect, int volume) {
        g2.setColor(new Color(255, 255, 255, 100));
        g2.fillRoundRect(rect.x, rect.y + 8, rect.width, 4, 2, 2);
        g2.setColor(Color.WHITE);
        int fillWidth = (int) (rect.width * (volume / 100.0));
        g2.fillRoundRect(rect.x, rect.y + 8, fillWidth, 4, 2, 2);
        int knobX = rect.x + fillWidth - 10;
        g2.fillOval(knobX, rect.y, 20, 20);
        g2.drawString(volume + "%", rect.x + rect.width + 15, rect.y + 17);
    }

    private void drawStatusIcons(Graphics2D g2) {
        int ix = 20, iy = 70;
        int pCount = 0;
        for(Enemy e : player.currentEnemies) if(e.value == 2) pCount++;
        if(pCount > 0) {
            if(iconPoison != null) g2.drawImage(iconPoison, ix, iy, 35, 35, null);
            ix += 45;
        }
        int cCount = 0;
        for(Enemy e : player.currentEnemies) if(e.value == 6) cCount++;
        if(cCount >= 2) {
            if(iconCurse != null) g2.drawImage(iconCurse, ix, iy, 35, 35, null);
            ix += 45;
        }
        if(player.invulnerableTurns > 0) {
            if(iconBless != null) g2.drawImage(iconBless, ix, iy, 35, 35, null);
        }
    }

    private void drawChalkBoard(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(80, 50, 20)); g2.fillRoundRect(x - 10, y - 10, w + 20, h + 20, 15, 15);
        g2.setColor(new Color(20, 60, 40)); g2.fillRect(x, y, w, h);
    }

    private void drawCustomButton(Graphics2D g2, int x, int y, int w, int h, String text) {
        boolean hover = new Rectangle(x, y, w, h).contains(mouseX, mouseY);
        g2.setColor(hover ? new Color(255, 255, 255, 60) : new Color(255, 255, 255, 30));
        g2.fillRoundRect(x, y, w, h, 15, 15);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent()) / 2 - 5);
    }

    private void drawFlashImage(Graphics2D g2, BufferedImage img, int x, int y, int w, int h, Color color) {
        Composite oldComp = g2.getComposite();
        g2.drawImage(img, x, y, w, h, null);
        g2.setComposite(AlphaComposite.SrcAtop.derive(0.4f));
        g2.setColor(color); g2.fillRect(x, y, w, h);
        g2.setComposite(oldComp);
    }

    // --- АНИМАЦИИ ---
    public void spawnHealParticles(int x, int y) {
        isHealingFlash = true;
        particles.clear();
        for(int i=0; i<12; i++) particles.add(new Point(x + rnd.nextInt(100), y + rnd.nextInt(100)));
        Timer pTimer = new Timer(30, null);
        pTimer.addActionListener(e -> {
            for(Point p : particles) p.y -= 5;
            if(!particles.isEmpty() && particles.get(0).y < y - 80) {
                particles.clear();
                isHealingFlash = false;
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });
        pTimer.start();
    }

    public void startTyping() {
        state = GameState.INTRO;
        visibleText = ""; charIndex = 0; isTyping = true;
        if (typewriterTimer != null) typewriterTimer.stop();
        typewriterTimer = new Timer(40, e -> {
            if (charIndex < introTexts[currentTextIndex].length()) {
                visibleText += introTexts[currentTextIndex].charAt(charIndex++);
                repaint();
            } else { isTyping = false; ((Timer)e.getSource()).stop(); repaint(); }
        });
        typewriterTimer.start();
    }

    public void animateEnemiesEntry() {
        for(int i=0; i<6; i++) enemyEntryOffsets[i] = 500f;
        Timer entryTimer = new Timer(20, null);
        entryTimer.addActionListener(e -> {
            boolean allFinished = true;
            for(int i=0; i<player.currentEnemies.size(); i++) {
                if(enemyEntryOffsets[i] > 0) {
                    enemyEntryOffsets[i] -= 20;
                    allFinished = false;
                } else enemyEntryOffsets[i] = 0;
            }
            repaint();
            if(allFinished) ((Timer)e.getSource()).stop();
        });
        entryTimer.start();
    }

    public void animateEnemyAttack(int index) {
        this.attackingEnemyIndex = index;
        Timer timer = new Timer(20, null); final int[] frame = {0};
        timer.addActionListener(e -> {
            frame[0]++;
            if (frame[0] <= 10) enemyAnimOffsetX -= 10;
            else if (frame[0] <= 20) enemyAnimOffsetX += 10;
            else { attackingEnemyIndex = -1; enemyAnimOffsetX = 0; ((Timer)e.getSource()).stop(); }
            repaint();
        });
        timer.start();
    }

    public void animateEnemyHurt(int index) {
        this.damagedEnemyIndex = index;
        Timer timer = new Timer(30, null); final int[] frame = {0};
        timer.addActionListener(e -> {
            if (++frame[0] <= 5) enemyHurtOffsetY = -15;
            else if (frame[0] <= 10) enemyHurtOffsetY = 0;
            else { damagedEnemyIndex = -1; ((Timer)e.getSource()).stop(); }
            repaint();
        });
        timer.start();
    }
}