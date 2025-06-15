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
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;

public class StaticInfoModeTest {
    private SystemData systemData;
    private StaticInfoMode staticInfoMode;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        systemData = new SystemData();
        staticInfoMode = spy(new StaticInfoMode(systemData));
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("Test de l'initialisation du mode statique")
    void testInitialization() {
        assertNotNull(staticInfoMode, "Le mode statique devrait être initialisé");
    }

    @Test
    @DisplayName("Test de la collecte des informations système")
    void testSystemInfoCollection() {
        staticInfoMode.collectSystemInfo();
        verify(staticInfoMode, times(1)).collectSystemInfo();
    }

    @Test
    @DisplayName("Test de la collecte des informations CPU")
    void testCpuInfoCollection() {
        staticInfoMode.collectCpuInfo();
        assertTrue(systemData.getCpuName().isPresent(), "Le nom du CPU devrait être présent");
    }

    @Test
    @DisplayName("Test de la collecte des informations disque")
    void testDiskInfoCollection() {
        staticInfoMode.collectDiskInfo();
        assertTrue(systemData.getDiskSize().isPresent(), "La taille du disque devrait être présente");
    }

    @Test
    @DisplayName("Test de l'exécution du mode statique")
    void testExecute() {
        String input = "\n"; 
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        staticInfoMode.execute(scanner);
        
        String output = outputStream.toString();
        assertTrue(output.contains("=== Informations STATIC ==="), "L'en-tête du mode statique devrait être affiché");
    }

    @Test
    @DisplayName("Test complet du mode statique")
    void testStaticModeComplete() {
        systemData.setCpuName("Intel Core i7");
        systemData.setCpuCores(8L);
        systemData.setCpuThreads(16L);
        systemData.setTotalMemory(16384L * 1024 * 1024); 
        systemData.setOsName("Windows 10");
        systemData.setOsVersion("10.0");

        String input = "\n"; 
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        staticInfoMode.execute(scanner);

        String output = outputStream.toString();
        System.setOut(originalOut);
        System.out.println("\n=== Résultat du test du mode informations statiques ===");
        System.out.println(output);

        assertTrue(output.contains("=== Informations STATIC ==="), "Titre manquant");
        assertTrue(output.contains("Processeur : Intel Core i7"), "Modèle CPU incorrect");
        assertTrue(output.contains("Cœurs physiques : 8"), "Nombre de cœurs incorrect");
        assertTrue(output.contains("Threads logiques : 16"), "Nombre de threads incorrect");
        assertTrue(output.contains("Mémoire totale : 16.0 GB"), "Mémoire totale incorrecte");
        assertTrue(output.contains("Mémoire utilisée : "), "Mémoire utilisée manquante");
        assertTrue(output.contains("Mémoire libre : "), "Mémoire libre manquante");
        assertTrue(output.contains("Utilisation : "), "Utilisation RAM manquante");
        assertTrue(output.contains("["), "Barre de progression RAM manquante (début)");
        assertTrue(output.contains("]"), "Barre de progression RAM manquante (fin)");
        assertTrue(output.matches(".*[█▓░ ]+.*"), "Caractères de barre de progression RAM manquants");

        assertTrue(output.contains("=== Cartes Graphiques ==="), "Titre cartes graphiques manquant");
        assertTrue(output.contains("Aucune carte graphique détectée"), "Message GPU non détecté manquant");

        assertTrue(output.contains("=== Disques ==="), "Titre disques manquant");

        assertTrue(output.contains("=== Réseau ==="), "Titre réseau manquant");
        assertTrue(output.contains("Aucune interface réseau active détectée"), "Message réseau non détecté manquant");

        assertTrue(output.contains("=== État d'Activation Windows ==="), "Titre activation Windows manquant");
        assertTrue(output.contains("Impossible de déterminer l'état d'activation de Windows"), "Message activation Windows non déterminée manquant");

        assertTrue(output.contains("=== Informations Batterie ==="), "Titre batterie manquant");
        assertTrue(output.contains("Aucune batterie détectée"), "Message batterie non détectée manquant");
        
        assertTrue(output.contains("Système d'exploitation : Windows 10 10.0"), "Système d'exploitation incorrect");
        assertTrue(output.contains("Architecture : x64"), "Architecture incorrecte");
    }
} 