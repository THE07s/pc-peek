package com.pcpeek;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pcpeek.OSMonitor;

import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class OSMonitorTest {
    private OSMonitor osMonitor;

    @BeforeEach
    void setUp() {
        osMonitor = new OSMonitor();
    }

    @Test
    @DisplayName("Test de la détection du système d'exploitation")
    void testOSDetection() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            assertTrue(osMonitor.isCompatibleOS(), 
                "Le système devrait être détecté comme compatible sur Windows");
        } else {
            assertFalse(osMonitor.isCompatibleOS(), 
                "Le système ne devrait pas être détecté comme compatible sur non-Windows");
        }
    }

    @Test
    @DisplayName("Test de la mise à jour en temps réel")
    void testRealTimeUpdate() {
        // Test que la mise à jour ne lance pas d'exception
        assertDoesNotThrow(() -> osMonitor.update(),
            "La mise à jour ne devrait pas lancer d'exception");
    }

    @Test
    @DisplayName("Test de l'affichage")
    void testDisplay() {
        // Test que l'affichage ne lance pas d'exception
        assertDoesNotThrow(() -> osMonitor.display(),
            "L'affichage ne devrait pas lancer d'exception");
    }

    @Test
    @DisplayName("Test de la gestion des erreurs")
    void testErrorHandling() {
        // Test que les opérations de base ne lancent pas d'exception
        assertDoesNotThrow(() -> {
            osMonitor.update();
            osMonitor.display();
            osMonitor.isCompatibleOS();
        }, "Les opérations de base ne devraient pas lancer d'exception");
    }

    @Test
    @DisplayName("Test de la cohérence du moniteur")
    void testMonitorConsistency() {
        // Test que le moniteur maintient un état cohérent
        assertDoesNotThrow(() -> {
            // Première mise à jour
            osMonitor.update();
            osMonitor.display();
            
            // Attendre un court instant
            Thread.sleep(1000);
            
            // Deuxième mise à jour
            osMonitor.update();
            osMonitor.display();
        }, "Le moniteur devrait maintenir un état cohérent entre les mises à jour");
    }
} 