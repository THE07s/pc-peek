package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.*;
import java.util.Scanner;

public class TemperatureMode {    private static final double TEMP_CRITICAL = 90.0;
    private static final double TEMP_HIGH = 85.0;
    private static final double VAR_EXTREME = 8.0;
    private static final double VAR_HIGH = 5.0;
    private static final double STD_DEV_CRITICAL = 5.0;
    private static final double STD_DEV_HIGH = 3.0;
    private static final double TREND_CRITICAL = 3.0;
    private static final double TREND_HIGH = 2.0;
    private static final double TREND_LOW = 0.5;
    
    private final SystemData systemData;
    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();
    private final Queue<Double> temperatureHistory = new LinkedList<>();
    private final Queue<Double> loadHistory = new LinkedList<>();
    private final int HISTORY_SIZE = 5;
    private boolean running = true;

    public TemperatureMode(SystemData systemData) {
        this.systemData = systemData;
    }

    public void execute(Scanner scanner) {
        clearScreen();
        showMenu(scanner);
    }

    private void showMenu(Scanner scanner) {
        while (running) {
            clearScreen();
            System.out.println("=== Mode Diagnostic ===");
            System.out.println("1. État actuel");
            System.out.println("2. Analyse détaillée");
            System.out.println("3. Retour au menu principal");
            System.out.print("\nChoix: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> showCurrentState(scanner);
                case "2" -> showDetailedAnalysis(scanner);
                case "3" -> running = false;
            }
        }
    }

    public void updateMetrics() {
        // Mettre à jour les données système
        updateSystemData();
        
        // Récupérer les données depuis SystemData
        double currentTemp = systemData.getCpuTemperature().orElse(0.0);
        double currentLoad = systemData.getCpuLoad().orElse(0.0);

        temperatureHistory.offer(currentTemp);
        loadHistory.offer(currentLoad);

        if (temperatureHistory.size() > HISTORY_SIZE) {
            temperatureHistory.poll();
            loadHistory.poll();
        }
    }

    private void updateSystemData() {
        // Mise à jour de la température CPU
        double cpuTemp = hal.getSensors().getCpuTemperature();
        if (cpuTemp > 0) {
            systemData.setCpuTemperature(cpuTemp);
        }
        
        // Mise à jour de la charge CPU
        double cpuLoad = getCurrentCpuLoad();
        systemData.setCpuLoad(cpuLoad);
    }

