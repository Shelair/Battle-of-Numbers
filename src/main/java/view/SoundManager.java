package com.example.demo.view;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundManager {
    private static Clip backgroundMusic;

    // Глобальные переменные громкости (0-100)
    public static int musicVolume = 70;
    public static int soundVolume = 80;

    /**
     * Проигрывание коротких звуковых эффектов (атака, клик, хил)
     */
    public static void playSound(String name) {
        new Thread(() -> {
            try {
                InputStream is = SoundManager.class.getResourceAsStream("/sounds/" + name + ".wav");
                if (is == null) return;

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(bis);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);

                // Применяем текущую громкость эффектов
                setClipVolume(clip, soundVolume);

                clip.start();

                // Автоматическое освобождение ресурсов после проигрывания
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception e) {
                System.err.println("SoundManager: Ошибка проигрывания звука -> " + name);
            }
        }).start();
    }

    /**
     * Проигрывание фоновой музыки (циклично)
     */
    public static void playMusic(String name) {
        try {
            if (backgroundMusic != null && backgroundMusic.isRunning()) {
                backgroundMusic.stop();
                backgroundMusic.close();
            }

            InputStream is = SoundManager.class.getResourceAsStream("/sounds/" + name + ".wav");
            if (is == null) return;

            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(bis);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioIn);

            updateMusicVolume(); // Установка громкости перед стартом

            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        } catch (Exception e) {
            System.err.println("SoundManager: Ошибка проигрывания музыки -> " + name);
        }
    }

    /**
     * Обновляет громкость фоновой музыки в реальном времени
     */
    public static void updateMusicVolume() {
        if (backgroundMusic != null && backgroundMusic.isOpen()) {
            setClipVolume(backgroundMusic, musicVolume);
        }
    }

    /**
     * Внутренний метод для конвертации 0-100 в децибелы (логарифмическая шкала)
     */
    private static void setClipVolume(Clip clip, int volume) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

                // Если громкость 0, ставим минимально возможное значение (тишина)
                if (volume <= 0) {
                    gainControl.setValue(gainControl.getMinimum());
                    return;
                }

                // Логарифмическая формула для естественного восприятия звука ухом
                // Громкость в dB = 20 * log10(уровень от 0.0 до 1.0)
                float targetVolume = volume / 100.0f;
                float dB = (float) (Math.log10(targetVolume) * 20.0);

                // Ограничиваем значение рамками контроллера (обычно от -80.0 до 6.0)
                dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));

                gainControl.setValue(dB);
            }
        } catch (Exception e) {
            System.err.println("SoundManager: Не удалось изменить громкость.");
        }
    }

    public static void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }
}