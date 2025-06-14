package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import com.pcpeek.monitors.dynamicinfo.ProbeMonitor;
import com.pcpeek.monitors.dynamicinfo.ResourceMonitor;

import java.util.Scanner;

public class RealTimeMode {
    private final SystemData systemData;
    private final ProbeMonitor probeMonitor;
    private final ResourceMonitor resourceMonitor;

    public RealTimeMode() {
        this.systemData = new SystemData();
        this.probeMonitor = new ProbeMonitor();
        this.resourceMonitor = new ResourceMonitor();
    }

    public void execute(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Real Time ===");
        
        // Vérifier la compatibilité OS pour OpenHardwareMonitor
        if (!isWindows()) {
            System.out.println("\nLe mode Real Time nécessite OpenHardwareMonitor qui n'est disponible que sur Windows.");
            System.out.println("Système détecté: " + System.getProperty("os.name"));
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        try {
            boolean monitoring = true;
            System.out.println("Surveillance temps réel démarrée... (Appuyez sur Entrée pour arrêter)");

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

            while (monitoring && inputThread.isAlive()) {
                clearScreen();
                StringBuilder display = new StringBuilder();
                display.append("=== Mode Real Time ===\n");
                display.append("Appuyez sur Entrée pour revenir au menu...\n\n");
                
                // Mettre à jour les données (les moniteurs se mettent à jour automatiquement)
                // resourceMonitor et probeMonitor peuvent être utilisés pour récupérer des données
                
                // Afficher les informations CPU
                display.append("=== CPU ===\n");
                display.append("Processeurs disponibles: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
                
                // Charge CPU (simulation basique)
                double cpuLoad = getCpuLoad();
                display.append(String.format("Charge CPU: %.1f%%\n", cpuLoad));
                
                // Barre de progression CPU
                int barLength = 30;
                int filledLength = (int) (cpuLoad * barLength / 100);
                StringBuilder cpuBar = new StringBuilder();
                cpuBar.append("[");
                for (int i = 0; i < barLength; i++) {
                    if (i < filledLength) {
                        if (cpuLoad >= 80) {
                            cpuBar.append("█");
                        } else if (cpuLoad >= 50) {
                            cpuBar.append("▓");
                        } else {
                            cpuBar.append("░");
                        }
                    } else {
                        cpuBar.append(" ");
                    }
                }
                cpuBar.append("]");
                display.append("  ").append(cpuBar.toString()).append("\n");
                
                // Mémoire
                display.append("\n=== Mémoire ===\n");
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                double memoryUsage = (usedMemory * 100.0) / totalMemory;
                
                display.append(String.format("Totale (JVM): %s\n", formatSize(totalMemory)));
                display.append(String.format("Utilisée: %s (%.1f%%)\n", formatSize(usedMemory), memoryUsage));
                display.append(String.format("Libre: %s\n", formatSize(freeMemory)));
                
                // Barre de progression mémoire
                int memFilledLength = (int) (memoryUsage * barLength / 100);
                StringBuilder memBar = new StringBuilder();
                memBar.append("[");
                for (int i = 0; i < barLength; i++) {
                    if (i < memFilledLength) {
                        if (memoryUsage >= 90) {
                            memBar.append("█");
                        } else if (memoryUsage >= 70) {
                            memBar.append("▓");
                        } else {
                            memBar.append("░");
                        }
                    } else {
                        memBar.append(" ");
                    }
                }
                memBar.append("]");
                display.append("  ").append(memBar.toString()).append("\n");
                
                // Informations système
                display.append("\n=== Système ===\n");
                display.append("OS: ").append(System.getProperty("os.name")).append(" ")
                       .append(System.getProperty("os.version")).append("\n");
                display.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
                display.append("Java: ").append(System.getProperty("java.version")).append("\n");

                System.out.print(display.toString());
                System.out.flush();

                // Vérifier si l'utilisateur a appuyé sur Entrée
                if (!inputThread.isAlive()) {
                    monitoring = false;
                }
                
                Thread.sleep(1000); // Mise à jour toutes les secondes
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du mode temps réel: " + e.getMessage());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private double getCpuLoad() {
        // Simulation simple de charge CPU
        // Dans un cas réel, on utiliserait OperatingSystemMXBean ou OpenHardwareMonitor
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        double memoryUsage = ((double)(totalMemory - freeMemory) / totalMemory) * 100;
        
        // Approximation basée sur l'utilisation mémoire + facteur aléatoire
        return Math.min(95.0, memoryUsage + (Math.random() * 20 - 10));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
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
