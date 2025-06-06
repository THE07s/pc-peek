package com.pcpeek.src;// src/Main.java
import java.util.Scanner;
import java.io.IOException;
import java.io.File;

public class Main {
    private static Process openHardwareMonitorProcess; // Ajout d'une variable pour suivre le processus
    private static final String OHM_EXE_NAME = "OpenHardwareMonitor.exe";
    private static final String OHM_RELATIVE_PATH = "OpenHardwareMonitor/" + OHM_EXE_NAME;

    // Instances des moniteurs
    private static final SystemMonitor systemMonitor = new SystemMonitor();
    private static final HardwareMonitor hardwareMonitor = new HardwareMonitor();
    private static final OSMonitor osMonitor = new OSMonitor();
    private static final OHMMonitor ohmMonitor = new OHMMonitor();  // Ajout de l'instance OHMMonitor

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            displayMenu();
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    displaySystemInfo();
                    break;
                case "2":
                    startRTMode(scanner);
                    break;
                case "3":
                    startCPUTempMode(scanner);
                    break;
                case "4":
                    startCPUFreqMode(scanner);
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Choix invalide");
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
        System.out.println("=== Mode Temps Réel ===");
        
        if (!osMonitor.isCompatibleOS()) {
            System.out.println("\nLe mode Temps Réel n'est pas disponible sur votre système.");
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
                // Créer un script VBS temporaire pour lancer avec privilèges administrateur
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
                System.out.println("=== Mode Temps Réel ===");
                System.out.println("Appuyez sur Entrée pour revenir au menu...\n");
                
                // Mise à jour et affichage des informations dynamiques uniquement
                osMonitor.update();
                ohmMonitor.updateSensors();
                
                // Afficher uniquement les informations dynamiques
                osMonitor.display();
                System.out.println();
                ohmMonitor.display();

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
            Process cpuProcess = Runtime.getRuntime().exec("wmic cpu get name,numberofcores,numberoflogicalprocessors,maxclockspeed");
            java.io.BufferedReader cpuReader = new java.io.BufferedReader(new java.io.InputStreamReader(cpuProcess.getInputStream()));
            String cpuLine;
            boolean firstLine = true;
            boolean hasCPU = false;
            while ((cpuLine = cpuReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!cpuLine.trim().isEmpty()) {
                    hasCPU = true;
                    String[] parts = cpuLine.trim().split("\\s+", 4);
                    if (parts.length >= 4) {
                        if (!hasCPU) {
                            System.out.println("\n=== Processeur ===");
                        }
                        System.out.println("Modèle : " + parts[0]);
                        System.out.println("Cœurs physiques : " + parts[1]);
                        System.out.println("Threads : " + parts[2]);
                        System.out.println("Fréquence maximale : " + (Integer.parseInt(parts[3]) / 1000.0) + " GHz");
                    }
                }
            }
            cpuProcess.waitFor();

            // Informations GPU
            Process gpuProcess = Runtime.getRuntime().exec("wmic path win32_VideoController get name,adapterram,driverversion,currentrefreshrate");
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
                    String[] parts = gpuLine.trim().split("\\s+", 4);
                    if (parts.length >= 4) {
                        System.out.println("\nCarte graphique : " + parts[0]);
                        try {
                            long vram = Long.parseLong(parts[1]);
                            System.out.println("Mémoire vidéo : " + formatSize(vram));
                        } catch (NumberFormatException e) {
                            // Ignorer si la mémoire vidéo n'est pas disponible
                        }
                        System.out.println("Version du pilote : " + parts[2]);
                        try {
                            int refreshRate = Integer.parseInt(parts[3]);
                            if (refreshRate > 0) {
                                System.out.println("Fréquence de rafraîchissement : " + refreshRate + " Hz");
                            }
                        } catch (NumberFormatException e) {
                            // Ignorer si la fréquence n'est pas disponible
                        }
                    } else if (parts.length >= 1) {
                        System.out.println("\nCarte graphique : " + parts[0]);
                    }
                }
            }

            // Informations RAM
            Process ramProcess = Runtime.getRuntime().exec("wmic computersystem get totalphysicalmemory");
            java.io.BufferedReader ramReader = new java.io.BufferedReader(new java.io.InputStreamReader(ramProcess.getInputStream()));
            String ramLine;
            firstLine = true;
            boolean hasRAM = false;
            while ((ramLine = ramReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!ramLine.trim().isEmpty()) {
                    hasRAM = true;
                    long totalRAM = Long.parseLong(ramLine.trim());
                    if (totalRAM > 0) {
                        System.out.println("\n=== Mémoire ===");
                        System.out.println("RAM totale : " + formatSize(totalRAM));
                    }
                }
            }
            ramProcess.waitFor();

            // Informations carte mère
            Process mbProcess = Runtime.getRuntime().exec("wmic baseboard get manufacturer,product,serialnumber,version");
            java.io.BufferedReader mbReader = new java.io.BufferedReader(new java.io.InputStreamReader(mbProcess.getInputStream()));
            String mbLine;
            firstLine = true;
            boolean hasMB = false;
            while ((mbLine = mbReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!mbLine.trim().isEmpty()) {
                    String[] parts = mbLine.trim().split("\\s+", 4);
                    if (parts.length >= 4 && !parts[0].equals("NULL")) {
                        if (!hasMB) {
                            System.out.println("\n=== Carte Mère ===");
                            hasMB = true;
                        }
                        System.out.println("Fabricant : " + parts[0]);
                        System.out.println("Modèle : " + parts[1]);
                        if (!parts[2].equals("NULL")) {
                            System.out.println("Numéro de série : " + parts[2]);
                        }
                        if (!parts[3].equals("NULL")) {
                            System.out.println("Version : " + parts[3]);
                        }
                    }
                }
            }
            mbProcess.waitFor();

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

        // Informations du moniteur OS (uniquement si des informations sont disponibles)
        if (osMonitor.hasInfo()) {
            System.out.println("\n=== Informations OS Détailées ===");
            osMonitor.display();
        }

        // Informations du moniteur Hardware (uniquement si des informations sont disponibles)
        if (hardwareMonitor.hasInfo()) {
            System.out.println("\n=== Informations Matérielles Détailées ===");
            hardwareMonitor.display();
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

    private static void startCPUTempMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Température CPU ===");
        
        if (!osMonitor.isCompatibleOS()) {
            System.out.println("\n⚠️ ATTENTION: Ce mode n'est pas disponible sur votre système d'exploitation.");
            System.out.println("La lecture de la température CPU nécessite OpenHardwareMonitor qui ne fonctionne que sur Windows.");
            System.out.println("\nSystème détecté: " + System.getProperty("os.name"));
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        try {
            // Ferme l'instance précédente d'OpenHardwareMonitor
            stopOpenHardwareMonitor();
            Thread.sleep(1000);

            // Chercher et lancer OpenHardwareMonitor
            String ohmPath = findOpenHardwareMonitor();
            if (ohmPath == null) {
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            // Lance OpenHardwareMonitor avec privilèges administrateur
            try {
                // Créer un script VBS temporaire pour lancer avec privilèges administrateur
                File vbsFile = createElevationScript(ohmPath);
                ProcessBuilder pb = new ProcessBuilder("wscript.exe", vbsFile.getAbsolutePath());
                openHardwareMonitorProcess = pb.start();
                Thread.sleep(2000); // Attendre l'initialisation

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
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
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
                        
                        // Afficher la température CPU
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
                            display.append("   - Vérifiez la pâte thermique\n");
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
        } finally {
            stopOpenHardwareMonitor();
        }
    }

    private static void startCPUFreqMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Fréquences CPU ===");
        
        if (!osMonitor.isCompatibleOS()) {
            System.out.println("\n⚠️ ATTENTION: Ce mode n'est pas disponible sur votre système d'exploitation.");
            System.out.println("La lecture des fréquences CPU nécessite Windows.");
            System.out.println("\nSystème détecté: " + System.getProperty("os.name"));
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
            return;
        }

        System.out.println("\nAppuyez sur 'q' pour quitter, ou une autre touche pour mettre à jour...");
        
        boolean running = true;
        while (running) {
            try {
                // Mettre à jour les capteurs
                ohmMonitor.updateSensors();
                
                // Effacer l'écran pour un affichage plus propre
                clearScreen();
                System.out.println("=== Mode Fréquences CPU ===");
                
                // Afficher les informations CPU
                System.out.println("\nProcesseur : " + ohmMonitor.getProcessorName());
                System.out.println("=".repeat(50));
                
                // Afficher les fréquences par cœur
                double[] frequencies = ohmMonitor.getCpuFrequencies();
                if (frequencies != null && frequencies.length > 0) {
                    System.out.println("\nFréquences par cœur :");
                    System.out.println("-".repeat(30));
                    
                    for (int i = 0; i < frequencies.length; i++) {
                        if (frequencies[i] > 0) {
                            // Créer une barre visuelle pour la fréquence
                            int barLength = 30;
                            double maxFreq = 5000; // 5 GHz comme maximum pour la visualisation
                            int filledLength = (int) ((frequencies[i] / maxFreq) * barLength);
                            filledLength = Math.min(filledLength, barLength);
                            
                            StringBuilder bar = new StringBuilder();
                            bar.append("[");
                            for (int j = 0; j < barLength; j++) {
                                if (j < filledLength) {
                                    if (frequencies[i] >= 4000) {
                                        bar.append("█"); // Fréquence élevée
                                    } else if (frequencies[i] >= 3000) {
                                        bar.append("▓"); // Fréquence moyenne
                                    } else {
                                        bar.append("░"); // Fréquence basse
                                    }
                                } else {
                                    bar.append(" ");
                                }
                            }
                            bar.append("]");
                            
                            System.out.printf("Cœur %2d: %6.2f GHz %s%n", 
                                i, frequencies[i] / 1000.0, bar.toString());
                        }
                    }
                    
                    // Afficher la fréquence moyenne
                    double avgFreq = 0;
                    int count = 0;
                    for (double freq : frequencies) {
                        if (freq > 0) {
                            avgFreq += freq;
                            count++;
                        }
                    }
                    if (count > 0) {
                        avgFreq /= count;
                        System.out.printf("\nFréquence moyenne : %.2f GHz%n", avgFreq / 1000.0);
                    }
                } else {
                    System.out.println("\n⚠️ Impossible de récupérer les fréquences CPU");
                }
                
                // Attendre l'entrée utilisateur
                if (System.in.available() > 0) {
                    int input = System.in.read();
                    if (input == 'q' || input == 'Q') {
                        running = false;
                    }
                }
                
                // Attendre un court instant avant la prochaine mise à jour
                Thread.sleep(1000);
                
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour des fréquences : " + e.getMessage());
                running = false;
            }
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
            if (System.getProperty("os.name").contains("Windows")) {
                // Pour Windows, utiliser une séquence de commandes plus agressive
                new ProcessBuilder("cmd", "/c", "cls && echo. && echo. && echo.").inheritIO().start().waitFor();
                // Repositionner le curseur en haut
                System.out.print("\033[H");
                System.out.flush();
            } else {
                // Pour Unix/Linux/Mac, utiliser une séquence ANSI plus complète
                System.out.print("\033[H\033[2J\033[3J");
                System.out.flush();
            }
        } catch (Exception e) {
            // En cas d'échec, on essaie une autre approche
            try {
                // Essayer de nettoyer avec des caractères de contrôle
                System.out.print("\u001b[H\u001b[2J\u001b[3J");
                System.out.flush();
            } catch (Exception ex) {
                // Dernier recours : beaucoup de lignes vides
                for (int i = 0; i < 100; i++) {
                    System.out.println();
                }
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n=== PC-Peek ===");
        System.out.println("1. Informations STATIC");
        System.out.println("2. Mode temps réel");
        System.out.println("3. Mode température CPU");
        System.out.println("4. Mode fréquences CPU");
        System.out.println("0. Quitter");
        System.out.print("\nVotre choix : ");
    }

    private static void displaySystemInfo() {
        startStaticMode(new Scanner(System.in));
    }

    private static void displayHardwareInfo() {
        // Implementation of displayHardwareInfo method
    }
}