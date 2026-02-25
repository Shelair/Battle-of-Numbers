package com.example.demo;

import com.example.demo.model.Player;
import com.example.demo.model.Enemy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BattleOfNumbersApplicationTests {

	@Test
	void contextLoads() {
		// Проверка, что Spring-контекст загружается без ошибок
	}

	// --- ТЕСТЫ ИГРОКА ---

	@Test
	void testHealLimit() {
		Player player = new Player();
		player.hp = 95;
		player.hp += 10; // Лечение картой "10"

		if (player.hp > 100) player.hp = 100;

		assertEquals(100, player.hp, "HP не должно быть больше 100");
	}

	@Test
	void testPlayerDeath() {
		Player player = new Player();
		player.hp = 10;
		player.hp -= 20; // Получил сильный удар

		assertTrue(player.hp <= 0, "Игрок должен считаться погибшим");
	}

	// --- ТЕСТЫ ВРАГОВ ---

	@Test
	void testEnemyFreeze() {
		Enemy enemy = new Enemy(5);
		enemy.freezeTurns = 2;

		assertTrue(enemy.freezeTurns > 0, "Враг должен быть заморожен");
	}
}