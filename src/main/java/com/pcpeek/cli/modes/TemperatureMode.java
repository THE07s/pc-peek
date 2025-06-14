package com.pcpeek.cli.modes;

import java.util.Scanner;

public class TemperatureMode {

    public void execute(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Températures ===");
        
        if (!isWindows()) {
            System.out.println("\nLe mode Températures nécessite OpenHardwareMonitor qui n'est disponible que sur Windows.");
            System.out.println("Système détecté: " + System.getProperty("os.name"));
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        try {
            System.out.println("Surveillance des températures démarrée... (Appuyez sur Entrée pour arrêter)");

            // Thread pour vérifier l'entrée utilisateur
            Thread inputThread = new Thread(() -> {
                try {
                    scanner.nextLine();
                } catch (Exception e) {
                    // Ignorer
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();

            while (inputThread.isAlive()) {
                clearScreen();
                StringBuilder display = new StringBuilder();
                display.append("=== Mode Températures ===\n");
                display.append("Appuyez sur Entrée pour revenir au menu...\n\n");
                
                // CPU (simulation car nous n'avons pas OpenHardwareMonitor)
                double cpuTemp = simulateCpuTemperature();
                display.append("=== CPU ===\n");
                display.append(String.format("Température: %.1f°C\n", cpuTemp));
                
                // Barre de température CPU
                int barLength = 30;
                int filledLength = (int) ((cpuTemp / 100.0) * barLength);
                filledLength = Math.min(filledLength, barLength);
                filledLength = Math.max(filledLength, 0);
                
                StringBuilder tempBar = new StringBuilder();
                tempBar.append("[");
                for (int i = 0; i < barLength; i++) {
                    if (i < filledLength) {
                        if (cpuTemp >= 80) {
                            tempBar.append("█"); // Rouge pour température élevée
                        } else if (cpuTemp >= 60) {
                            tempBar.append("▓"); // Orange pour température moyenne
                        } else {
                            tempBar.append("░"); // Vert pour température normale
                        }
                    } else {
                        tempBar.append(" ");
                    }
                }
                tempBar.append("]");
                display.append("  ").append(tempBar.toString()).append("\n");
                
                // Indicateurs de température CPU
                if (cpuTemp >= 80) {
                    display.append("  ⚠️  ATTENTION: Température élevée!\n");
                    display.append("  Recommandations:\n");
                    display.append("  - Vérifiez le refroidissement\n");
                    display.append("  - Nettoyez les ventilateurs\n");
                    display.append("  - Vérifiez la pâte thermique\n");
                } else if (cpuTemp >= 60) {
                    display.append("  ℹ️  Température normale sous charge\n");
                } else {
                    display.append("  ✓ Température normale\n");
                }
                
                // GPU (simulation)
                double gpuTemp = simulateGpuTemperature();
                if (gpuTemp > 0) {
                    display.append("\n=== GPU ===\n");
                    display.append(String.format("Température: %.1f°C\n", gpuTemp));
                    
                    // Barre de température GPU
                    filledLength = (int) ((gpuTemp / 100.0) * barLength);
                    filledLength = Math.min(filledLength, barLength);
                    filledLength = Math.max(filledLength, 0);
                    
                    tempBar = new StringBuilder();
                    tempBar.append("[");
                    for (int i = 0; i < barLength; i++) {
                        if (i < filledLength) {
                            if (gpuTemp >= 80) {
                                tempBar.append("█");
                            } else if (gpuTemp >= 60) {
                                tempBar.append("▓");
                            } else {
                                tempBar.append("░");
                            }
                        } else {
                            tempBar.append(" ");
                        }
                    }
                    tempBar.append("]");
                    display.append("  ").append(tempBar.toString()).append("\n");
                    
                    // Indicateurs de température GPU
                    if (gpuTemp >= 80) {
                        display.append("  ⚠️  ATTENTION: Température élevée!\n");
                        display.append("  Recommandations:\n");
                        display.append("  - Vérifiez le refroidissement\n");
                        display.append("  - Nettoyez les ventilateurs\n");
                        display.append("  - Réduisez la charge graphique\n");
                    } else if (gpuTemp >= 60) {
                        display.append("  ℹ️  Température normale sous charge\n");
                    } else {
                        display.append("  ✓ Température normale\n");
                    }
                }
                
                // Simulation ventilateurs
                display.append("\n=== Ventilateurs ===\n");
                int[] fanSpeeds = simulateFanSpeeds(cpuTemp, gpuTemp);
                for (int i = 0; i < fanSpeeds.length; i++) {
                    if (fanSpeeds[i] > 0) {
                        display.append(String.format("Ventilateur %d: %d RPM\n", i + 1, fanSpeeds[i]));
                    }
                }

                // Légende
                display.append("\n=== Légende ===\n");
                display.append("░ : Température normale (< 60°C)\n");
                display.append("▓ : Température moyenne (60-80°C)\n");
                display.append("█ : Température élevée (> 80°C)\n");
                
                // Note sur la simulation
                display.append("\n⚠️  Note: Températures simulées (OpenHardwareMonitor requis pour valeurs réelles)\n");

                System.out.print(display.toString());
                System.out.flush();

                Thread.sleep(1000); // Mise à jour toutes les secondes
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du mode températures: " + e.getMessage());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private double simulateCpuTemperature() {
        // Simulation basée sur la charge système
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100;
        
        // Température de base + variation selon la charge + variation aléatoire
        double baseTemp = 35.0;
        double loadTemp = memoryUsage * 0.3; // Max +30°C selon la charge
        double randomVariation = (Math.random() - 0.5) * 4; // ±2°C de variation
        
        return Math.max(30.0, Math.min(85.0, baseTemp + loadTemp + randomVariation));
    }

    private double simulateGpuTemperature() {
        // Simulation GPU (généralement plus chaude que CPU)
        double cpuTemp = simulateCpuTemperature();
        double gpuOffset = 5.0 + (Math.random() * 10); // +5 à +15°C par rapport au CPU
        
        return Math.max(30.0, Math.min(90.0, cpuTemp + gpuOffset));
    }

    private int[] simulateFanSpeeds(double cpuTemp, double gpuTemp) {
        // Simulation de 2-3 ventilateurs
        int[] fanSpeeds = new int[3];
        
        // Ventilateur CPU
        fanSpeeds[0] = (int)(800 + (cpuTemp - 30) * 20); // 800-1900 RPM selon température
        
        // Ventilateur GPU
        fanSpeeds[1] = (int)(600 + (gpuTemp - 30) * 25); // 600-2100 RPM selon température
        
        // Ventilateur système
        double avgTemp = (cpuTemp + gpuTemp) / 2;
        fanSpeeds[2] = (int)(500 + (avgTemp - 30) * 15); // 500-1325 RPM selon température moyenne
        
        // S'assurer que les vitesses restent dans des limites réalistes
        for (int i = 0; i < fanSpeeds.length; i++) {
            fanSpeeds[i] = Math.max(0, Math.min(3000, fanSpeeds[i]));
        }
        
        return fanSpeeds;
    }

    private void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: imprimer des lignes vides
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
}
