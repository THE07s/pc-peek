package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class RealTimeMode {
    private final SystemData systemData;
    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final DateTimeFormatter timeFormatter;
    private long[] prevTicks;

    public RealTimeMode(SystemData systemData) {
        this.systemData = systemData;
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.prevTicks = new long[CentralProcessor.TickType.values().length];
    }

    private double getCpuLoad() {
        CentralProcessor processor = hardware.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long[] ticks = processor.getSystemCpuLoadTicks();
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + sys + idle;
        return totalCpu > 0 ? 100.0 * (totalCpu - idle) / totalCpu : 0.0;
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

            // Première lecture pour initialiser les ticks
            hardware.getProcessor().getSystemCpuLoadTicks();            while (monitoring && inputThread.isAlive()) {
                clearScreen();
                StringBuilder display = new StringBuilder();
                display.append("=== Mode Real Time ===\n");
                display.append("Appuyez sur Entrée pour revenir au menu...\n\n");
                
                // Mettre à jour SystemData avec les nouvelles données
                updateSystemData();
                
                // Afficher l'heure actuelle
                display.append("=== Heure ===\n");
                display.append(LocalDateTime.now().format(timeFormatter)).append("\n\n");
                
                // Afficher les informations CPU depuis SystemData
                display.append("=== CPU ===\n");
                CentralProcessor processor = hardware.getProcessor();
                long cpuCores = processor.getLogicalProcessorCount();
                systemData.setCpuCores(cpuCores);
                display.append("Processeurs disponibles: ").append(cpuCores).append("\n");
                
                // Charge CPU
                double cpuLoad = getCpuLoad();
                systemData.setCpuLoad(cpuLoad);
                display.append(String.format("Charge CPU: %.1f%%\n", cpuLoad));
                
                // Barre de progression CPU
                addProgressBar(display, cpuLoad, "CPU");
                
                // Mémoire
                display.append("\n=== Mémoire ===\n");
                GlobalMemory memory = hardware.getMemory();
                long totalMemory = memory.getTotal();
                long availableMemory = memory.getAvailable();
                long usedMemory = totalMemory - availableMemory;
                double memoryUsage = (usedMemory * 100.0) / totalMemory;
                
                // Mettre à jour SystemData
                systemData.setTotalMemory(totalMemory);
                systemData.setAvailableMemory(availableMemory);
                
                display.append(String.format("Totale: %s\n", formatSize(totalMemory)));
                display.append(String.format("Utilisée: %s (%.1f%%)\n", formatSize(usedMemory), memoryUsage));
                display.append(String.format("Libre: %s\n", formatSize(availableMemory)));
                
                // Barre de progression mémoire
                addProgressBar(display, memoryUsage, "Mémoire");

                // Températures
                display.append("\nTempératures:\n");
                try {
                    double cpuTemp = hardware.getSensors().getCpuTemperature();
                    systemData.setCpuTemperature(cpuTemp);
                    if (cpuTemp > 0) {
                        display.append(String.format("CPU: %.1f°C\n", cpuTemp));
                        addTemperatureBar(display, cpuTemp);
                    } else {
                        display.append("Température CPU non disponible\n");
                    }
                } catch (Exception e) {
                    display.append("Température CPU non disponible\n");
                }

                // Ventilateurs via WMI
                display.append("\nVentilateurs:\n");
                displayFanInfo(display);

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

    private void updateSystemData() {
        try {
            // Mettre à jour les informations système de base
            CentralProcessor processor = hardware.getProcessor();
            GlobalMemory memory = hardware.getMemory();
            
            // CPU
            systemData.setCpuCores((long) processor.getLogicalProcessorCount());
            systemData.setCpuLoad(getCpuLoad());
            
            // Mémoire
            systemData.setTotalMemory(memory.getTotal());
            systemData.setAvailableMemory(memory.getAvailable());
            
            // Température
            try {
                double cpuTemp = hardware.getSensors().getCpuTemperature();
                if (cpuTemp > 0) {
                    systemData.setCpuTemperature(cpuTemp);
                }
            } catch (Exception e) {
                // Ignorer les erreurs de température
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des données système: " + e.getMessage());
        }
    }

    private void addProgressBar(StringBuilder display, double percentage, String label) {
        int barLength = 30;
        int filledLength = (int) (percentage * barLength / 100);
        filledLength = Math.min(filledLength, barLength);
        filledLength = Math.max(filledLength, 0);

        StringBuilder bar = new StringBuilder();
        bar.append("  [");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                if (percentage >= 80) {
                    bar.append("#");
                } else if (percentage >= 50) {
                    bar.append("=");
                } else {
                    bar.append("-");
                }
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        display.append(bar.toString()).append("\n");
    }

    private void addTemperatureBar(StringBuilder display, double temperature) {
        int tempBarLength = 30;
        int tempFilledLength = (int) ((temperature / 100.0) * tempBarLength);
        tempFilledLength = Math.min(tempFilledLength, tempBarLength);
        tempFilledLength = Math.max(tempFilledLength, 0);
        
        StringBuilder tempBar = new StringBuilder();
        tempBar.append("  [");
        for (int j = 0; j < tempBarLength; j++) {
            if (j < tempFilledLength) {
                if (temperature >= 80) {
                    tempBar.append("#"); // Rouge pour température élevée
                } else if (temperature >= 60) {
                    tempBar.append("="); // Orange pour température moyenne
                } else {
                    tempBar.append("-"); // Vert pour température normale
                }
            } else {
                tempBar.append(" ");
            }
        }
        tempBar.append("]");
        display.append(tempBar.toString()).append("\n");
    }

    private void displayFanInfo(StringBuilder display) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-Command",
                "Get-WmiObject -Class Win32_Fan | Select-Object Name, Speed | Format-Table -AutoSize"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            boolean firstLine = true;
            boolean hasFans = false;
            java.util.List<Integer> fanSpeeds = new java.util.ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (firstLine || line.isEmpty() || line.contains("----")) {
                    firstLine = false;
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        String fanName = parts[0];
                        int speed = Integer.parseInt(parts[parts.length - 1]);
                        if (speed > 0) {
                            hasFans = true;
                            fanSpeeds.add(speed);
                            display.append(String.format("%s: %d RPM\n", fanName, speed));
                            
                            // Barre de vitesse du ventilateur
                            int fanBarLength = 30;
                            int fanFilledLength = (int) ((speed / 5000.0) * fanBarLength); // 5000 RPM comme max
                            fanFilledLength = Math.min(fanFilledLength, fanBarLength);
                            fanFilledLength = Math.max(fanFilledLength, 0);
                            
                            StringBuilder fanBar = new StringBuilder();
                            fanBar.append("  [");
                            for (int j = 0; j < fanBarLength; j++) {
                                if (j < fanFilledLength) {
                                    if (speed >= 4000) {
                                        fanBar.append("█"); // Rouge pour vitesse élevée
                                    } else if (speed >= 2000) {
                                        fanBar.append("▓"); // Orange pour vitesse moyenne
                                    } else {
                                        fanBar.append("░"); // Vert pour vitesse normale
                                    }
                                } else {
                                    fanBar.append(" ");
                                }
                            }
                            fanBar.append("]");
                            display.append(fanBar.toString()).append("\n");
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer les lignes qui ne sont pas des nombres
                    }
                }
            }
            
            // Mettre à jour SystemData avec les vitesses des ventilateurs
            if (!fanSpeeds.isEmpty()) {
                systemData.setFanSpeeds(fanSpeeds.stream().mapToInt(Integer::intValue).toArray());
            }
            
            if (!hasFans) {
                display.append("Aucun ventilateur détecté\n");
            }
            
            process.waitFor();
        } catch (Exception e) {
            display.append("Aucun ventilateur détecté\n");
        }
    }
}