    private double getCurrentCpuLoad() {
        CentralProcessor processor = hal.getProcessor();
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

    private void displayMetrics() {
        Double currentTemp = ((LinkedList<Double>) temperatureHistory).peekLast();
        Double currentLoad = ((LinkedList<Double>) loadHistory).peekLast();

        if (currentTemp != null) {
            System.out.printf("\nTempérature CPU: %.1f°C\n", currentTemp);
            System.out.print("État: ");            if (currentTemp >= TEMP_CRITICAL) {
                System.out.println("CRITIQUE");
            } else if (currentTemp >= TEMP_HIGH) {
                System.out.println("ELEVEE");
            } else {
                System.out.println("NORMAL");
            }
        }

        if (currentLoad != null) {
            System.out.printf("\nCharge CPU: %.1f%%\n", currentLoad);
            System.out.print("État: ");            if (currentLoad >= 80) {
                System.out.println("ELEVEE");
            } else {
                System.out.println("NORMAL");
            }
        }
    }

    public void showCurrentState(Scanner scanner) {
        clearScreen();
        System.out.println("=== État Actuel ===");
        updateMetrics();
        displayMetrics();
        waitForEnter(scanner);
    }

    public void showDetailedAnalysis(Scanner scanner) {
        clearScreen();
        System.out.println("=== Analyse Détaillée ===");
        System.out.println("\nCollecte des données en cours...");
        
        for (int i = 0; i < 5; i++) {
            updateMetrics();
            System.out.print(".");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("\n");

        if (temperatureHistory.size() < 2) {
            System.out.println("Erreur lors de la collecte des données. Veuillez réessayer.");
            return;
        }

        List<Double> temps = new ArrayList<>(temperatureHistory);
        double mean = calculateMean(temps);
        double variance = calculateVariance(temps);
        double stdDev = Math.sqrt(variance);
        double currentTemp = temps.get(temps.size() - 1);
        
        System.out.println("=== Situation Actuelle ===");
        System.out.printf("Température actuelle: %.1f°C\n", currentTemp);
        System.out.printf("Température moyenne: %.1f°C\n", mean);
        System.out.printf("Écart type: %.2f°C\n", stdDev);
        
        boolean isWorrisome = false;
        if (currentTemp >= TEMP_CRITICAL) {
            System.out.println("\nTEMPERATURE CRITIQUE!");
            System.out.println("DANGER: Risque de dommages materiels immediats");
            isWorrisome = true;
        } else if (currentTemp >= TEMP_HIGH) {
            System.out.println("\nTEMPERATURE ELEVEE");
            System.out.println("ATTENTION: Risque de surchauffe");
            isWorrisome = true;
        }
        
        double deviationFromMean = currentTemp - mean;
        if (deviationFromMean > VAR_EXTREME) {
            System.out.println("\nTEMPERATURE ANORMALEMENT HAUTE");
            System.out.printf("Ecart par rapport a la moyenne: +%.1f°C\n", deviationFromMean);
            isWorrisome = true;
        }
        
        double maxVariation = calculateMaxVariation(temps);
        if (maxVariation > VAR_EXTREME && currentTemp > mean) {
            System.out.println("\nVARIATIONS EXTREMES");
            System.out.printf("Variation: %.1f°C/seconde\n", maxVariation);
            System.out.println("DANGER: Risque de surchauffe rapide");
            isWorrisome = true;
        } else if (maxVariation > VAR_HIGH && currentTemp > mean) {
            System.out.println("\nVARIATIONS RAPIDES");
            System.out.printf("Variation: %.1f°C/seconde\n", maxVariation);
            isWorrisome = true;
        }
        
        if (stdDev > STD_DEV_CRITICAL && currentTemp > mean) {
            System.out.println("\nINSTABILITE CRITIQUE");
            System.out.println("DANGER: Fluctuations dangereuses vers le haut");
            isWorrisome = true;
        } else if (stdDev > STD_DEV_HIGH && currentTemp > mean) {
            System.out.println("\nINSTABILITE DETECTEE");
            System.out.println("ATTENTION: Fluctuations importantes");
            isWorrisome = true;
        }
        
        System.out.println("\n=== Tendance ===");
        double trend = calculateSimpleTrend(temps);
        if (trend > TREND_CRITICAL) {
            System.out.println("AUGMENTATION TRES RAPIDE");
            isWorrisome = true;
        } else if (trend > TREND_HIGH) {
            System.out.println("AUGMENTATION RAPIDE");
            isWorrisome = true;
        } else if (trend > TREND_LOW) {
            System.out.println("Legere augmentation");
        } else if (trend < -TREND_CRITICAL) {
            System.out.println("Diminution rapide (normal)");
        } else if (trend < -TREND_HIGH) {
            System.out.println("Diminution (normal)");
        } else if (trend < -TREND_LOW) {
            System.out.println("Legere diminution (normal)");
        } else {
            System.out.println("Stable");
        }
        
        if (isWorrisome) {
            System.out.println("\n=== Recommandations ===");
            if (currentTemp >= TEMP_CRITICAL) {
                System.out.println("1. ARRÊTEZ les applications gourmandes");
                System.out.println("2. Vérifiez le refroidissement");
                System.out.println("3. Considérez l'arrêt du système");
            } else if (currentTemp >= TEMP_HIGH || maxVariation > VAR_HIGH) {
                System.out.println("1. Fermez les applications non essentielles");
                System.out.println("2. Vérifiez la ventilation");
                System.out.println("3. Surveillez la température");
            }
        }
        
        waitForEnter(scanner);
    }

    public double calculateMean(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public double calculateVariance(List<Double> values) {
        double mean = calculateMean(values);
        return values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
    }

    private double calculateMaxVariation(List<Double> values) {
        double maxVar = 0.0;
        for (int i = 1; i < values.size(); i++) {
            double var = Math.abs(values.get(i) - values.get(i-1));
            maxVar = Math.max(maxVar, var);
        }
        return maxVar;
    }

    private double calculateSimpleTrend(List<Double> values) {
        if (values.size() < 2) return 0.0;
        return values.get(values.size() - 1) - values.get(0);
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

    private void waitForEnter(Scanner scanner) {
        System.out.println("\nAppuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }
}
