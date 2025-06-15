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

public class StaticInfoModeTest {
    private StaticInfoMode staticInfoMode;
    private SystemData systemData;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        systemData = new SystemData();
        staticInfoMode = new StaticInfoMode(systemData);
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        scanner = new Scanner(System.in);
    }

    @Test
    @DisplayName("Test de l'affichage des informations CPU")
    void testDisplayCPUInfo() {
        // Arrange
        Map<String, Object> cpuInfo = new HashMap<>();
        cpuInfo.put("cpu.name", "Intel Core i7");
        cpuInfo.put("cpu.cores", 8);
        cpuInfo.put("cpu.threads", 16);
        systemData.updateStaticData(cpuInfo);

        // Act
        staticInfoMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Processeur ==="));
        assertTrue(output.contains("Intel Core i7"));
        assertTrue(output.contains("Cœurs physiques : 8"));
        assertTrue(output.contains("Cœurs logiques : 16"));
    }

    @Test
    @DisplayName("Test de l'affichage des informations RAM")
    void testDisplayRAMInfo() {
        // Arrange
        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("memory.total", 16777216000L); // 16 GB
        systemData.updateStaticData(memoryInfo);

        // Act
        staticInfoMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Mémoire RAM ==="));
        assertTrue(output.contains("Mémoire totale : 15.6 GB"));
        assertTrue(output.contains("Utilisation :"));
    }

    @Test
    @DisplayName("Test de l'affichage des informations disque")
    void testDisplayDiskInfo() {
        // Act
        staticInfoMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Disques ==="));
        assertTrue(output.contains("Espace total :"));
        assertTrue(output.contains("Espace libre :"));
        assertTrue(output.contains("Espace utilisé :"));
    }

    @Test
    @DisplayName("Test de l'affichage des informations réseau")
    void testDisplayNetworkInfo() {
        // Act
        staticInfoMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Réseau ==="));
        assertTrue(output.contains("Nom d'hôte :"));
    }

    @Test
    @DisplayName("Test de l'affichage des informations Windows")
    void testDisplayWindowsInfo() {
        // Arrange
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Map<String, Object> windowsInfo = new HashMap<>();
            windowsInfo.put("os.caption", "Windows 10 Pro");
            windowsInfo.put("os.serial", "XXXXX-XXXXX-XXXXX-XXXXX");
            windowsInfo.put("os.license", "2024-01-01");
            systemData.updateStaticData(windowsInfo);

            // Act
            staticInfoMode.execute(scanner);

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("=== État d'Activation Windows ==="));
            assertTrue(output.contains("Windows 10 Pro"));
            assertTrue(output.contains("XXXXX-XXXXX-XXXXX-XXXXX"));
            assertTrue(output.contains("2024-01-01"));
        }
    }

    @Test
    @DisplayName("Test de l'affichage des informations batterie")
    void testDisplayBatteryInfo() {
        // Act
        staticInfoMode.execute(scanner);

        // Assert
        String output = outContent.toString();
        assertTrue(output.contains("=== Informations Batterie ==="));
        // La présence de batterie dépend du système
        assertTrue(output.contains("Niveau de charge :") || output.contains("Aucune batterie détectée"));
    }

    @Test
    @DisplayName("Test de la gestion des erreurs")
    void testErrorHandling() {
        // Arrange
        System.setOut(originalOut); // Restaurer la sortie standard
        System.setOut(null); // Simuler une erreur d'écriture

        // Act & Assert
        assertDoesNotThrow(() -> staticInfoMode.execute(scanner), 
            "Le mode statique devrait gérer les erreurs d'écriture");
    }

    @Test
    @DisplayName("Test complet du mode statique")
    void testStaticModeComplete() {
        // Simuler les données système
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("cpu.name", "Intel Core i7-12700K");
        systemInfo.put("cpu.cores", 12);
        systemInfo.put("cpu.threads", 20);
        systemInfo.put("memory.total", 34359738368L); // 32 GB
        systemInfo.put("os.caption", "Windows 11 Pro");
        systemInfo.put("os.serial", "XXXXX-XXXXX-XXXXX-XXXXX");
        systemInfo.put("os.license", "2024-01-01");
        systemData.updateStaticData(systemInfo);

        // Simuler l'entrée utilisateur (appuyer sur Entrée pour quitter)
        String input = "\n";
        ByteArrayInputStream inContent = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inContent);

        // Exécuter le mode statique
        staticInfoMode.execute(scanner);

        // Vérifier l'affichage
        String output = outContent.toString();
        System.out.println("\n=== Résultat du test du mode statique ===");
        System.out.println(output);

        // Vérifications
        assertTrue(output.contains("=== Processeur ==="), "Section CPU manquante");
        assertTrue(output.contains("Intel Core i7-12700K"), "Nom du CPU incorrect");
        assertTrue(output.contains("Cœurs physiques : 12"), "Nombre de cœurs incorrect");
        assertTrue(output.contains("Cœurs logiques : 20"), "Nombre de threads incorrect");

        assertTrue(output.contains("=== Mémoire RAM ==="), "Section RAM manquante");
        assertTrue(output.contains("Mémoire totale : 32.0 GB"), "Mémoire totale incorrecte");

        assertTrue(output.contains("=== Disques ==="), "Section disques manquante");
        assertTrue(output.contains("Espace total :"), "Informations disque manquantes");

        assertTrue(output.contains("=== Réseau ==="), "Section réseau manquante");
        assertTrue(output.contains("Nom d'hôte :"), "Informations réseau manquantes");

        assertTrue(output.contains("=== État d'Activation Windows ==="), "Section Windows manquante");
        assertTrue(output.contains("Windows 11 Pro"), "Version Windows incorrecte");
        assertTrue(output.contains("XXXXX-XXXXX-XXXXX-XXXXX"), "Numéro de série manquant");

        assertTrue(output.contains("=== Informations Batterie ==="), "Section batterie manquante");
    }
} 