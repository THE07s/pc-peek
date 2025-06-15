package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import java.util.Scanner;
import java.io.File;

public class StaticInfoMode {
    private final SystemData systemData;
    
    public StaticInfoMode(SystemData systemData) {
        this.systemData = systemData;
    }
      public void execute(Scanner scanner) {
        clearScreen();
        System.out.println("=== Informations STATIC ===");
        System.out.println("Affichage des informations système détaillées...\n");

        // Collecter et mettre à jour les données dans SystemData
        collectSystemInfo();

        // Afficher les informations depuis SystemData
        displaySystemInfo();

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }

    private void collectSystemInfo() {
        // Informations système de base
        try {
            // Informations OS
            systemData.setOsName(System.getProperty("os.name"));
            systemData.setOsVersion(System.getProperty("os.version"));
            systemData.setOsArchitecture(System.getProperty("os.arch"));

            // Informations CPU via WMI
            collectCpuInfo();
            
            // Informations GPU via WMI
            collectGpuInfo();
            
            // Informations disques
            collectDiskInfo();
            
            // Informations réseau
            collectNetworkInfo();
            
            // État d'activation Windows
            if (isCompatibleOS()) {
                collectWindowsActivationInfo();
                collectBatteryInfo();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des informations système : " + e.getMessage());
        }
    }

    private void displaySystemInfo() {
        // Afficher les informations depuis SystemData
        System.out.println("=== Informations Système ===");
        
        systemData.getCpuName().ifPresent(name -> 
            System.out.println("\n=== Processeur ===\n" + name));
        
        // Informations OS
        System.out.println("\nSystème d'exploitation : " + 
            systemData.getOsName().orElse(System.getProperty("os.name")) + " " + 
            systemData.getOsVersion().orElse(System.getProperty("os.version")));
        System.out.println("Architecture : " + 
            systemData.getOsArchitecture().orElse(System.getProperty("os.arch")));
        
        // Informations Java et runtime
        System.out.println("Version Java : " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Utilisateur : " + System.getProperty("user.name"));
        System.out.println("Répertoire de travail : " + System.getProperty("user.dir"));
        System.out.println("Processeurs disponibles : " + Runtime.getRuntime().availableProcessors());
        System.out.println("Mémoire totale (Java) : " + formatSize(Runtime.getRuntime().totalMemory()));
        System.out.println("Mémoire utilisée (Java) : " + formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        
        // Affichage des informations collectées
        displayCollectedInfo();
    }
    
    // Méthodes utilitaires
    private void clearScreen() {
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
    
    private boolean isCompatibleOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows");
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String formatMAC(byte[] mac) {
        if (mac == null) return "Non disponible";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return sb.toString();
    }

    private void collectCpuInfo() {
        try {
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
                    systemData.setCpuName(cpuLine.trim());
                    break;
                }
            }
            cpuProcess.waitFor();
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des informations CPU : " + e.getMessage());
        }
    }

    private void collectGpuInfo() {
        // Les informations GPU sont affichées directement car SystemData n'a pas de setter GPU pour le moment
    }

    private void collectDiskInfo() {
        try {
            File[] roots = File.listRoots();
            for (File root : roots) {
                if (root.getTotalSpace() > 0) {
                    // Stocker les informations du premier disque trouvé
                    systemData.setDiskSize(root.getTotalSpace());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des informations disque : " + e.getMessage());
        }
    }

    private void collectNetworkInfo() {
        // Les informations réseau sont affichées directement pour le moment
    }

    private void collectWindowsActivationInfo() {
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
                        systemData.setOsCaption(parts[0] + " " + parts[1]);
                        systemData.setOsArchitecture(parts[2]);
                        if (!parts[3].equals("NULL")) {
                            systemData.setOsSerial(parts[3]);
                        }
                        if (!parts[4].equals("NULL")) {
                            systemData.setOsLicense(parts[4]);
                        }
                        break;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des informations Windows : " + e.getMessage());
        }
    }

    private void collectBatteryInfo() {
        // Les informations de batterie sont affichées directement pour le moment
    }

    private void displayCollectedInfo() {
        // Informations CPU
        System.out.println("\n=== Processeur ===");
        systemData.getCpuName().ifPresent(name -> 
            System.out.println("Modèle : " + name));
        System.out.println("Cœurs physiques : " + systemData.getCpuCores());
        System.out.println("Cœurs logiques : " + systemData.getCpuThreads());

        // Informations GPU
        System.out.println("\n=== Cartes Graphiques ===");
        try {
            Process gpuProcess = Runtime.getRuntime().exec("wmic path win32_VideoController get name");
            java.io.BufferedReader gpuReader = new java.io.BufferedReader(new java.io.InputStreamReader(gpuProcess.getInputStream()));
            String gpuLine;
            boolean firstLine = true;
            boolean hasGPU = false;
            while ((gpuLine = gpuReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!gpuLine.trim().isEmpty()) {
                    hasGPU = true;
                    System.out.println(gpuLine.trim());
                }
            }
            if (!hasGPU) {
                System.out.println("Aucune carte graphique détectée");
            }
            gpuProcess.waitFor();
        } catch (Exception e) {
            System.out.println("Erreur GPU : " + e.getMessage());
        }

        // Informations RAM
        System.out.println("\n=== Mémoire RAM ===");
        long totalMemory = systemData.getTotalMemory().orElse(0L);
        long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
        System.out.println("Mémoire totale : " + formatSize(totalMemory));
        System.out.println("Mémoire utilisée : " + formatSize(usedMemory));
        System.out.println("Mémoire libre : " + formatSize(Runtime.getRuntime().freeMemory()));
        double ramUsagePercent = totalMemory > 0 ? ((double)usedMemory / totalMemory) * 100 : 0;
        System.out.println("Utilisation : " + String.format("%.1f%%", ramUsagePercent));
        
        // Barre de progression RAM
        int barLength = 30;
        int filledLength = (int) (ramUsagePercent * barLength / 100);
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("█");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        System.out.println(bar.toString());

        // Informations Disques
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
                
                // Barre de progression disque
                barLength = 30;
                filledLength = (int) (usedPercent * barLength / 100);
                bar = new StringBuilder();
                bar.append("[");
                for (int i = 0; i < barLength; i++) {
                    if (i < filledLength) {
                        bar.append("█");
                    } else {
                        bar.append(" ");
                    }
                }
                bar.append("]");
                System.out.println(bar.toString());
            }
        }

        // Informations Réseau
        System.out.println("\n=== Réseau ===");
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

        // État d'activation Windows
        if (isCompatibleOS()) {
            System.out.println("\n=== État d'Activation Windows ===");
            systemData.getOsCaption().ifPresentOrElse(
                caption -> System.out.println("Système : " + caption),
                () -> System.out.println("Impossible de déterminer l'état d'activation de Windows")
            );
            systemData.getOsSerial().ifPresent(serial -> 
                System.out.println("Numéro de série : " + serial));
            systemData.getOsLicense().ifPresent(license -> 
                System.out.println("Date d'activation : " + license));
        }

        // Informations sur la batterie
        if (isCompatibleOS()) {
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
    }

    private void displayPerformanceInfo() {
        System.out.println("\n=== Performances Système ===");
        try {
            // Score de performance Windows
            ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-Command",
                "Get-WmiObject -Class Win32_WinSAT | Select-Object CPUScore,MemoryScore,DiskScore,GraphicsScore"
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
                    String[] scores = line.trim().split("\\s+");
                    if (scores.length >= 4) {
                        System.out.println("Score CPU : " + scores[0]);
                        System.out.println("Score Mémoire : " + scores[1]);
                        System.out.println("Score Disque : " + scores[2]);
                        System.out.println("Score Graphique : " + scores[3]);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Impossible de récupérer les scores de performance");
        }
    }

    private void displaySecurityInfo() {
        System.out.println("\n=== Sécurité ===");
        try {
            // État de l'antivirus
            ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-Command",
                "Get-WmiObject -Namespace root/SecurityCenter2 -Class AntiVirusProduct | Select-Object displayName,productState"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            boolean firstLine = true;
            boolean hasAntivirus = false;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    hasAntivirus = true;
                    String[] parts = line.trim().split("\\s+", 2);
                    if (parts.length >= 2) {
                        System.out.println("Antivirus : " + parts[0]);
                        System.out.println("État : " + (parts[1].contains("1") ? "Actif" : "Inactif"));
                    }
                }
            }
            if (!hasAntivirus) {
                System.out.println("Aucun antivirus détecté");
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Impossible de récupérer les informations de sécurité");
        }
    }

    private void displayDeviceInfo() {
        System.out.println("\n=== Périphériques ===");
        try {
            // Périphériques USB
            ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-Command",
                "Get-WmiObject Win32_USBHub | Select-Object Name,Status"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            boolean firstLine = true;
            boolean hasUSB = false;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    hasUSB = true;
                    String[] parts = line.trim().split("\\s+", 2);
                    if (parts.length >= 2) {
                        System.out.println("USB : " + parts[0]);
                        System.out.println("État : " + parts[1]);
                    }
                }
            }
            if (!hasUSB) {
                System.out.println("Aucun périphérique USB détecté");
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Impossible de récupérer les informations des périphériques");
        }
    }

    private void displayNetworkInfo() {
        System.out.println("\n=== Réseau ===");
        try {
            // Informations réseau détaillées
            ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-Command",
                "Get-NetAdapter | Where-Object Status -eq 'Up' | Select-Object Name,InterfaceDescription,LinkSpeed"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            boolean firstLine = true;
            boolean hasNetwork = false;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    hasNetwork = true;
                    String[] parts = line.trim().split("\\s+", 3);
                    if (parts.length >= 3) {
                        System.out.println("\nInterface : " + parts[0]);
                        System.out.println("Description : " + parts[1]);
                        System.out.println("Vitesse : " + parts[2]);
                    }
                }
            }
            if (!hasNetwork) {
                System.out.println("Aucune interface réseau active");
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Impossible de récupérer les informations réseau");
        }
    }

