package com.pcpeek;// src/Main.java
import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static Process openHardwareMonitorProcess; // Ajout d'une variable pour suivre le processus
    private static final String OHM_EXE_NAME = "OpenHardwareMonitor.exe";
    private static final String OHM_RELATIVE_PATH = "OpenHardwareMonitor/" + OHM_EXE_NAME;

    // Instances des moniteurs
    private static final SystemMonitor systemMonitor = new SystemMonitor();
    private static final HardwareMonitor hardwareMonitor = new HardwareMonitor();
    private static final OSMonitor osMonitor = new OSMonitor();
    private static OHMMonitor ohmMonitor;

    static {
        try {
            ohmMonitor = new OHMMonitor();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation d'OHMMonitor: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            try {
                displayMenu();
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consommer le retour à la ligne
                handleMenuChoice(choice, scanner);
            } catch (Exception e) {
                System.err.println("Erreur : " + e.getMessage());
                scanner.nextLine(); // Consommer l'entrée invalide
            }
        }
        scanner.close();
    }

    private static String findOpenHardwareMonitor() {
        // Vérifier le système d'exploitation
        if (!osMonitor.isCompatibleOS()) {
            System.out.println("\n⚠️ ATTENTION: OpenHardwareMonitor n'est pas compatible avec votre système d'exploitation.");
            System.out.println("OpenHardwareMonitor est un programme Windows (.exe) qui ne fonctionne que sur Windows.");
            System.out.println("\nSystème détecté: " + System.getProperty("os.name"));
            System.out.println("Le mode Temps Réel (RT) n'est pas disponible sur votre système.");
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            return null;
        }

        // Chercher OpenHardwareMonitor dans plusieurs emplacements possibles
        String[] possiblePaths = {
            "OpenHardwareMonitor/OpenHardwareMonitor.exe",  // Chemin relatif
            "src/main/java/com/pcpeek/OpenHardwareMonitor/OpenHardwareMonitor.exe",  // Chemin dans le projet
            System.getProperty("user.dir") + "/OpenHardwareMonitor/OpenHardwareMonitor.exe",  // Chemin absolu depuis le répertoire de travail
            "D:/DATA - Java E3e/pc-peek/OpenHardwareMonitor/OpenHardwareMonitor.exe"  // Chemin absolu spécifique
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                System.out.println("OpenHardwareMonitor trouvé: " + file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }
        
        // Si non trouvé, afficher un message détaillé
        System.out.println("\n❌ OpenHardwareMonitor.exe non trouvé!");
        System.out.println("Emplacements recherchés:");
        for (String path : possiblePaths) {
            System.out.println("  - " + path);
        }
        System.out.println("\nVeuillez vous assurer que OpenHardwareMonitor.exe est présent dans l'un de ces emplacements.");
        return null;
    }

    private static void startRTMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Real Time ===");
        
        if (!osMonitor.isCompatibleOS()) {
            System.out.println("\nLe mode Real Time n'est pas disponible sur votre système.");
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        try {
            // Ferme l'instance précédente d'OpenHardwareMonitor
            stopOpenHardwareMonitor();
            Thread.sleep(1000);

            // Chercher OpenHardwareMonitor
            String ohmPath = findOpenHardwareMonitor();
            
            if (ohmPath == null) {
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            // Lance OpenHardwareMonitor avec privilèges administrateur
            try {
                File vbsFile = createElevationScript(ohmPath);
                ProcessBuilder pb = new ProcessBuilder("wscript.exe", vbsFile.getAbsolutePath());
                openHardwareMonitorProcess = pb.start();
                Thread.sleep(2000); // Attendre l'initialisation
                
                System.out.println("Lancement d'OpenHardwareMonitor avec privilèges administrateur...");

                // Tenter de se connecter à OHM
                if (!ohmMonitor.connect()) {
                    System.out.println("\n⚠️ Impossible de se connecter à OpenHardwareMonitor.");
                    System.out.println("Veuillez vérifier que l'application est bien lancée avec les privilèges administrateur.");
                    System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                    scanner.nextLine();
                    return;
                }

            } catch (Exception e) {
                System.err.println("Erreur lors du lancement d'OpenHardwareMonitor: " + e.getMessage());
                if (e.getMessage().contains("740")) {
                    System.out.println("\n⚠️ L'application nécessite des privilèges administrateur.");
                    System.out.println("Veuillez relancer le programme en tant qu'administrateur.");
                }
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            boolean monitoring = true;
            while (monitoring) {
                clearScreen();
                StringBuilder display = new StringBuilder();
                display.append("=== Mode Real Time ===\n");
                display.append("Appuyez sur Entrée pour revenir au menu...\n\n");
                
                // Mise à jour des capteurs
                ohmMonitor.updateSensors();
                
                // Informations CPU
                display.append("=== CPU ===\n");
                // Nom du CPU
                if (hardwareMonitor.systemInfo.containsKey("cpu.name")) {
                    display.append("Modèle: ").append(hardwareMonitor.systemInfo.get("cpu.name")).append("\n");
                }
                
                // Charge CPU globale et par cœur
                double[] loadPerCore = ohmMonitor.getCpuLoadPerCore();
                if (loadPerCore != null && loadPerCore.length > 0) {
                    double totalLoad = 0;
                    display.append("\nCharge par cœur:\n");
                    for (int i = 0; i < loadPerCore.length; i++) {
                        totalLoad += loadPerCore[i];
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
                        display.append(String.format("  Cœur %d: %s %.1f%%\n", i, bar.toString(), loadPerCore[i] * 100));
                    }
                    display.append(String.format("\nCharge CPU globale: %.1f%%\n", (totalLoad / loadPerCore.length) * 100));
                }
                
                // Températures CPU
                double cpuTemp = ohmMonitor.getCpuTemperature();
                if (cpuTemp > 0) {
                    display.append("\nTempératures CPU:\n");
                    display.append(String.format("  Global: %.1f°C\n", cpuTemp));
                    
                    // Barre de température
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
                    display.append("  ").append(tempBar.toString()).append("\n");
                    
                    // Indicateurs de température
                    if (cpuTemp >= 80) {
                        display.append("  ⚠️  ATTENTION: Température élevée!\n");
                    } else if (cpuTemp >= 60) {
                        display.append("  ℹ️  Température normale sous charge\n");
                    } else {
                        display.append("  ✓ Température normale\n");
                    }
                }
                
                // Mémoire
                display.append("\n=== Mémoire ===\n");
                long totalMemory = ohmMonitor.getTotalMemory();
                long availableMemory = ohmMonitor.getAvailableMemory();
                long usedMemory = totalMemory - availableMemory;
                double memoryUsagePercent = (usedMemory * 100.0) / totalMemory;
                
                display.append(String.format("Totale: %.1f GB\n", totalMemory / (1024.0 * 1024.0 * 1024.0)));
                display.append(String.format("Utilisée: %.1f GB (%.1f%%)\n", 
                    usedMemory / (1024.0 * 1024.0 * 1024.0), memoryUsagePercent));
                display.append(String.format("Disponible: %.1f GB\n", 
                    availableMemory / (1024.0 * 1024.0 * 1024.0)));
                
                // Barre de progression mémoire
                int memBarLength = 30;
                int memFilledLength = (int) (memoryUsagePercent * memBarLength / 100);
                StringBuilder memBar = new StringBuilder();
                memBar.append("[");
                for (int i = 0; i < memBarLength; i++) {
                    if (i < memFilledLength) {
                        if (memoryUsagePercent >= 90) {
                            memBar.append("█");
                        } else if (memoryUsagePercent >= 70) {
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
                
                // Ventilateurs
                int[] fanSpeeds = ohmMonitor.getFanSpeeds();
                if (fanSpeeds != null && fanSpeeds.length > 0) {
                    display.append("\n=== Ventilateurs ===\n");
                    for (int i = 0; i < fanSpeeds.length; i++) {
                        if (fanSpeeds[i] > 0) {
                            display.append(String.format("Ventilateur %d: %d RPM\n", i, fanSpeeds[i]));
                        }
                    }
                }
                
                // GPU (si disponible)
                display.append("\n=== GPU ===\n");
                double gpuTemp = ohmMonitor.getGpuTemperature();
                double gpuLoad = ohmMonitor.getGpuLoad();
                if (gpuTemp > 0 || gpuLoad > 0) {
                    if (gpuTemp > 0) {
                        display.append(String.format("Température: %.1f°C\n", gpuTemp));
                    }
                    if (gpuLoad > 0) {
                        display.append(String.format("Charge: %.1f%%\n", gpuLoad));
                    }
                } else {
                    display.append("Informations non disponibles\n");
                }

                System.out.print(display.toString());
                System.out.flush();

                // Attendre l'entrée de l'utilisateur
                if (scanner.hasNextLine()) {
                    scanner.nextLine();
                    monitoring = false;
                }
                Thread.sleep(1000); // Mise à jour toutes les secondes
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du mode temps réel: " + e.getMessage());
        } finally {
            stopOpenHardwareMonitor();
        }
    }

    private static File createElevationScript(String exePath) throws IOException {
        // Créer un script VBS temporaire pour lancer avec privilèges administrateur
        String vbsContent = 
            "Set UAC = CreateObject(\"Shell.Application\")\n" +
            "UAC.ShellExecute \"" + exePath.replace("\\", "\\\\") + "\", \"\", \"\", \"runas\", 1\n";
        
        File vbsFile = File.createTempFile("elevate", ".vbs");
        vbsFile.deleteOnExit();
        
        try (java.io.FileWriter writer = new java.io.FileWriter(vbsFile)) {
            writer.write(vbsContent);
        }
        
        return vbsFile;
    }

    private static void startStaticMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Informations STATIC ===");
        System.out.println("Affichage des informations système détaillées...\n");

        // Informations système de base
        System.out.println("=== Informations Système ===");
        try {
            // Informations CPU
            Process cpuProcess = Runtime.getRuntime().exec("wmic cpu get name");
            java.io.BufferedReader cpuReader = new java.io.BufferedReader(new java.io.InputStreamReader(cpuProcess.getInputStream()));
            String cpuLine;
            boolean firstLine = true;
            while ((cpuLine = cpuReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!cpuLine.trim().isEmpty()) {
                    System.out.println("\n=== Processeur ===");
                    System.out.println(cpuLine.trim());
                }
            }
            cpuProcess.waitFor();

            // Informations GPU
            Process gpuProcess = Runtime.getRuntime().exec("wmic path win32_VideoController get name");
            java.io.BufferedReader gpuReader = new java.io.BufferedReader(new java.io.InputStreamReader(gpuProcess.getInputStream()));
            String gpuLine;
            firstLine = true;
            boolean hasGPU = false;
            while ((gpuLine = gpuReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!gpuLine.trim().isEmpty()) {
                    if (!hasGPU) {
                        System.out.println("\n=== Cartes Graphiques ===");
                        hasGPU = true;
                    }
                    System.out.println(gpuLine.trim());
                }
            }

            // Informations disques avec espace
            System.out.println("\n=== Disques ===");
            File[] roots = File.listRoots();
            for (File root : roots) {
                if (root.getTotalSpace() > 0) {
                    System.out.println("\n" + root.getPath());
                    System.out.println("Espace total : " + formatSize(root.getTotalSpace()));
                    System.out.println("Espace libre : " + formatSize(root.getFreeSpace()));
                    System.out.println("Espace utilisé : " + formatSize(root.getTotalSpace() - root.getFreeSpace()));
                    double usedPercent = ((double)(root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace()) * 100;
                    System.out.println("Utilisé : " + String.format("%.1f%%", usedPercent));
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des informations système : " + e.getMessage());
        }

        System.out.println("\nSystème d'exploitation : " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("Architecture : " + System.getProperty("os.arch"));
        System.out.println("Version Java : " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Utilisateur : " + System.getProperty("user.name"));
        System.out.println("Répertoire de travail : " + System.getProperty("user.dir"));
        System.out.println("Processeurs disponibles : " + Runtime.getRuntime().availableProcessors());
        System.out.println("Mémoire totale (Java) : " + formatSize(Runtime.getRuntime().totalMemory()));
        System.out.println("Mémoire utilisée (Java) : " + formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

        // État d'activation Windows
        if (osMonitor.isCompatibleOS()) {
            System.out.println("\n=== État d'Activation Windows ===");
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "wmic",
                    "path",
                    "win32_operatingsystem",
                    "get",
                    "caption,version,osarchitecture,serialnumber,licensedatetime"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
                );
                
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.trim().split("\\s+", 5);
                        if (parts.length >= 5) {
                            System.out.println("Système : " + parts[0] + " " + parts[1]);
                            System.out.println("Architecture : " + parts[2]);
                            if (!parts[3].equals("NULL")) {
                                System.out.println("Numéro de série : " + parts[3]);
                            }
                            if (!parts[4].equals("NULL")) {
                                System.out.println("Date d'activation : " + parts[4]);
                            }
                        }
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                System.out.println("Impossible de déterminer l'état d'activation de Windows");
            }
        }

        // Informations sur la batterie
        if (osMonitor.isCompatibleOS()) {
            System.out.println("\n=== Informations Batterie ===");
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "wmic",
                    "path",
                    "Win32_Battery",
                    "get",
                    "EstimatedChargeRemaining,Status"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
                );
                
                String line;
                boolean firstLine = true;
                boolean hasBattery = false;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    if (!line.trim().isEmpty()) {
                        hasBattery = true;
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            System.out.println("Niveau de charge : " + parts[0] + "%");
                            System.out.println("État : " + parts[1]);
                        }
                    }
                }
                if (!hasBattery) {
                    System.out.println("Aucune batterie détectée");
                }
                process.waitFor();
            } catch (Exception e) {
                System.out.println("Impossible de récupérer les informations de la batterie");
            }
        }

        // Informations réseau
        System.out.println("\n=== Informations Réseau ===");
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            System.out.println("Nom d'hôte : " + localHost.getHostName());
            
            final boolean[] hasNetworkInterfaces = {false};
            java.net.NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(ni -> {
                try {
                    if (ni.isUp() && !ni.isLoopback()) {
                        hasNetworkInterfaces[0] = true;
                        System.out.println("\nInterface : " + ni.getDisplayName());
                        System.out.println("  Nom : " + ni.getName());
                        System.out.println("  Adresse MAC : " + formatMAC(ni.getHardwareAddress()));
                        ni.getInetAddresses().asIterator().forEachRemaining(addr -> {
                            if (!addr.isLoopbackAddress() && !addr.getHostAddress().startsWith("169.254.")) {
                                System.out.println("  Adresse IP : " + addr.getHostAddress());
                            }
                        });
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs sur les interfaces individuelles
                }
            });
            if (!hasNetworkInterfaces[0]) {
                System.out.println("Aucune interface réseau active détectée");
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération des informations réseau");
        }

        // Informations détaillées sur les disques
        System.out.println("\n=== Disques ===");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "wmic",
                "diskdrive",
                "get",
                "model,size,status"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            boolean firstLine = true;
            boolean hasDisks = false;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    String[] parts = line.trim().split("\\s+", 3);
                    if (parts.length >= 3 && !parts[0].equals("NULL")) {
                        if (!hasDisks) {
                            System.out.println("\n=== Disques ===");
                            hasDisks = true;
                        }
                        System.out.println("\n" + parts[0]);
                        try {
                            long size = Long.parseLong(parts[1]);
                            if (size > 0) {
                                System.out.println("Taille : " + formatSize(size));
                            }
                        } catch (NumberFormatException e) {
                            // Ignorer si la taille n'est pas disponible
                        }
                        if (!parts[2].equals("NULL")) {
                            System.out.println("État : " + parts[2]);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération des informations disque");
        }

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }

    private static String getDriveType(String path) {
        if (osMonitor.isCompatibleOS()) {
            try {
                Process process = Runtime.getRuntime().exec("wmic logicaldisk where deviceid='" + path.substring(0, 2) + "' get drivetype");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    if (!line.trim().isEmpty()) {
                        int type = Integer.parseInt(line.trim());
                        switch (type) {
                            case 0: return "Inconnu";
                            case 1: return "Non existant";
                            case 2: return "Amovible";
                            case 3: return "Fixe";
                            case 4: return "Réseau";
                            case 5: return "CD-ROM";
                            case 6: return "RAM Disk";
                            default: return "Type " + type;
                        }
                    }
                }
            } catch (Exception e) {
                return "Erreur de lecture";
            }
        }
        return "Non disponible";
    }

    private static String formatMAC(byte[] mac) {
        if (mac == null) return "Non disponible";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return sb.toString();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private static void startSystemInfoMode(Scanner scanner) {
        try {
            clearScreen();
            System.out.println("=== Mode Informations Système ===");
            System.out.println("Appuyez sur Entrée pour revenir au menu...");
            
            // Afficher les informations système
            ohmMonitor.display();
            
            scanner.nextLine();
        } catch (Exception e) {
            System.err.println("Erreur lors du mode informations système : " + e.getMessage());
        }
    }

    private static void startCPUTempMode(Scanner scanner) {
        try {
            if (!isWindows()) {
                System.out.println("⚠️  Le mode température CPU n'est disponible que sous Windows");
                System.out.println("Appuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            // Variable pour contrôler la boucle de mise à jour
            final boolean[] monitoring = {true};
            
            // Thread pour la mise à jour de la température
            Thread updateThread = new Thread(() -> {
                while (monitoring[0]) {
                    try {
                        clearScreen();
                        StringBuilder display = new StringBuilder();
                        display.append("=== Mode Température CPU ===\n");
                        display.append("Appuyez sur Entrée pour revenir au menu...\n\n");
                        
                        // Mettre à jour les capteurs
                        ohmMonitor.updateSensors();
                        
                        double temp = ohmMonitor.getCpuTemperature();
                        String tempStr = String.format("%.1f°C", temp);
                        display.append("Température CPU: ").append(tempStr).append("\n");
                        
                        // Créer la barre de température
                        int barLength = 30;
                        int filledLength = (int) ((temp / 100.0) * barLength);
                        filledLength = Math.min(filledLength, barLength);
                        filledLength = Math.max(filledLength, 0);
                        
                        StringBuilder bar = new StringBuilder();
                        bar.append("[");
                        for (int i = 0; i < barLength; i++) {
                            if (i < filledLength) {
                                if (temp >= 80) {
                                    bar.append("█");
                                } else if (temp >= 60) {
                                    bar.append("▓");
                                } else {
                                    bar.append("░");
                                }
                            } else {
                                bar.append(" ");
                            }
                        }
                        bar.append("]\n\n");
                        display.append(bar);
                        
                        // Ajouter les conseils
                        if (temp >= 80) {
                            display.append("⚠️  ATTENTION: Température élevée!\n");
                            display.append("   - Vérifiez le refroidissement\n");
                            display.append("   - Nettoyez les ventilateurs\n");
                        } else if (temp >= 60) {
                            display.append("ℹ️  Température normale sous charge\n");
                        } else {
                            display.append("✓ Température normale\n");
                        }

                        System.out.print(display.toString());
                        System.out.flush();

                        Thread.sleep(1000);
                    } catch (Exception e) {
                        if (monitoring[0]) {
                            System.err.println("Erreur lors de la mise à jour: " + e.getMessage());
                        }
                    }
                }
            });

            updateThread.start();
            scanner.nextLine();
            monitoring[0] = false;
            updateThread.join(1000);

        } catch (Exception e) {
            System.err.println("Erreur lors du mode température: " + e.getMessage());
        }
    }

    private static void stopOpenHardwareMonitor() {
        try {
            if (openHardwareMonitorProcess != null) {
                openHardwareMonitorProcess.destroy();
                openHardwareMonitorProcess = null;
            }
            // S'assurer que le processus est bien fermé
            Process killProcess = Runtime.getRuntime().exec("taskkill /F /IM OpenHardwareMonitor.exe");
            killProcess.waitFor();
            System.out.println("Arrêt d'OpenHardwareMonitor...");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'arrêt d'OpenHardwareMonitor: " + e.getMessage());
        }
    }

    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage de l'écran: " + e.getMessage());
        }
    }

    private static void displayMenu() {
        System.out.println("\n=== PC-Peek ===");
        System.out.println("1. Mode Statique (snapshot complet)");
        System.out.println("2. Mode Real Time (informations dynamiques)");
        System.out.println("3. Mode Températures (affichage en temps réel)");
        System.out.println("4. Quitter");
        System.out.print("\nVotre choix : ");
    }

    private static void handleMenuChoice(int choice, Scanner scanner) {
        switch (choice) {
            case 1:
                startStaticMode(scanner);
                break;
            case 2:
                startRTMode(scanner);
                break;
            case 3:
                startTemperatureMode(scanner);
                break;
            case 4:
                System.out.println("Au revoir !");
                System.exit(0);
                break;
            default:
                System.out.println("Choix invalide !");
        }
    }

    private static void startTemperatureMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Températures ===");
        
        if (!osMonitor.isCompatibleOS()) {
            System.out.println("\nLe mode Températures n'est pas disponible sur votre système.");
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        try {
            // Ferme l'instance précédente d'OpenHardwareMonitor
            stopOpenHardwareMonitor();
            Thread.sleep(1000);

            // Chercher OpenHardwareMonitor
            String ohmPath = findOpenHardwareMonitor();
            
            if (ohmPath == null) {
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            // Lance OpenHardwareMonitor avec privilèges administrateur
            try {
                File vbsFile = createElevationScript(ohmPath);
                ProcessBuilder pb = new ProcessBuilder("wscript.exe", vbsFile.getAbsolutePath());
                openHardwareMonitorProcess = pb.start();
                Thread.sleep(2000); // Attendre l'initialisation
                
                System.out.println("Lancement d'OpenHardwareMonitor avec privilèges administrateur...");

                // Tenter de se connecter à OHM
                if (!ohmMonitor.connect()) {
                    System.out.println("\n⚠️ Impossible de se connecter à OpenHardwareMonitor.");
                    System.out.println("Veuillez vérifier que l'application est bien lancée avec les privilèges administrateur.");
                    System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                    scanner.nextLine();
                    return;
                }

            } catch (Exception e) {
                System.err.println("Erreur lors du lancement d'OpenHardwareMonitor: " + e.getMessage());
                if (e.getMessage().contains("740")) {
                    System.out.println("\n⚠️ L'application nécessite des privilèges administrateur.");
                    System.out.println("Veuillez relancer le programme en tant qu'administrateur.");
                }
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            // Créer un thread pour lire l'entrée utilisateur
            final boolean[] monitoring = {true};
            Thread inputThread = new Thread(() -> {
                try {
                    while (monitoring[0]) {
                        if (System.in.available() > 0) {
                            int input = System.in.read();
                            if (input == 'q' || input == 'Q') {
                                monitoring[0] = false;
                                break;
                            }
                        }
                        Thread.sleep(100); // Vérifier toutes les 100ms
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs de lecture
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();

            // Boucle principale de mise à jour
            while (monitoring[0]) {
                clearScreen();
                StringBuilder display = new StringBuilder();
                display.append("=== Mode Températures ===\n");
                display.append("Appuyez sur 'Q' pour quitter...\n\n");
                
                // Mise à jour des capteurs
                ohmMonitor.updateSensors();
                
                // Température CPU
                double cpuTemp = ohmMonitor.getCpuTemperature();
                if (cpuTemp > 0) {
                    display.append("=== CPU ===\n");
                    display.append(String.format("Température: %.1f°C\n", cpuTemp));
                    
                    // Barre de température CPU
                    int barLength = 30;
                    int filledLength = (int) ((cpuTemp / 100.0) * barLength);
                    filledLength = Math.min(filledLength, barLength);
                    filledLength = Math.max(filledLength, 0);
                    
                    StringBuilder tempBar = new StringBuilder();
                    tempBar.append("[");
                    for (int i = 0; i < barLength; i++) {
                        if (i < filledLength) {
                            if (cpuTemp >= 80) {
                                tempBar.append("█"); // Rouge pour température élevée
                            } else if (cpuTemp >= 60) {
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
                    
                    // Indicateurs de température CPU
                    if (cpuTemp >= 80) {
                        display.append("  ⚠️  ATTENTION: Température élevée!\n");
                        display.append("  Recommandations:\n");
                        display.append("  - Vérifiez le refroidissement\n");
                        display.append("  - Nettoyez les ventilateurs\n");
                        display.append("  - Vérifiez la pâte thermique\n");
                    } else if (cpuTemp >= 60) {
                        display.append("  ℹ️  Température normale sous charge\n");
                    } else {
                        display.append("  ✓ Température normale\n");
                    }
                }
                
                // Température GPU
                double gpuTemp = ohmMonitor.getGpuTemperature();
                if (gpuTemp > 0) {
                    display.append("\n=== GPU ===\n");
                    display.append(String.format("Température: %.1f°C\n", gpuTemp));
                    
                    // Barre de température GPU
                    int barLength = 30;
                    int filledLength = (int) ((gpuTemp / 100.0) * barLength);
                    filledLength = Math.min(filledLength, barLength);
                    filledLength = Math.max(filledLength, 0);
                    
                    StringBuilder tempBar = new StringBuilder();
                    tempBar.append("[");
                    for (int i = 0; i < barLength; i++) {
                        if (i < filledLength) {
                            if (gpuTemp >= 80) {
                                tempBar.append("█"); // Rouge pour température élevée
                            } else if (gpuTemp >= 60) {
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
                    
                    // Indicateurs de température GPU
                    if (gpuTemp >= 80) {
                        display.append("  ⚠️  ATTENTION: Température élevée!\n");
                        display.append("  Recommandations:\n");
                        display.append("  - Vérifiez le refroidissement\n");
                        display.append("  - Nettoyez les ventilateurs\n");
                        display.append("  - Vérifiez la pâte thermique\n");
                    } else if (gpuTemp >= 60) {
                        display.append("  ℹ️  Température normale sous charge\n");
                    } else {
                        display.append("  ✓ Température normale\n");
                    }
                }
                
                // Ventilateurs
                int[] fanSpeeds = ohmMonitor.getFanSpeeds();
                if (fanSpeeds != null && fanSpeeds.length > 0) {
                    display.append("\n=== Ventilateurs ===\n");
                    for (int i = 0; i < fanSpeeds.length; i++) {
                        if (fanSpeeds[i] > 0) {
                            display.append(String.format("Ventilateur %d: %d RPM\n", i, fanSpeeds[i]));
                        }
                    }
                }

                // Légende
                display.append("\n=== Légende ===\n");
                display.append("░ : Température normale (< 60°C)\n");
                display.append("▓ : Température moyenne (60-80°C)\n");
                display.append("█ : Température élevée (> 80°C)\n");

                System.out.print(display.toString());
                System.out.flush();

                Thread.sleep(1000); // Mise à jour toutes les secondes
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du mode températures: " + e.getMessage());
        } finally {
            stopOpenHardwareMonitor();
        }
    }

    private static void startHardwareMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Informations Matérielles ===");
        hardwareMonitor.display();
        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }

    private static void startOSMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Informations OS ===");
        osMonitor.display();
        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }
}