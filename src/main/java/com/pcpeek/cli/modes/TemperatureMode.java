package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import com.pcpeek.monitors.dynamicinfo.ProbeMonitor;
import java.util.*;

public class TemperatureMode {
    // Constantes pour l'analyse avancée des températures
    private static final double TEMP_CRITICAL = 90.0;
    private static final double TEMP_HIGH = 85.0;
    private static final double VAR_EXTREME = 8.0;
    private static final double VAR_HIGH = 5.0;
    private static final double STD_DEV_CRITICAL = 5.0;
    private static final double STD_DEV_HIGH = 3.0;
    private static final double TREND_CRITICAL = 3.0;
    private static final double TREND_HIGH = 2.0;
    private static final double TREND_LOW = 0.5;
    
    private final SystemData systemData;
    private final ProbeMonitor probeMonitor;
    private final Queue<Double> temperatureHistory = new LinkedList<>();
    private final Queue<Double> loadHistory = new LinkedList<>();
    private final int HISTORY_SIZE = 5;

    public TemperatureMode(SystemData systemData) {
        this.systemData = systemData;
        this.probeMonitor = new ProbeMonitor();
    }

    public void execute(Scanner scanner) {
        clearScreen();
        showMenu(scanner);
    }

    private void showMenu(Scanner scanner) {
        boolean running = true;
        while (running) {
            clearScreen();
            System.out.println("=== Mode Température ===");
            System.out.println("1. État actuel");
            System.out.println("2. Analyse détaillée");
            System.out.println("3. Surveillance continue");
            System.out.println("4. Retour au menu principal");
            System.out.print("\nChoix: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    showCurrentState(scanner);
                    break;
                case "2":
                    showDetailedAnalysis(scanner);
                    break;
                case "3":
                    showContinuousMonitoring(scanner);
                    break;
                case "4":
                    running = false;
                    break;
                default:
                    System.out.println("Choix invalide. Appuyez sur Entrée pour continuer...");
                    scanner.nextLine();
                    break;
            }
        }
    }

    private void showCurrentState(Scanner scanner) {
        clearScreen();
        System.out.println("=== État Actuel ===");
        updateMetrics();
        displayCurrentMetrics();
        waitForEnter(scanner);
    }

    private void showDetailedAnalysis(Scanner scanner) {
        clearScreen();
        System.out.println("=== Analyse Détaillée ===");
        System.out.println("\nCollecte des données en cours...");
        
        // Collecter des données pendant 5 secondes
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
            waitForEnter(scanner);
            return;
        }

