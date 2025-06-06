package com.pcpeek.src;

import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Moniteur système d'exploitation qui hérite de Monitor pour gérer les informations sur l'OS
 */
public class OSMonitor extends Monitor {
    private static final String[] WMIC_COMMANDS = {
        "wmic os get freephysicalmemory,totalvisiblememorysize",
        "wmic cpu get loadpercentage",
        "wmic path win32_operatingsystem get lastbootuptime",
        "wmic path win32_operatingsystem get systemuptime"
    };

    @Override
    protected Map<String, Object> initializeSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        if (!isCompatibleOS()) {
            info.put("error", "Système d'exploitation non supporté");
            return info;
        }

        try {
            for (String command : WMIC_COMMANDS) {
                Process process = Runtime.getRuntime().exec(command);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || line.contains("FreePhysicalMemory") || 
                            line.contains("TotalVisibleMemorySize") || line.contains("LoadPercentage") ||
                            line.contains("LastBootUpTime") || line.contains("SystemUpTime")) continue;
                        processOSInfo(line.trim(), info);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des informations OS: " + e.getMessage());
            info.put("error", "Erreur lors de la récupération des informations OS");
        }
        return info;
    }

    @Override
    protected void performUpdate() {
        // Les informations OS sont statiques, pas besoin de mise à jour
    }

    @Override
    protected void displayContent() {
        if (systemInfo.containsKey("error")) {
            System.out.println("Erreur : " + systemInfo.get("error"));
            return;
        }

        // Effacer l'écran pour un affichage plus propre
        System.out.print("\033[H\033[2J");
        System.out.flush();

        // En-tête avec le nom du processeur
        if (systemInfo.containsKey("cpu.name")) {
            System.out.println("\n" + systemInfo.get("cpu.name"));
            System.out.println("=".repeat(50));
        }

        // Affichage de la charge CPU globale
        displayCPUUsage();
        
        // Affichage des fréquences
        displayFrequencies();
        
        // Affichage des cœurs
        displayCores();
        
        // Affichage de la température
        displayTemperature();
    }

    @Override
    protected String getMonitorName() {
        return "Moniteur OS";
    }

    private void displayMemoryUsage() {
        System.out.println("\nUtilisation de la mémoire :");
        System.out.println("-------------------------");
        
        long totalMemory = (Long) systemInfo.getOrDefault("memory.total", 0L);
        long freeMemory = (Long) systemInfo.getOrDefault("memory.free", 0L);
        long usedMemory = totalMemory - freeMemory;
        
        if (totalMemory > 0) {
            double memoryUsage = (double) usedMemory / totalMemory;
            System.out.printf("Mémoire totale : %s%n", formatSize(totalMemory));
            System.out.printf("Mémoire utilisée : %s%n", formatSize(usedMemory));
            System.out.printf("Mémoire libre : %s%n", formatSize(freeMemory));
            System.out.printf("Utilisation : %s %s%n", 
                formatPercentage(memoryUsage),
                createProgressBar(memoryUsage, 20));
        } else {
            System.out.println("Informations mémoire non disponibles");
        }
    }

    private void displayCPUUsage() {
        int cpuLoad = (Integer) systemInfo.getOrDefault("cpu.load", 0);
        double cpuUsage = cpuLoad / 100.0;
        
        System.out.printf("\nCharge CPU globale : %d%% %s%n", 
            cpuLoad,
            createProgressBar(cpuUsage, 30));
    }

    private void displayFrequencies() {
        System.out.println("\nFréquences :");
        System.out.println("------------");
        if (systemInfo.containsKey("cpu.current.speed")) {
            System.out.printf("Actuelle : %s MHz%n", systemInfo.get("cpu.current.speed"));
        }
        if (systemInfo.containsKey("cpu.max.speed")) {
            System.out.printf("Maximale : %s MHz%n", systemInfo.get("cpu.max.speed"));
        }
    }

    private void displayCores() {
        System.out.println("\nCœurs :");
        System.out.println("-------");
        if (systemInfo.containsKey("cpu.cores")) {
            System.out.printf("Physiques : %s%n", systemInfo.get("cpu.cores"));
        }
        if (systemInfo.containsKey("cpu.threads")) {
            System.out.printf("Logiques  : %s%n", systemInfo.get("cpu.threads"));
        }
    }

    private void displayTemperature() {
        if (systemInfo.containsKey("cpu.temperature")) {
            double temp = (Double) systemInfo.get("cpu.temperature");
            String tempLevel = temp > 80 ? "CRITIQUE" : temp > 70 ? "ÉLEVÉE" : "NORMALE";
            String color = temp > 80 ? "\033[31m" : temp > 70 ? "\033[33m" : "\033[32m";
            System.out.printf("\nTempérature CPU : %.1f°C %s%s\033[0m%n", 
                temp, 
                color,
                createProgressBar(temp/100.0, 20));
            System.out.printf("Niveau : %s%s\033[0m%n", color, tempLevel);
        }
    }

    private void processOSInfo(String line, Map<String, Object> info) {
        if (line.contains("FreePhysicalMemory")) {
            info.put("memory.free", Long.parseLong(line.split("FreePhysicalMemory")[1].trim()) * 1024L);
        } else if (line.contains("TotalVisibleMemorySize")) {
            info.put("memory.total", Long.parseLong(line.split("TotalVisibleMemorySize")[1].trim()) * 1024L);
        } else if (line.contains("LoadPercentage")) {
            info.put("cpu.load", Integer.parseInt(line.split("LoadPercentage")[1].trim()));
        } else if (line.contains("LastBootUpTime")) {
            info.put("os.boottime", formatDateTime(line.split("LastBootUpTime")[1].trim()));
        } else if (line.contains("SystemUpTime")) {
            info.put("os.uptime", formatUptime(Long.parseLong(line.split("SystemUpTime")[1].trim())));
        }
    }

    private String formatDateTime(String wmiDateTime) {
        try {
            // Format WMI: YYYYMMDDHHMMSS.mmmmmm+UUU
            String dateStr = wmiDateTime.substring(0, 8);
            String timeStr = wmiDateTime.substring(8, 14);
            return String.format("%s-%s-%s %s:%s:%s",
                dateStr.substring(0, 4),
                dateStr.substring(4, 6),
                dateStr.substring(6, 8),
                timeStr.substring(0, 2),
                timeStr.substring(2, 4),
                timeStr.substring(4, 6));
        } catch (Exception e) {
            return wmiDateTime;
        }
    }

    private String formatUptime(long seconds) {
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        
        StringBuilder uptime = new StringBuilder();
        if (days > 0) uptime.append(days).append(" jour(s) ");
        if (hours > 0) uptime.append(hours).append(" heure(s) ");
        if (minutes > 0) uptime.append(minutes).append(" minute(s) ");
        if (seconds > 0 || uptime.length() == 0) uptime.append(seconds).append(" seconde(s)");
        
        return uptime.toString().trim();
    }

    public boolean hasInfo() {
        return systemInfo != null && !systemInfo.isEmpty();
    }
} 