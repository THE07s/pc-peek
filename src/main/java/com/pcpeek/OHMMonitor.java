package com.pcpeek;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import java.lang.reflect.Method;

public class OHMMonitor {
    private SystemInfo systemInfo;
    private HardwareAbstractionLayer hardware;
    private CentralProcessor processor;
    private GlobalMemory memory;
    private Sensors sensors;
    private Object ohmHardware;
    private static final String OHM_SENSOR_CLASS = "OpenHardwareMonitorLib.Hardware";

    public OHMMonitor() {
        try {
            systemInfo = new SystemInfo();
            hardware = systemInfo.getHardware();
            processor = hardware.getProcessor();
            memory = hardware.getMemory();
            sensors = hardware.getSensors();
            
            // Initialiser les capteurs OHM
            initializeOHMSensors();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation d'OHMMonitor: " + e.getMessage());
        }
    }

    private void initializeOHMSensors() {
        try {
            // Se connecter à OpenHardwareMonitor via COM
            Class<?> ohmClass = Class.forName(OHM_SENSOR_CLASS);
            ohmHardware = ohmClass.newInstance();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des capteurs OHM: " + e.getMessage());
        }
    }

    public void updateSensors() {
        // Rien à faire ici, OSHI met à jour automatiquement
    }

    public void display() {
        if (!connect()) {
            System.out.println("Erreur de connexion aux capteurs système");
            return;
        }
        try {
            System.out.println("\n=== Capteurs Système ===");
            System.out.println("\nCPU:");
            System.out.println("  Modèle: " + processor.getProcessorIdentifier().getName());
            System.out.println("  Température CPU: " + String.format("%.1f°C", getCpuTemperature()));
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage des capteurs: " + e.getMessage());
        }
    }

    public boolean connect() {
        return systemInfo != null && hardware != null && processor != null && sensors != null;
    }

    public double getCpuTemperature() {
        if (!connect()) return -1;
        try {
            return sensors.getCpuTemperature();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture de la température CPU: " + e.getMessage());
            return -1;
        }
    }

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
     * Récupère la charge CPU par cœur
     * @return Un tableau contenant la charge de chaque cœur (0.0 à 1.0)
     */
    public double[] getCpuLoadPerCore() {
        try {
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            Thread.sleep(1000); // Attendre 1 seconde pour avoir une mesure
            long[] currTicks = processor.getSystemCpuLoadTicks();
            
            if (prevTicks != null && currTicks != null) {
                double[] loadPerCore = new double[processor.getLogicalProcessorCount()];
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
                
                return loadPerCore;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la charge CPU: " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère la mémoire totale du système
     * @return La mémoire totale en octets
     */
    public long getTotalMemory() {
        return memory.getTotal();
    }

    /**
     * Récupère la mémoire disponible du système
     * @return La mémoire disponible en octets
     */
    public long getAvailableMemory() {
        return memory.getAvailable();
    }

    /**
     * Récupère les vitesses des ventilateurs
     * @return Un tableau contenant les vitesses en RPM, ou null si non disponible
     */
    public int[] getFanSpeeds() {
        return sensors.getFanSpeeds();
    }

    /**
     * Récupère la température du GPU
     * @return La température en degrés Celsius, ou -1 si non disponible
     */
    public double getGpuTemperature() {
        try {
            if (ohmHardware != null) {
                Method getHardware = ohmHardware.getClass().getMethod("GetHardware");
                Object[] hardwareList = (Object[]) getHardware.invoke(ohmHardware);
                
                for (Object hw : hardwareList) {
                    Method getHwName = hw.getClass().getMethod("GetName");
                    String name = (String) getHwName.invoke(hw);
                    if (name.contains("GPU")) {
                        Method getSensors = hw.getClass().getMethod("GetSensors");
                        Object[] sensors = (Object[]) getSensors.invoke(hw);
                        
                        for (Object sensor : sensors) {
                            Method getSensorType = sensor.getClass().getMethod("GetSensorType");
                            int type = (int) getSensorType.invoke(sensor);
                            Method getSensorName = sensor.getClass().getMethod("GetName");
                            String sensorName = (String) getSensorName.invoke(sensor);
                            
                            if (type == 2 && sensorName.contains("Temperature")) { // Type 2 = Temperature
                                Method getValue = sensor.getClass().getMethod("GetValue");
                                Double value = (Double) getValue.invoke(sensor);
                                if (value != null) {
                                    return value;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs silencieusement
        }
        return -1;
    }

    /**
     * Récupère la charge du GPU
     * @return La charge en pourcentage (0.0 à 100.0), ou -1 si non disponible
     */
    public double getGpuLoad() {
        try {
            if (ohmHardware != null) {
                Method getHardware = ohmHardware.getClass().getMethod("GetHardware");
                Object[] hardwareList = (Object[]) getHardware.invoke(ohmHardware);
                
                for (Object hw : hardwareList) {
                    Method getHwName = hw.getClass().getMethod("GetName");
                    String name = (String) getHwName.invoke(hw);
                    if (name.contains("GPU")) {
                        Method getSensors = hw.getClass().getMethod("GetSensors");
                        Object[] sensors = (Object[]) getSensors.invoke(hw);
                        
                        for (Object sensor : sensors) {
                            Method getSensorType = sensor.getClass().getMethod("GetSensorType");
                            int type = (int) getSensorType.invoke(sensor);
                            Method getSensorName = sensor.getClass().getMethod("GetName");
                            String sensorName = (String) getSensorName.invoke(sensor);
                            
                            if (type == 3 && sensorName.contains("Load")) { // Type 3 = Load
                                Method getValue = sensor.getClass().getMethod("GetValue");
                                Double value = (Double) getValue.invoke(sensor);
                                if (value != null) {
                                    return value;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs silencieusement
        }
        return -1;
    }
} 