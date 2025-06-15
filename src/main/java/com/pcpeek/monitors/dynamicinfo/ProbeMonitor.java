package com.pcpeek.monitors.dynamicinfo;

import com.pcpeek.monitors.Monitor;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class ProbeMonitor extends Monitor {
    private SystemInfo systemInfo;
    private HardwareAbstractionLayer hardware;
    private CentralProcessor processor;
    private GlobalMemory memory;
    private Sensors sensors;
    private Object ohmHardware;
    private static final String OHM_SENSOR_CLASS = "OpenHardwareMonitorLib.Hardware";

    public ProbeMonitor() {
        super();
        try {
            systemInfo = new SystemInfo();
            hardware = systemInfo.getHardware();
            processor = hardware.getProcessor();
            memory = hardware.getMemory();
            sensors = hardware.getSensors();

            initializeOHMSensors();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de ProbeMonitor: " + e.getMessage());
        }
    }

    private void initializeOHMSensors() {
        try {
            Class<?> ohmClass = Class.forName(OHM_SENSOR_CLASS);
            ohmHardware = ohmClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des capteurs OHM: " + e.getMessage());
        }
    }

    public Map<String, Object> getProbeInfo() {
        Map<String, Object> probeInfo = new HashMap<>();

        try {
            probeInfo.put("cpu_temperature", getCpuTemperature());
            probeInfo.put("gpu_temperature", getGpuTemperature());

            double[] cpuLoads = getCpuLoadPerCore();
            if (cpuLoads != null) {
                probeInfo.put("cpu_loads_per_core", cpuLoads);
                double avgLoad = 0;
                for (double load : cpuLoads) {
                    avgLoad += load;
                }
                probeInfo.put("cpu_load_avg", avgLoad / cpuLoads.length);
            }
            probeInfo.put("gpu_load", getGpuLoad());

            probeInfo.put("total_memory", getTotalMemory());
            probeInfo.put("available_memory", getAvailableMemory());

            int[] fanSpeeds = getFanSpeeds();
            if (fanSpeeds != null) {
                probeInfo.put("fan_speeds", fanSpeeds);
            }

            probeInfo.put("processor_name", getProcessorName());

        } catch (Exception e) {
            System.err.println("Erreur lors de la collecte des sondes: " + e.getMessage());
            probeInfo.put("error", "Erreur lors de la collecte des données");
        }

        return probeInfo;
    }

    public void updateSensors() {}

    public void displayProbeInfo(Map<String, Object> probeInfo) {
        if (probeInfo.containsKey("error")) {
            System.out.println("Erreur : " + probeInfo.get("error"));
            return;
        }

        try {
            System.out.println("\n=== Capteurs Système ===");
            // CPU
            System.out.println("\nCPU:");
            if (probeInfo.containsKey("processor_name")) {
                System.out.println("  Modèle: " + probeInfo.get("processor_name"));
            }
            if (probeInfo.containsKey("cpu_temperature")) {
                double temp = (Double) probeInfo.get("cpu_temperature");
                if (temp > 0) {
                    System.out.println("  Température CPU: " + String.format("%.1f°C", temp));
                }
            }
            if (probeInfo.containsKey("cpu_load_avg")) {
                double load = (Double) probeInfo.get("cpu_load_avg");
                System.out.println("  Charge moyenne CPU: " + String.format("%.1f%%", load * 100));
            }

            // GPU
            System.out.println("\nGPU:");
            if (probeInfo.containsKey("gpu_temperature")) {
                double gpuTemp = (Double) probeInfo.get("gpu_temperature");
                if (gpuTemp > 0) {
                    System.out.println("  Température GPU: " + String.format("%.1f°C", gpuTemp));
                }
            }
            if (probeInfo.containsKey("gpu_load")) {
                double gpuLoad = (Double) probeInfo.get("gpu_load");
                if (gpuLoad > 0) {
                    System.out.println("  Charge GPU: " + String.format("%.1f%%", gpuLoad));
                }
            }

            // Mémoire
            System.out.println("\nMémoire:");
            if (probeInfo.containsKey("total_memory") && probeInfo.containsKey("available_memory")) {
                long total = (Long) probeInfo.get("total_memory");
                long available = (Long) probeInfo.get("available_memory");
                long used = total - available;
                double usage = (double) used / total * 100;

                System.out.println("  Totale: " + formatSize(total));
                System.out.println("  Utilisée: " + formatSize(used) + String.format(" (%.1f%%)", usage));
                System.out.println("  Disponible: " + formatSize(available));
            }

            // Ventilateurs
            if (probeInfo.containsKey("fan_speeds")) {
                int[] fanSpeeds = (int[]) probeInfo.get("fan_speeds");
                if (fanSpeeds != null && fanSpeeds.length > 0) {
                    System.out.println("\nVentilateurs:");
                    for (int i = 0; i < fanSpeeds.length; i++) {
                        if (fanSpeeds[i] > 0) {
                            System.out.println("  Ventilateur " + (i + 1) + ": " + fanSpeeds[i] + " RPM");
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage des capteurs: " + e.getMessage());
        }
    }

    public boolean connect() {
        return systemInfo != null && hardware != null && processor != null && sensors != null;
    }

    public double getCpuTemperature() {
        if (!connect())
            return -1;
        try {
            return sensors.getCpuTemperature();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture de la température CPU: " + e.getMessage());
            return -1;
        }
    }

    public String getProcessorName() {
        if (!connect())
            return "Inconnu";
        try {
            return processor.getProcessorIdentifier().getName();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du nom du processeur: " + e.getMessage());
            return "Inconnu";
        }
    }

    public double[] getCpuLoadPerCore() {
        try {
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            Thread.sleep(1000);
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

    public long getTotalMemory() {
        return memory.getTotal();
    }

    public long getAvailableMemory() {
        return memory.getAvailable();
    }

    public int[] getFanSpeeds() {
        return sensors.getFanSpeeds();
    }

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
        }
        return -1;
    }

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
        }
        return -1;
    }

    @Override
    protected Map<String, Object> initializeSystemInfo() {
        return getProbeInfo();
    }

    @Override
    protected void performUpdate() {
        updateSensors();
    }

    @Override
    protected void displayContent() {
        displayProbeInfo(getProbeInfo());
    }

    @Override
    protected String getMonitorName() {
        return "Moniteur de Sondes Système";
    }

}
