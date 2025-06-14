package com.pcpeek;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pcpeek.monitors.Monitor;

import java.util.HashMap;
import java.util.Map;

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
            public String getMonitorName() {
                return "TestMonitor";
            }

            @Override
            public Map<String, Object> initializeSystemInfo() {
                return new HashMap<>();
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
    void testUpdate() {
        // Test que update() ne lance pas d'exception
        assertDoesNotThrow(() -> monitor.update(), "update() ne devrait pas lancer d'exception");
    }

    @Test
    void testDisplay() {
        // Test que display() ne lance pas d'exception
        assertDoesNotThrow(() -> monitor.display(), "display() ne devrait pas lancer d'exception");
    }
}