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

public class RealTimeModeTest {
    private RealTimeMode realTimeMode;
    private SystemData systemData;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        systemData = new SystemData();
        realTimeMode = new RealTimeMode(systemData);
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        scanner = new Scanner(System.in);
    }

    @Test
    @DisplayName("Test du calcul de la charge CPU")
    void testCpuLoadCalculation() {
        // Arrange
        Map<String, Object> cpuInfo = new HashMap<>();
        cpuInfo.put("cpu.load", 25.5);
        systemData.updateDynamicData(cpuInfo);

        // Act
        realTimeMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Charge CPU :"));
        assertTrue(output.contains("25.5%"));
    }

    @Test
    @DisplayName("Test de l'affichage en temps réel")
    void testRealTimeDisplay() {
        // Arrange
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.load", 30.0);
        systemInfo.put("memory.used", 8589934592L); // 8 GB
        systemInfo.put("memory.total", 17179869184L); // 16 GB
        systemData.updateDynamicData(systemInfo);

        // Act
        realTimeMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Mode Temps Réel ==="));
        assertTrue(output.contains("Charge CPU :"));
        assertTrue(output.contains("Mémoire utilisée :"));
    }

    @Test
    @DisplayName("Test de la mise à jour des données")
    void testDataUpdate() {
        // Arrange
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("cpu.load", 20.0);
        systemData.updateDynamicData(initialData);

        // Act
        realTimeMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("Charge CPU : 20.0%"));
    }

    @Test
    @DisplayName("Test de la gestion des erreurs")
    void testErrorHandling() {
        // Arrange
        System.setOut(originalOut); // Restaurer la sortie standard
        System.setOut(null); // Simuler une erreur d'écriture

        // Act & Assert
        assertDoesNotThrow(() -> realTimeMode.execute(scanner), 
            "Le mode temps réel devrait gérer les erreurs d'écriture");
    }

    @Test
    @DisplayName("Test de l'affichage des barres de progression")
    void testProgressBars() {
        // Arrange
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.load", 75.0);
        systemInfo.put("memory.used", 12884901888L); // 12 GB
        systemInfo.put("memory.total", 17179869184L); // 16 GB
        systemData.updateDynamicData(systemInfo);

        // Act
        realTimeMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
        assertTrue(output.contains("█"));
    }

    @Test
    @DisplayName("Test du mode temps réel")
    void testRealTimeMode() {
        // Simuler les données système en temps réel
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.load", 45.5);
        systemInfo.put("memory.used", 17179869184L); // 16 GB
        systemInfo.put("memory.total", 34359738368L); // 32 GB
        systemInfo.put("gpu.load", 60.0);
        systemInfo.put("gpu.temperature", 75.0);
        systemData.updateDynamicData(systemInfo);

        // Simuler l'entrée utilisateur (appuyer sur Entrée pour quitter)
        String input = "\n";
        ByteArrayInputStream inContent = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inContent);

        // Exécuter le mode temps réel
        realTimeMode.execute(scanner);

        // Vérifier l'affichage
        String output = outContent.toString();
        System.out.println("\n=== Résultat du test du mode temps réel ===");
        System.out.println(output);

        // Vérifications
        assertTrue(output.contains("=== Mode Temps Réel ==="), "Titre manquant");
        assertTrue(output.contains("Charge CPU : 45.5%"), "Charge CPU incorrecte");
        assertTrue(output.contains("Mémoire utilisée : 16.0 GB"), "Mémoire utilisée incorrecte");
        assertTrue(output.contains("Charge GPU : 60.0%"), "Charge GPU incorrecte");
        assertTrue(output.contains("Température GPU : 75.0°C"), "Température GPU incorrecte");

        // Vérifier les barres de progression
        assertTrue(output.contains("["), "Barre de progression manquante");
        assertTrue(output.contains("]"), "Barre de progression manquante");
        assertTrue(output.contains("█"), "Caractère de progression manquant");
    }
} 