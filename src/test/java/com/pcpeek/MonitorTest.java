package com.pcpeek;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pcpeek.Monitor;

import static org.junit.jupiter.api.Assertions.*;

public class MonitorTest {
    private Monitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new Monitor() {
            @Override
            protected void performUpdate() {
                // Implémentation vide pour les tests
            }

            @Override
            protected void displayContent() {
                // Implémentation vide pour les tests
            }

            @Override
            protected String getMonitorName() {
                return "TestMonitor";
            }

            @Override
            protected void initializeSystemInfo() {
                // Implémentation vide pour les tests
            }
        };
    }

    @Test
    void testIsCompatibleOS() {
        // Test sur Windows
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertTrue(monitor.isCompatibleOS(), "Le système devrait être compatible sur Windows");
        } else {
            assertFalse(monitor.isCompatibleOS(), "Le système ne devrait pas être compatible sur non-Windows");
        }
    }

    @Test
    void testFormatSize() {
        // Test des différentes tailles
        assertEquals("500 B", monitor.formatSize(500), "Formatage de 500 bytes");
        assertEquals("1.0 KB", monitor.formatSize(1024), "Formatage de 1 KB");
        assertEquals("1.5 MB", monitor.formatSize(1024 * 1024 + 512 * 1024), "Formatage de 1.5 MB");
        assertEquals("2.0 GB", monitor.formatSize(2L * 1024 * 1024 * 1024), "Formatage de 2 GB");
    }

    @Test
    void testUpdate() {
        // Test que update() ne lance pas d'exception
        assertDoesNotThrow(() -> monitor.update(), "update() ne devrait pas lancer d'exception");
    }

    @Test
    void testDisplay() {
        // Test que display() ne lance pas d'exception
        assertDoesNotThrow(() -> monitor.display(), "display() ne devrait pas lancer d'exception");
    }

    @Test
    void testGetMonitorName() {
        assertEquals("TestMonitor", monitor.getMonitorName(), "Le nom du moniteur devrait être TestMonitor");
    }
} 