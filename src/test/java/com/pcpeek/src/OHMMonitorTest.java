package com.pcpeek.src;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class OHMMonitorTest {
    private OHMMonitor ohmMonitor;

    @BeforeEach
    void setUp() {
        ohmMonitor = new OHMMonitor();
    }

    @Test
    @DisplayName("Test de la connexion à OpenHardwareMonitor")
    void testOHMConnection() {
        boolean connected = ohmMonitor.connect();
        
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // Sur Windows, la connexion peut réussir ou échouer selon si OHM est disponible
            assertDoesNotThrow(() -> ohmMonitor.connect(),
                "La connexion ne devrait pas lancer d'exception sur Windows");
        } else {
            // Sur non-Windows, la connexion devrait échouer proprement
            assertFalse(connected,
                "La connexion devrait échouer sur non-Windows");
        }
    }

    @Test
    @DisplayName("Test de la récupération de la température CPU")
    void testCpuTemperature() {
        double temp = ohmMonitor.getCpuTemperature();
        
        if (ohmMonitor.isConnected()) {
            // Si connecté à OHM, la température devrait être valide
            assertTrue(temp >= 0 && temp <= 100,
                "La température CPU devrait être entre 0 et 100°C quand OHM est connecté");
        } else {
            // Si non connecté, la température devrait être -1
            assertEquals(-1, temp,
                "La température devrait être -1 quand OHM n'est pas connecté");
        }
    }

    @Test
    @DisplayName("Test de la mise à jour des capteurs")
    void testSensorUpdate() {
        // Premier relevé
        double temp1 = ohmMonitor.getCpuTemperature();
        
        // Mise à jour des capteurs
        ohmMonitor.updateSensors();
        
        // Deuxième relevé
        double temp2 = ohmMonitor.getCpuTemperature();

        if (ohmMonitor.isConnected()) {
            // Si connecté, les températures devraient être différentes
            // (sauf cas très particulier où la température est stable)
            assertTrue(temp1 == temp2 || Math.abs(temp1 - temp2) < 10,
                "Les températures devraient être similaires ou différentes de moins de 10°C");
        } else {
            // Si non connecté, les deux températures devraient être -1
            assertEquals(-1, temp1, "La première température devrait être -1 si non connecté");
            assertEquals(-1, temp2, "La deuxième température devrait être -1 si non connecté");
        }
    }

    @Test
    @DisplayName("Test de la gestion des erreurs de connexion")
    void testConnectionErrorHandling() {
        // Simuler plusieurs tentatives de connexion
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> {
                ohmMonitor.connect();
                ohmMonitor.getCpuTemperature();
                ohmMonitor.updateSensors();
            }, "Les opérations ne devraient pas lancer d'exception même en cas d'erreur de connexion");
        }
    }

    @Test
    @DisplayName("Test de la cohérence des données de température")
    void testTemperatureDataConsistency() {
        if (ohmMonitor.isConnected()) {
            // Faire plusieurs relevés pour vérifier la cohérence
            double[] temps = new double[5];
            for (int i = 0; i < temps.length; i++) {
                ohmMonitor.updateSensors();
                temps[i] = ohmMonitor.getCpuTemperature();
                try {
                    Thread.sleep(100); // Petit délai entre les relevés
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Vérifier que les températures sont cohérentes
            double maxDiff = 0;
            for (int i = 1; i < temps.length; i++) {
                maxDiff = Math.max(maxDiff, Math.abs(temps[i] - temps[i-1]));
            }
            
            // La différence maximale entre deux relevés consécutifs ne devrait pas être excessive
            assertTrue(maxDiff < 20,
                "La différence maximale entre deux relevés ne devrait pas dépasser 20°C");
        }
    }
} 