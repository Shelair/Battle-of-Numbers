package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель игрока.
 * Здесь хранятся все данные: от координат до карт в руке.
 */
public class Player {
    // Координаты и физические размеры
    public int x = 100, y = 100, size = 64, speed = 5;
    public boolean isRunningToBattle = false;

    public int invulnerableTurns = 0; // Ходы неуязвимости
    public boolean sevenEffectUsedInThisRound = false; // Флаг для семерок

    // Состояние анимации (нужно для GamePanel)
    public boolean isMoving = false;
    public boolean faceRight = true;
    public int animationTick = 0; // Счетчик для скорости смены кадров
    public int animationFrame = 0; // Текущий кадр (0, 1 или 2)

    // ДАННЫЕ ДЛЯ БОЯ (Именно их не хватало на твоих скриншотах!)
    public int hp = 50; // Твое здоровье
    public List<Integer> hand = new ArrayList<>(); // Карты в руке
    public List<Enemy> currentEnemies = new ArrayList<>(); // Список врагов в битве
    public int selectedCardIndex = -1; // Индекс выбранной карты (-1 = ничего не выбрано)
}