    private void displayStorageInfo() {
        System.out.println("\n=== Stockage ===");
        File[] roots = File.listRoots();
        for (File root : roots) {
            if (root.getTotalSpace() > 0) {
                System.out.println("\n" + root.getPath());
                System.out.println("Espace total : " + formatSize(root.getTotalSpace()));
                System.out.println("Espace libre : " + formatSize(root.getFreeSpace()));
                System.out.println("Espace utilisé : " + formatSize(root.getTotalSpace() - root.getFreeSpace()));
                double usedPercent = ((double)(root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace()) * 100;
                System.out.println("Utilisé : " + String.format("%.1f%%", usedPercent));
                
                // Barre de progression
                int barLength = 30;
                int filledLength = (int) (usedPercent * barLength / 100);
                StringBuilder bar = new StringBuilder();
                bar.append("[");
                for (int i = 0; i < barLength; i++) {
                    if (i < filledLength) {
                        bar.append("█");
                    } else {
                        bar.append(" ");
                    }
                }
                bar.append("]");
                System.out.println(bar.toString());
            }
        }
    }

    private void displayWindowsInfo() {
        if (isCompatibleOS()) {
            System.out.println("\n=== Windows ===");
            try {
                // Informations Windows détaillées
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-Command",
                    "Get-WmiObject -Class Win32_OperatingSystem | Select-Object LastBootUpTime,InstallDate,Version"
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
                        String[] parts = line.trim().split("\\s+", 3);
                        if (parts.length >= 3) {
                            System.out.println("Dernier démarrage : " + parts[0]);
                            System.out.println("Date d'installation : " + parts[1]);
                            System.out.println("Version : " + parts[2]);
                        }
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                System.out.println("Impossible de récupérer les informations Windows");
            }
        }
    }
}

