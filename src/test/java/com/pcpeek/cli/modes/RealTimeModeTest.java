package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Scanner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class RealTimeModeTest {
    private SystemData systemData;
    private RealTimeMode realTimeMode;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        systemData = new SystemData();
        realTimeMode = new RealTimeMode(systemData);
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("Test de l'initialisation du mode temps réel")
    void testInitialization() {
        assertNotNull(realTimeMode, "Le mode temps réel devrait être initialisé");
    }

    @Test
    @DisplayName("Test de la détection du système d'exploitation")
    void testOSDetection() {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("windows");
        assertEquals(isWindows, realTimeMode.isWindows(), "La détection du système d'exploitation devrait être correcte");
    }

    @Test
    @DisplayName("Test du formatage de la taille")
    void testFormatSize() {
        assertEquals("1.0 KB", realTimeMode.formatSize(1024), "Le formatage de 1024 bytes devrait donner 1.0 KB");
        assertEquals("1.0 MB", realTimeMode.formatSize(1024 * 1024), "Le formatage de 1MB devrait être correct");
        assertEquals("1.0 GB", realTimeMode.formatSize(1024 * 1024 * 1024), "Le formatage de 1GB devrait être correct");
    }

    @Test
    @DisplayName("Test de la mise à jour des données système")
    void testSystemDataUpdate() {
        realTimeMode.updateSystemData();
        assertTrue(systemData.getCpuCores().isPresent(), "Le nombre de cœurs CPU devrait être présent");
        assertTrue(systemData.getCpuLoad().isPresent(), "La charge CPU devrait être présente");
        assertTrue(systemData.getTotalMemory().isPresent(), "La mémoire totale devrait être présente");
        assertTrue(systemData.getAvailableMemory().isPresent(), "La mémoire disponible devrait être présente");
    }

    @Test
    @DisplayName("Test de l'exécution du mode temps réel")
    void testExecute() {
        String input = "\n"; // Simule l'appui sur Entrée
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        realTimeMode.execute(scanner);
        
        String output = outputStream.toString();
        assertTrue(output.contains("=== Mode Real Time ==="), "L'en-tête du mode temps réel devrait être affiché");
    }
} 