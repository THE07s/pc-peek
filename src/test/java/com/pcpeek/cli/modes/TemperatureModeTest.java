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
import java.util.List;
import java.util.ArrayList;

public class TemperatureModeTest {
    private SystemData systemData;
    private TemperatureMode temperatureMode;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        systemData = new SystemData();
        temperatureMode = new TemperatureMode(systemData);
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("Test de l'initialisation du mode température")
    void testInitialization() {
        assertNotNull(temperatureMode, "Le mode température devrait être initialisé");
    }

    @Test
    @DisplayName("Test de la mise à jour des métriques")
    void testUpdateMetrics() {
        temperatureMode.updateMetrics();
        
        assertTrue(systemData.getCpuTemperature().isPresent(), "La température CPU devrait être présente");
        assertTrue(systemData.getCpuLoad().isPresent(), "La charge CPU devrait être présente");
    }

    @Test
    @DisplayName("Test du calcul de la moyenne")
    void testCalculateMean() {
        List<Double> values = new ArrayList<>();
        values.add(20.0);
        values.add(30.0);
        values.add(40.0);
        
        double mean = temperatureMode.calculateMean(values);
        assertEquals(30.0, mean, 0.001, "La moyenne devrait être correctement calculée");
    }

    @Test
    @DisplayName("Test du calcul de la variance")
    void testCalculateVariance() {
        List<Double> values = new ArrayList<>();
        values.add(20.0);
        values.add(30.0);
        values.add(40.0);
        
        double variance = temperatureMode.calculateVariance(values);
        assertEquals(100.0, variance, 0.001, "La variance devrait être correctement calculée");
    }

    @Test
    @DisplayName("Test de l'affichage de l'état actuel")
    void testShowCurrentState() {
        String input = "\n"; 
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        temperatureMode.showCurrentState(scanner);
        
        String output = outputStream.toString();
        assertTrue(output.contains("=== État Actuel ==="), "L'en-tête de l'état actuel devrait être affiché");
    }

    @Test
    @DisplayName("Test de l'analyse détaillée")
    void testShowDetailedAnalysis() {
        String input = "\n"; 
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        temperatureMode.showDetailedAnalysis(scanner);
        
        String output = outputStream.toString();
        assertTrue(output.contains("=== Analyse Détaillée ==="), "L'en-tête de l'analyse détaillée devrait être affiché");
    }
} 