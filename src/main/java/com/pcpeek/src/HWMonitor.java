package com.pcpeek.src;// src/HWMonitor.java
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.ComputerSystem;

public class HWMonitor {
    private final HardwareAbstractionLayer hal;
    private final SystemInfo si;
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final ComputerSystem computerSystem;
    private long[] prevTicks;

    public HWMonitor(SystemInfo si) {
        this.si = si;
        this.hal = si.getHardware();
        this.processor = hal.getProcessor();
        this.memory = hal.getMemory();
        this.computerSystem = hal.getComputerSystem();
        this.prevTicks = processor.getSystemCpuLoadTicks();
    }

    public double getCpuTemperature() {
        Sensors sensors = hal.getSensors();
        return sensors.getCpuTemperature();
    }

    public int[] getFanSpeeds() {
        Sensors sensors = hal.getSensors();
        return sensors.getFanSpeeds();
    }

    public double getCpuVoltage() {
        Sensors sensors = hal.getSensors();
        return sensors.getCpuVoltage();
    }

    public double[] getCpuLoadPerCore() {
        double[] load = processor.getProcessorCpuLoad(1000);
        for (int i = 0; i < load.length; i++) {
            load[i] *= 100.0;
        }
        return load;
    }

    public double[] getCpuFrequencyPerCore() {
        long[] frequencies = processor.getCurrentFreq();
        double[] freqInMHz = new double[frequencies.length];
        for (int i = 0; i < frequencies.length; i++) {
            freqInMHz[i] = frequencies[i] / 1_000_000.0;
        }
        return freqInMHz;
    }

    public double getCpuLoad() {
        long[] ticks = processor.getSystemCpuLoadTicks();
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;
        prevTicks = ticks;
        return totalCpu > 0 ? (double) (totalCpu - idle) / totalCpu * 100.0 : 0.0;
    }

    public double getCpuFrequency() {
        return processor.getProcessorIdentifier().getVendorFreq() / 1_000_000.0;
    }

    public String getCpuName() {
        return processor.getProcessorIdentifier().getName();
    }

    public long getTotalMemory() {
        return memory.getTotal();
    }

    public long getAvailableMemory() {
        return memory.getAvailable();
    }

    public double getMemoryUsage() {
        return (memory.getTotal() - memory.getAvailable()) * 100.0 / memory.getTotal();
    }

    public void printDetailedInfo() {
        System.out.println("\n=== Informations Détaillées du Système ===");

        // Informations CPU générales
        System.out.println("\n--- CPU ---");
        System.out.printf("Modèle: %s%n", getCpuName());
        System.out.printf("Fréquence de base: %.2f MHz%n", getCpuFrequency());
        System.out.printf("Charge globale: %.1f%%%n", getCpuLoad());
        System.out.printf("Température: %.1f°C%n", getCpuTemperature());
        System.out.printf("Tension: %.2f V%n", getCpuVoltage());

        // Informations détaillées par cœur
        System.out.println("\n--- Détails par Cœur ---");
        double[] loadPerCore = getCpuLoadPerCore();
        double[] freqPerCore = getCpuFrequencyPerCore();

        for (int i = 0; i < loadPerCore.length; i++) {
            System.out.printf("Cœur %d:%n", i + 1);
            System.out.printf("  Fréquence: %.2f MHz%n", freqPerCore[i]);
            System.out.printf("  Charge: %.1f%%%n", loadPerCore[i]);
        }

        // Informations Mémoire
        System.out.println("\n--- Mémoire ---");
        System.out.printf("Mémoire Totale: %.2f GB%n", getTotalMemory() / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Mémoire Disponible: %.2f GB%n", getAvailableMemory() / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Utilisation: %.1f%%%n", getMemoryUsage());

        // Informations Ventilateurs
        System.out.println("\n--- Ventilateurs ---");
        int[] fanSpeeds = getFanSpeeds();
        if (fanSpeeds.length == 0) {
            System.out.println("Vitesse ventilateurs: Non disponible.");
        } else {
            for (int i = 0; i < fanSpeeds.length; i++) {
                System.out.printf("Ventilateur %d: %d RPM%n", i + 1, fanSpeeds[i]);
            }
        }
    }
}