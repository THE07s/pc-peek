package com.pcpeek;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.software.os.OperatingSystem;
import java.lang.reflect.Method;
import java.util.Arrays;

public class OHMMonitor {
    private SystemInfo systemInfo;
    private HardwareAbstractionLayer hardware;
    private CentralProcessor processor;
    private GlobalMemory memory;
    private Sensors sensors;
    private long[] prevTicks;
    private long[] currTicks;
    private long[] prevFreq;
    private long[] currFreq;
    private static final String OHM_SENSOR_CLASS = "OpenHardwareMonitorLib.Hardware";
    private static final String OHM_CPU_CLOCK_SENSOR = "CPU Core #";
    private Object ohmHardware;
    private Object[] cpuSensors;
    private double[] maxFrequencies;  // Pour stocker les fréquences maximales

    public OHMMonitor() {
        try {
            systemInfo = new SystemInfo();
            hardware = systemInfo.getHardware();
            processor = hardware.getProcessor();
            memory = hardware.getMemory();
            sensors = hardware.getSensors();
            prevTicks = processor.getSystemCpuLoadTicks();
            currTicks = new long[prevTicks.length];
            prevFreq = new long[processor.getLogicalProcessorCount()];
            currFreq = new long[processor.getLogicalProcessorCount()];
            
            // Initialiser les capteurs OHM
            initializeOHMSensors();
            
            // Initialiser le tableau des fréquences maximales
            int numCores = processor.getLogicalProcessorCount();
            maxFrequencies = new double[numCores];
            // Obtenir la fréquence maximale du processeur
            long maxFreq = processor.getProcessorIdentifier().getVendorFreq();
            if (maxFreq > 0) {
                for (int i = 0; i < numCores; i++) {
                    maxFrequencies[i] = maxFreq / 1_000_000.0; // Convertir en MHz
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation d'OHMMonitor: " + e.getMessage());
        }
    }

    private void initializeOHMSensors() {
        try {
            // Se connecter à OpenHardwareMonitor via COM
            Class<?> ohmClass = Class.forName(OHM_SENSOR_CLASS);
            ohmHardware = ohmClass.newInstance();
            
            // Obtenir les capteurs CPU
            Method getHardware = ohmClass.getMethod("GetHardware");
            Object[] hardwareList = (Object[]) getHardware.invoke(ohmHardware);
            
            // Chercher les capteurs de fréquence CPU
            for (Object hw : hardwareList) {
                Method getHwName = hw.getClass().getMethod("GetName");
                String name = (String) getHwName.invoke(hw);
                if (name.contains("CPU")) {
                    Method getSensors = hw.getClass().getMethod("GetSensors");
                    Object[] sensors = (Object[]) getSensors.invoke(hw);
                    
                    // Filtrer les capteurs de fréquence CPU
                    cpuSensors = Arrays.stream(sensors)
                        .filter(sensor -> {
                            try {
                                Method getSensorType = sensor.getClass().getMethod("GetSensorType");
                                int type = (int) getSensorType.invoke(sensor);
                                Method getSensorName = sensor.getClass().getMethod("GetName");
                                String sensorName = (String) getSensorName.invoke(sensor);
                                // Accepter à la fois les capteurs "Core" et "Bus Speed"
                                return (type == 7 && (sensorName.contains("Core") || sensorName.contains("Bus Speed")));
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .toArray();
                    
                    // Mettre à jour les fréquences maximales si disponibles
                    for (Object sensor : sensors) {
                        try {
                            Method getSensorType = sensor.getClass().getMethod("GetSensorType");
                            Method getSensorName = sensor.getClass().getMethod("GetName");
                            String sensorName = (String) getSensorName.invoke(sensor);
                            if (sensorName.contains("Max")) {
                                Method getValue = sensor.getClass().getMethod("GetValue");
                                Double value = (Double) getValue.invoke(sensor);
                                if (value != null && value > 0) {
                                    // Mettre à jour la fréquence maximale pour tous les cœurs
                                    double maxFreq = value * 1000; // Convertir GHz en MHz
                                    for (int i = 0; i < maxFrequencies.length; i++) {
                                        maxFrequencies[i] = Math.max(maxFrequencies[i], maxFreq);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignorer les erreurs sur les capteurs individuels
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des capteurs OHM: " + e.getMessage());
        }
    }

    public boolean connect() {
        return systemInfo != null && hardware != null && processor != null && memory != null && sensors != null;
    }

    public void updateSensors() {
        if (!connect()) return;

        try {
            // Mettre à jour les ticks CPU pour le calcul de la charge
            prevTicks = currTicks;
            currTicks = processor.getSystemCpuLoadTicks();

            // Mettre à jour les fréquences
            prevFreq = currFreq;
            currFreq = processor.getCurrentFreq();
            
            // Si les fréquences ne sont pas disponibles, essayer d'obtenir la fréquence de base
            if (currFreq == null || currFreq.length == 0 || currFreq[0] == 0) {
                long baseFreq = processor.getProcessorIdentifier().getVendorFreq();
                if (baseFreq > 0) {
                    currFreq = new long[processor.getLogicalProcessorCount()];
                    for (int i = 0; i < currFreq.length; i++) {
                        currFreq[i] = baseFreq;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des capteurs: " + e.getMessage());
        }
    }

    public void display() {
        if (!connect()) {
            System.out.println("Erreur de connexion aux capteurs système");
            return;
        }

        try {
            System.out.println("\n=== Capteurs Système ===");
            
            // CPU
            System.out.println("\nCPU:");
            
            // Informations détaillées par cœur
            int logicalProcessorCount = processor.getLogicalProcessorCount();
            System.out.printf("  Nombre de cœurs logiques: %d%n", logicalProcessorCount);
            
            // Fréquences par cœur
            System.out.println("\n  Fréquences par cœur:");
            if (currFreq != null && currFreq.length > 0) {
                for (int i = 0; i < currFreq.length; i++) {
                    if (currFreq[i] > 0) {
                        System.out.printf("    Cœur %d: %.2f GHz%n", i, currFreq[i] / 1_000_000_000.0);
                    }
                }
            } else {
                // Si les fréquences ne sont pas disponibles, afficher la fréquence de base
                long baseFreq = processor.getProcessorIdentifier().getVendorFreq();
                if (baseFreq > 0) {
                    System.out.printf("    Fréquence de base: %.2f GHz%n", baseFreq / 1_000_000_000.0);
                } else {
                    System.out.println("    Fréquence: Non disponible");
                }
            }

            // Afficher les informations du processeur
            System.out.println("\n  Informations processeur:");
            System.out.println("    Fabricant: " + processor.getProcessorIdentifier().getVendor());
            System.out.println("    Modèle: " + processor.getProcessorIdentifier().getName());
            System.out.println("    Architecture: " + processor.getProcessorIdentifier().getMicroarchitecture());
            System.out.println("    Famille: " + processor.getProcessorIdentifier().getFamily());
            System.out.println("    Modèle: " + processor.getProcessorIdentifier().getModel());
            System.out.println("    Stepping: " + processor.getProcessorIdentifier().getStepping());
            
            // Charge par cœur
            double[] loadPerCore = new double[processor.getLogicalProcessorCount()];
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            try {
                Thread.sleep(1000); // Attendre 1 seconde pour avoir une mesure
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long[] currTicks = processor.getSystemCpuLoadTicks();
            
            if (prevTicks != null && currTicks != null) {
                // Calculer la charge CPU globale
                long totalTicks = 0;
                long idleTicks = currTicks[oshi.hardware.CentralProcessor.TickType.IDLE.getIndex()] - 
                                prevTicks[oshi.hardware.CentralProcessor.TickType.IDLE.getIndex()];
                
                for (int i = 0; i < currTicks.length; i++) {
                    totalTicks += currTicks[i] - prevTicks[i];
                }
                
                double systemLoad = totalTicks > 0 ? 1.0 - ((double) idleTicks / totalTicks) : 0;
                
                // Répartir la charge système sur tous les cœurs
                for (int i = 0; i < loadPerCore.length; i++) {
                    loadPerCore[i] = systemLoad;
                }
                
                System.out.println("\n  Charge par cœur:");
                for (int i = 0; i < loadPerCore.length; i++) {
                    if (loadPerCore[i] >= 0) {
                        // Créer une barre de progression visuelle
                        int barLength = 20;
                        int filledLength = (int) (loadPerCore[i] * barLength);
                        StringBuilder bar = new StringBuilder();
                        bar.append("[");
                        for (int j = 0; j < barLength; j++) {
                            if (j < filledLength) {
                                if (loadPerCore[i] >= 0.8) {
                                    bar.append("█");
                                } else if (loadPerCore[i] >= 0.5) {
                                    bar.append("▓");
                                } else {
                                    bar.append("░");
                                }
                            } else {
                                bar.append(" ");
                            }
                        }
                        bar.append("]");
                        System.out.printf("    Cœur %d: %s %.1f%%%n", i, bar.toString(), loadPerCore[i] * 100);
                    }
                }
            }
            
            // Température CPU (globale et par cœur si disponible)
            double cpuTemp = sensors.getCpuTemperature();
            if (cpuTemp > 0) {
                System.out.println("\n  Températures:");
                System.out.printf("    CPU (global): %.1f°C%n", cpuTemp);
                
                // Afficher une barre de température
                int barLength = 20;
                int filledLength = (int) ((cpuTemp / 100.0) * barLength);
                filledLength = Math.min(filledLength, barLength);
                filledLength = Math.max(filledLength, 0);
                
                StringBuilder tempBar = new StringBuilder();
                tempBar.append("[");
                for (int i = 0; i < barLength; i++) {
                    if (i < filledLength) {
                        if (cpuTemp >= 80) {
                            tempBar.append("█");
                        } else if (cpuTemp >= 60) {
                            tempBar.append("▓");
                        } else {
                            tempBar.append("░");
                        }
                    } else {
                        tempBar.append(" ");
                    }
                }
                tempBar.append("]");
                System.out.printf("    %s%n", tempBar.toString());
                
                // Ajouter des indicateurs de température
                if (cpuTemp >= 80) {
                    System.out.println("    ⚠️  ATTENTION: Température élevée!");
                } else if (cpuTemp >= 60) {
                    System.out.println("    ℹ️  Température normale sous charge");
                } else {
                    System.out.println("    ✓ Température normale");
                }
            }
            
            // Charge CPU globale
            if (prevTicks != null && currTicks != null) {
                long totalTicks = 0;
                long idleTicks = currTicks[oshi.hardware.CentralProcessor.TickType.IDLE.getIndex()] - 
                                prevTicks[oshi.hardware.CentralProcessor.TickType.IDLE.getIndex()];
                
                for (int i = 0; i < currTicks.length; i++) {
                    totalTicks += currTicks[i] - prevTicks[i];
                }
                
                double cpuLoad = totalTicks > 0 ? 1.0 - ((double) idleTicks / totalTicks) : 0;
                
                if (cpuLoad >= 0) {
                    System.out.println("\n  Charge CPU globale:");
                    int barLength = 20;
                    int filledLength = (int) (cpuLoad * barLength);
                    StringBuilder bar = new StringBuilder();
                    bar.append("[");
                    for (int i = 0; i < barLength; i++) {
                        if (i < filledLength) {
                            if (cpuLoad >= 0.8) {
                                bar.append("█");
                            } else if (cpuLoad >= 0.5) {
                                bar.append("▓");
                            } else {
                                bar.append("░");
                            }
                        } else {
                            bar.append(" ");
                        }
                    }
                    bar.append("]");
                    System.out.printf("    %s %.1f%%%n", bar.toString(), cpuLoad * 100);
                }
            }

            // Mémoire
            System.out.println("\nMémoire:");
            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;
            double memoryUsagePercent = (usedMemory * 100.0) / totalMemory;
            
            System.out.printf("  Totale: %.1f GB%n", totalMemory / (1024.0 * 1024.0 * 1024.0));
            System.out.printf("  Utilisée: %.1f GB (%.1f%%)%n", 
                usedMemory / (1024.0 * 1024.0 * 1024.0), memoryUsagePercent);
            System.out.printf("  Disponible: %.1f GB%n", 
                availableMemory / (1024.0 * 1024.0 * 1024.0));

            // Températures système
            System.out.println("\nTempératures Système:");
            double systemTemp = sensors.getCpuTemperature();
            if (systemTemp > 0) {
                System.out.printf("  CPU: %.1f°C%n", systemTemp);
            }
            
            // Ventilateurs
            int[] fanSpeeds = sensors.getFanSpeeds();
            if (fanSpeeds != null && fanSpeeds.length > 0) {
                System.out.println("\nVentilateurs:");
                for (int i = 0; i < fanSpeeds.length; i++) {
                    if (fanSpeeds[i] > 0) {
                        System.out.printf("  Ventilateur %d: %d RPM%n", i, fanSpeeds[i]);
                    }
                }
            }

            // GPU (via OSHI, informations limitées)
            System.out.println("\nGPU (informations limitées):");
            System.out.println("  Note: Pour des informations GPU détaillées, utilisez OpenHardwareMonitor");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage des capteurs: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connect();
    }

    /**
     * Récupère la température actuelle du CPU
     * @return La température en degrés Celsius, ou -1 si non disponible
     */
    public double getCpuTemperature() {
        if (!connect()) return -1;
        try {
            return sensors.getCpuTemperature();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture de la température CPU: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Récupère le nom du processeur
     * @return Le nom du processeur, ou "Inconnu" si non disponible
     */
    public String getProcessorName() {
        if (!connect()) return "Inconnu";
        try {
            return processor.getProcessorIdentifier().getName();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du nom du processeur: " + e.getMessage());
            return "Inconnu";
        }
    }

    /**
     * Récupère les fréquences actuelles de tous les cœurs CPU
     * @return Un tableau des fréquences en MHz, ou null si non disponible
     */
    public double[] getCpuFrequencies() {
        if (!connect()) return null;
        try {
            // Essayer d'abord d'obtenir les fréquences via OpenHardwareMonitor
            if (cpuSensors != null && cpuSensors.length > 0) {
                double[] frequencies = new double[cpuSensors.length];
                boolean hasValidFrequencies = false;
                
                for (int i = 0; i < cpuSensors.length; i++) {
                    try {
                        Method getValue = cpuSensors[i].getClass().getMethod("GetValue");
                        Double value = (Double) getValue.invoke(cpuSensors[i]);
                        if (value != null && value > 0) {
                            frequencies[i] = value * 1000; // Convertir GHz en MHz
                            // Mettre à jour la fréquence maximale si nécessaire
                            if (frequencies[i] > maxFrequencies[i]) {
                                maxFrequencies[i] = frequencies[i];
                            }
                            hasValidFrequencies = true;
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs sur les capteurs individuels
                    }
                }
                
                if (hasValidFrequencies) {
                    return frequencies;
                }
            }
            
            // Fallback sur OSHI si OHM n'est pas disponible
            long[] frequencies = processor.getCurrentFreq();
            if (frequencies == null || frequencies.length == 0) {
                // Si les fréquences ne sont pas disponibles, essayer d'obtenir la fréquence de base
                long baseFreq = processor.getProcessorIdentifier().getVendorFreq();
                if (baseFreq > 0) {
                    frequencies = new long[processor.getLogicalProcessorCount()];
                    for (int i = 0; i < frequencies.length; i++) {
                        frequencies[i] = baseFreq;
                    }
                }
            }
            
            // Convertir en double[] pour plus de précision
            if (frequencies != null) {
                double[] result = new double[frequencies.length];
                for (int i = 0; i < frequencies.length; i++) {
                    result[i] = frequencies[i] / 1_000_000.0; // Convertir en MHz
                    // Mettre à jour la fréquence maximale si nécessaire
                    if (result[i] > maxFrequencies[i]) {
                        maxFrequencies[i] = result[i];
                    }
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des fréquences CPU: " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère les fréquences maximales enregistrées pour tous les cœurs CPU
     * @return Un tableau des fréquences maximales en MHz, ou null si non disponible
     */
    public double[] getMaxFrequencies() {
        return maxFrequencies != null ? maxFrequencies.clone() : null;
    }
} 