        performDetailedAnalysis();
        waitForEnter(scanner);
    }

    private void showContinuousMonitoring(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Températures ===");

        if (!isWindows()) {
            System.out.println(
                    "\nLe mode Températures nécessite OpenHardwareMonitor qui n'est disponible que sur Windows.");
            System.out.println("Système détecté: " + System.getProperty("os.name"));
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        try {
            System.out.println("Surveillance des températures démarrée... (Appuyez sur Entrée pour arrêter)");

            // Thread pour vérifier l'entrée utilisateur
            Thread inputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        scanner.nextLine();
                    } catch (Exception e) {
                        // Ignorer
                    }
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();

            while (inputThread.isAlive()) {
                clearScreen();
                StringBuilder display = new StringBuilder();
                display.append("=== Mode Températures ===\n");
                display.append("Appuyez sur Entrée pour revenir au menu...\n\n");

                // Mettre à jour SystemData avec les données des capteurs
                try {
                    // Utiliser les méthodes standardisées de Monitor
                    probeMonitor.update();
                    systemData.updateDynamicData(probeMonitor.getSystemInfo());
                } catch (Exception e) {
                    display.append("Erreur lors de la récupération des températures: ").append(e.getMessage())
                            .append("\n");
                }

                // Température CPU depuis SystemData ou simulation
                display.append("=== CPU ===\n");
                if (systemData.getCpuTemperature().isPresent()) {
                    double cpuTemp = systemData.getCpuTemperature().getAsDouble();
                    display.append(String.format("Température: %.1f°C\n", cpuTemp));
                    addTemperatureBar(display, cpuTemp, "CPU");
                    addTemperatureAdvice(display, cpuTemp, "CPU");
                } else {
                    double cpuTemp = simulateCpuTemperature();
                    display.append(String.format("Température (simulée): %.1f°C\n", cpuTemp));
                    addTemperatureBar(display, cpuTemp, "CPU");
                    addTemperatureAdvice(display, cpuTemp, "CPU");
                }

                // GPU (température depuis SystemData ou simulation)
                display.append("\n=== GPU ===\n");
                if (systemData.getGpuTemperature().isPresent()) {
                    double gpuTemp = systemData.getGpuTemperature().getAsDouble();
                    display.append(String.format("Température: %.1f°C\n", gpuTemp));
                    addTemperatureBar(display, gpuTemp, "GPU");
                    addTemperatureAdvice(display, gpuTemp, "GPU");
                } else {
                    double gpuTemp = simulateGpuTemperature();
                    display.append(String.format("Température (simulée): %.1f°C\n", gpuTemp));
                    addTemperatureBar(display, gpuTemp, "GPU");
                    addTemperatureAdvice(display, gpuTemp, "GPU");
                }

                // Simulation ventilateurs (utilise les valeurs actuelles ou simulées)
                display.append("\n=== Ventilateurs ===\n");
                double currentCpuTemp = systemData.getCpuTemperature().isPresent() ? 
                    systemData.getCpuTemperature().getAsDouble() : simulateCpuTemperature();
                double currentGpuTemp = systemData.getGpuTemperature().isPresent() ? 
                    systemData.getGpuTemperature().getAsDouble() : simulateGpuTemperature();
                int[] fanSpeeds = simulateFanSpeeds(currentCpuTemp, currentGpuTemp);
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

    private void updateMetrics() {
        // Mettre à jour SystemData
        try {
            probeMonitor.update();
            systemData.updateDynamicData(probeMonitor.getSystemInfo());
        } catch (Exception e) {
            // En cas d'erreur, utiliser les valeurs simulées
        }

        // Obtenir la température actuelle (réelle ou simulée)
        double currentTemp;
        if (systemData.getCpuTemperature().isPresent()) {
            currentTemp = systemData.getCpuTemperature().getAsDouble();
        } else {
            currentTemp = simulateCpuTemperature();
        }

        // Obtenir la charge CPU actuelle
        double currentLoad = getCurrentCpuLoad();

        // Ajouter à l'historique
        temperatureHistory.offer(currentTemp);
        loadHistory.offer(currentLoad);

        // Maintenir la taille de l'historique
        if (temperatureHistory.size() > HISTORY_SIZE) {
            temperatureHistory.poll();
            loadHistory.poll();
        }
    }

    private double getCurrentCpuLoad() {
        // Utiliser SystemData si disponible
        if (systemData.getCpuLoad().isPresent()) {
            return systemData.getCpuLoad().getAsDouble();
        }
        
        // Sinon, estimer basé sur la mémoire
        Runtime runtime = Runtime.getRuntime();
        return ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100;
    }

    private void displayCurrentMetrics() {
        Double currentTemp = temperatureHistory.isEmpty() ? null : 
            ((LinkedList<Double>) temperatureHistory).peekLast();
        Double currentLoad = loadHistory.isEmpty() ? null : 
            ((LinkedList<Double>) loadHistory).peekLast();

        if (currentTemp != null) {
            System.out.printf("\nTempérature CPU: %.1f°C\n", currentTemp);
            System.out.print("État: ");
            if (currentTemp >= TEMP_CRITICAL) {
                System.out.println("❌ CRITIQUE");
            } else if (currentTemp >= TEMP_HIGH) {
                System.out.println("⚠️  ÉLEVÉE");
            } else {
                System.out.println("✅ NORMAL");
            }
        }

        if (currentLoad != null) {
            System.out.printf("\nCharge CPU: %.1f%%\n", currentLoad);
            System.out.print("État: ");
            if (currentLoad >= 80) {
                System.out.println("⚠️  ÉLEVÉE");
            } else {
                System.out.println("✅ NORMAL");
            }
        }
    }

    private void performDetailedAnalysis() {
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
            System.out.println("\n❌ TEMPÉRATURE CRITIQUE!");
            System.out.println("DANGER: Risque de dommages matériels immédiats");
            isWorrisome = true;
        } else if (currentTemp >= TEMP_HIGH) {
            System.out.println("\n⚠️  TEMPÉRATURE ÉLEVÉE");
            System.out.println("ATTENTION: Risque de surchauffe");
            isWorrisome = true;
        }
        
        double deviationFromMean = currentTemp - mean;
        if (deviationFromMean > stdDev * 2) {
            System.out.println("\n⚠️  TEMPÉRATURE ANORMALEMENT HAUTE");
            System.out.printf("Écart par rapport à la moyenne: +%.1f°C\n", deviationFromMean);
            isWorrisome = true;
        }
        
        double maxVariation = calculateMaxVariation(temps);
        if (maxVariation > VAR_EXTREME && currentTemp > mean) {
            System.out.println("\n❌ VARIATIONS EXTRÊMES");
            System.out.printf("Variation: %.1f°C/seconde\n", maxVariation);
            System.out.println("DANGER: Risque de surchauffe rapide");
            isWorrisome = true;
        } else if (maxVariation > VAR_HIGH && currentTemp > mean) {
            System.out.println("\n⚠️  VARIATIONS RAPIDES");
            System.out.printf("Variation: %.1f°C/seconde\n", maxVariation);
            isWorrisome = true;
        }
        
        if (stdDev > STD_DEV_CRITICAL && currentTemp > mean) {
            System.out.println("\n❌ INSTABILITÉ CRITIQUE");
            System.out.println("DANGER: Fluctuations dangereuses vers le haut");
            isWorrisome = true;
        } else if (stdDev > STD_DEV_HIGH && currentTemp > mean) {
            System.out.println("\n⚠️  INSTABILITÉ DÉTECTÉE");
            System.out.println("ATTENTION: Fluctuations importantes");
            isWorrisome = true;
        }
        
        System.out.println("\n=== Tendance ===");
        double trend = calculateSimpleTrend(temps);
        if (trend > TREND_CRITICAL) {
            System.out.println("❌ AUGMENTATION TRÈS RAPIDE");
            isWorrisome = true;
        } else if (trend > TREND_HIGH) {
            System.out.println("⚠️  AUGMENTATION RAPIDE");
            isWorrisome = true;
        } else if (trend > TREND_LOW) {
            System.out.println("↗️  Légère augmentation");
        } else if (trend < -TREND_CRITICAL) {
            System.out.println("↘️  Diminution rapide (normal)");
        } else if (trend < -TREND_HIGH) {
            System.out.println("↘️  Diminution (normal)");
        } else if (trend < -TREND_LOW) {
            System.out.println("↘️  Légère diminution (normal)");
        } else {
            System.out.println("→ Stable");
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
    }

    // Méthodes de calcul statistique
    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double calculateVariance(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double mean = calculateMean(values);
        double sum = 0.0;
        for (Double value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return sum / values.size();
    }

    private double calculateMaxVariation(List<Double> values) {
        if (values.size() < 2) return 0.0;
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

    private void waitForEnter(Scanner scanner) {
        System.out.println("\nAppuyez sur Entrée pour continuer...");
        scanner.nextLine();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private double simulateCpuTemperature() {
        // Simulation basée sur la charge système
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100;

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
        fanSpeeds[0] = (int) (800 + (cpuTemp - 30) * 20); // 800-1900 RPM selon température

        // Ventilateur GPU
        fanSpeeds[1] = (int) (600 + (gpuTemp - 30) * 25); // 600-2100 RPM selon température

        // Ventilateur système
        double avgTemp = (cpuTemp + gpuTemp) / 2;
        fanSpeeds[2] = (int) (500 + (avgTemp - 30) * 15); // 500-1325 RPM selon température moyenne

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

    private void addTemperatureBar(StringBuilder display, double temperature, String component) {
        int barLength = 30;
        int filledLength = (int) ((temperature / 100.0) * barLength);
        filledLength = Math.min(filledLength, barLength);
        filledLength = Math.max(filledLength, 0);

        StringBuilder tempBar = new StringBuilder();
        tempBar.append("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                if (temperature >= 80) {
                    tempBar.append("█"); // Rouge pour température élevée
                } else if (temperature >= 60) {
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
    }

    private void addTemperatureAdvice(StringBuilder display, double temperature, String component) {
        if (temperature >= 80) {
            display.append("  ⚠️  ATTENTION: Température élevée!\n");
            display.append("  Recommandations:\n");
            display.append("  - Vérifiez le refroidissement\n");
            display.append("  - Nettoyez les ventilateurs\n");
            display.append("  - Vérifiez la pâte thermique\n");
        } else if (temperature >= 60) {
            display.append("  ℹ️  Température normale sous charge\n");
        } else {
            display.append("  ✓ Température normale\n");
        }
    }
}
