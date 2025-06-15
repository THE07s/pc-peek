package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.ByteArrayInputStream;

public class TemperatureModeTest {
    private TemperatureMode temperatureMode;
    private SystemData systemData;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        systemData = new SystemData();
        temperatureMode = new TemperatureMode(systemData);
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        scanner = new Scanner(System.in);
    }

    @Test
    @DisplayName("Test de la lecture des températures")
    void testTemperatureReading() {
        // Arrange
        Map<String, Object> tempInfo = new HashMap<>();
        tempInfo.put("cpu.temperature", 45.5);
        tempInfo.put("gpu.temperature", 60.0);
        systemData.updateDynamicData(tempInfo);

        // Act
        temperatureMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Mode Diagnostic ==="));
        assertTrue(output.contains("Température CPU : 45.5°C"));
        assertTrue(output.contains("Température GPU : 60.0°C"));
    }

    @Test
    @DisplayName("Test de l'affichage des diagnostics")
    void testDiagnosticDisplay() {
        // Arrange
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.temperature", 80.0);
        systemInfo.put("cpu.load", 90.0);
        systemInfo.put("memory.used", 15032385536L); // ~14 GB
        systemInfo.put("memory.total", 17179869184L); // 16 GB
        systemData.updateDynamicData(systemInfo);

        // Act
        temperatureMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Mode Diagnostic ==="));
        assertTrue(output.contains("Température CPU : 80.0°C"));
        assertTrue(output.contains("Charge CPU : 90.0%"));
        assertTrue(output.contains("Mémoire utilisée :"));
    }

    @Test
    @DisplayName("Test des alertes de température")
    void testTemperatureAlerts() {
        // Arrange
        Map<String, Object> highTempInfo = new HashMap<>();
        highTempInfo.put("cpu.temperature", 95.0);
        systemData.updateDynamicData(highTempInfo);

        // Act
        temperatureMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("ALERTE"));
        assertTrue(output.contains("Température élevée"));
    }

    @Test
    @DisplayName("Test de l'affichage des barres de progression")
    void testProgressBars() {
        // Arrange
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.temperature", 75.0);
        systemInfo.put("cpu.load", 85.0);
        systemInfo.put("memory.used", 12884901888L); // 12 GB
        systemInfo.put("memory.total", 17179869184L); // 16 GB
        systemData.updateDynamicData(systemInfo);

        // Act
        temperatureMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
        assertTrue(output.contains("█"));
    }

    @Test
    @DisplayName("Test du mode diagnostic")
    void testTemperatureMode() {
        // Simuler les données système
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.load", 75.5);
        systemInfo.put("cpu.temperature", 85.0);
        systemInfo.put("gpu.load", 90.0);
        systemInfo.put("gpu.temperature", 95.0);
        systemInfo.put("memory.used", 25769803776L); // 24 GB
        systemInfo.put("memory.total", 34359738368L); // 32 GB
        systemInfo.put("disk.used", 429496729600L); // 400 GB
        systemInfo.put("disk.total", 1073741824000L); // 1 TB
        systemData.updateDynamicData(systemInfo);

        // Simuler l'entrée utilisateur (appuyer sur Entrée pour quitter)
        String input = "\n";
        ByteArrayInputStream inContent = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inContent);

        // Exécuter le mode diagnostic
        temperatureMode.execute(scanner);

        // Vérifier l'affichage
        String output = outContent.toString();
        System.out.println("\n=== Résultat du test du mode diagnostic ===");
        System.out.println(output);

        // Vérifications
        assertTrue(output.contains("=== Mode Diagnostic ==="), "Titre manquant");
        assertTrue(output.contains("Charge CPU : 75.5%"), "Charge CPU incorrecte");
        assertTrue(output.contains("Température CPU : 85.0°C"), "Température CPU incorrecte");
        assertTrue(output.contains("Charge GPU : 90.0%"), "Charge GPU incorrecte");
        assertTrue(output.contains("Température GPU : 95.0°C"), "Température GPU incorrecte");
        assertTrue(output.contains("Mémoire utilisée : 24.0 GB"), "Mémoire utilisée incorrecte");
        assertTrue(output.contains("Espace disque utilisé : 400.0 GB"), "Espace disque incorrect");

        // Vérifier les alertes de température
        assertTrue(output.contains("[ALERTE]"), "Symbole d'alerte manquant");
        assertTrue(output.contains("Température élevée"), "Message d'alerte manquant");

        // Vérifier les barres de progression
        assertTrue(output.contains("["), "Barre de progression manquante");
        assertTrue(output.contains("]"), "Barre de progression manquante");
        assertTrue(output.contains("█"), "Caractère de progression manquant");
    }
} 