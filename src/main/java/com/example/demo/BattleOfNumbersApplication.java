package com.example.demo;

import com.example.demo.controller.GameController;
import com.example.demo.model.Player;
import com.example.demo.view.GamePanel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import javax.swing.*;
import java.awt.*;

@SpringBootApplication
public class BattleOfNumbersApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder(BattleOfNumbersApplication.class)
				.headless(false)
				.run(args);

		EventQueue.invokeLater(() -> {
			JFrame frame = new JFrame("Numbers Battle");
			Player player = new Player();
			GamePanel panel = new GamePanel(player);
			GameController controller = new GameController(player, panel);

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(panel);
			frame.setSize(800, 600);

			// Регистрируем управление везде, где можно
			var listener = controller.getKeyListener();
			frame.addKeyListener(listener);
			panel.addKeyListener(listener);

			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

			// ФИНАЛЬНЫЙ ПИНОК ДЛЯ ФОКУСА
			panel.requestFocusInWindow();

			new Thread(controller).start();
		});
	}
}