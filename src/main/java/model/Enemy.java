package com.example.demo.model;

public class Enemy {
    public int value;
    public boolean isAlive = true; // Новое поле
    public int freezeTurns = 0;
    public boolean isSuperOne = false; // Для особенности №1

    public Enemy(int value) {
        this.value = value;
    }
}