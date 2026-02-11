package com.example.demo.model;
import java.util.*;

public class Player {
    public int x = 100, y = 100, size = 50, speed = 5, hp = 50;
    public List<Integer> hand = new ArrayList<>();
    public List<Enemy> currentEnemies = new ArrayList<>();
    public int selectedCardIndex = -1; // -1 означает, что карта не выбрана
}