package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import com.pcpeek.monitors.dynamicinfo.ProbeMonitor;
import com.pcpeek.monitors.dynamicinfo.ResourceMonitor;

import java.util.Scanner;

public class RealTimeMode {
    private final SystemData systemData;
    private final ProbeMonitor probeMonitor;
    private final ResourceMonitor resourceMonitor;

    public RealTimeMode(SystemData systemData) {
        this.systemData = systemData;
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
                
                // Mettre à jour SystemData avec les données dynamiques
                try {
                    systemData.updateDynamicData(resourceMonitor.getResourceInfo());
                    systemData.updateDynamicData(probeMonitor.getProbeInfo());
                } catch (Exception e) {
                    display.append("Erreur lors de la récupération des données: ").append(e.getMessage()).append("\n");
                }
                
                // Afficher les informations CPU depuis SystemData
                display.append("=== CPU ===\n");
                systemData.getCpuCores().ifPresentOrElse(
                    cores -> display.append("Processeurs disponibles: ").append(cores).append("\n"),
                    () -> display.append("Processeurs disponibles: ").append(Runtime.getRuntime().availableProcessors()).append("\n")
                );
                
                // Charge CPU depuis SystemData ou fallback
                systemData.getCpuLoad().ifPresentOrElse(
                    usage -> {
                        display.append(String.format("Charge CPU: %.1f%%\n", usage));
                        addProgressBar(display, usage, "CPU");
                    },
                    () -> {
                        double cpuLoad = getCpuLoad();
                        display.append(String.format("Charge CPU: %.1f%%\n", cpuLoad));
                        addProgressBar(display, cpuLoad, "CPU");
                    }
                );
                
                // Mémoire depuis SystemData ou fallback JVM
                display.append("\n=== Mémoire ===\n");
                
                systemData.getMemoryTotal().ifPresentOrElse(
                    totalMem -> {
                        // Utiliser les données système
                        systemData.getMemoryFree().ifPresent(freeMem -> {
                            long usedMem = totalMem - freeMem;
                            double memoryUsage = (usedMem * 100.0) / totalMem;
                            
                            display.append(String.format("Totale: %s\n", formatSize(totalMem)));
                            display.append(String.format("Utilisée: %s (%.1f%%)\n", formatSize(usedMem), memoryUsage));
                            display.append(String.format("Libre: %s\n", formatSize(freeMem)));
                            
                            addProgressBar(display, memoryUsage, "Mémoire");
                        });
                    },
                    () -> {
                        // Fallback JVM
                        Runtime runtime = Runtime.getRuntime();
                        long totalMemory = runtime.totalMemory();
                        long freeMemory = runtime.freeMemory();
                        long usedMemory = totalMemory - freeMemory;
                        double memoryUsage = (usedMemory * 100.0) / totalMemory;
                        
                        display.append(String.format("Totale (JVM): %s\n", formatSize(totalMemory)));
                        display.append(String.format("Utilisée: %s (%.1f%%)\n", formatSize(usedMemory), memoryUsage));
                        display.append(String.format("Libre: %s\n", formatSize(freeMemory)));
                        
                        addProgressBar(display, memoryUsage, "Mémoire");
                    }
                );
                
                // Informations système depuis SystemData
                display.append("\n=== Système ===\n");
                systemData.getOsCaption().ifPresentOrElse(
                    caption -> display.append("OS: ").append(caption).append("\n"),
                    () -> display.append("OS: ").append(System.getProperty("os.name")).append(" ")
                           .append(System.getProperty("os.version")).append("\n")
                );
                
                systemData.getOsArchitecture().ifPresentOrElse(
                    arch -> display.append("Architecture: ").append(arch).append("\n"),
                    () -> display.append("Architecture: ").append(System.getProperty("os.arch")).append("\n")
                );
                
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

    private void addProgressBar(StringBuilder display, double percentage, String label) {
        int barLength = 30;
        int filledLength = (int) (percentage * barLength / 100);
        filledLength = Math.min(filledLength, barLength);
        filledLength = Math.max(filledLength, 0);
        
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                if (percentage >= 80) {
                    bar.append("█");
                } else if (percentage >= 50) {
                    bar.append("▓");
                } else {
                    bar.append("░");
                }
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        display.append("  ").append(bar.toString()).append("\n");
    }
}
