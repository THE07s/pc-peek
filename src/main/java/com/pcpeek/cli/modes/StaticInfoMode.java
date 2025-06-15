package com.pcpeek.cli.modes;

import com.pcpeek.SystemData;
import com.pcpeek.monitors.staticinfo.OSLevelMonitor;
import com.pcpeek.monitors.staticinfo.HardwareLevelMonitor;
import java.util.Scanner;
import java.io.File;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Enumeration;

public class StaticInfoMode {
    private final SystemData systemData;
    private final OSLevelMonitor osMonitor;
    private final HardwareLevelMonitor hwMonitor;
    
    public StaticInfoMode(SystemData systemData) {
        this.systemData = systemData;
        this.osMonitor = new OSLevelMonitor();
        this.hwMonitor = new HardwareLevelMonitor();
    }
    
    public void execute(Scanner scanner) {
        clearScreen();
        System.out.println("=== Informations STATIC ===");
        System.out.println("Affichage des informations système détaillées...\n");

        // Mettre à jour SystemData avec les données des moniteurs
        updateSystemData();
        
        // Afficher les informations depuis SystemData
        displaySystemInfo();
        displayHardwareInfo();
        displayNetworkInfo();
        displayDiskInfo();

        System.out.println("\n=== Options ===");
        System.out.println("1. Revenir au menu principal");
        System.out.println("2. Mode surveillance continue (mise à jour temps réel)");
        System.out.print("Votre choix (ou Entrée pour option 1) : ");
        
        String choice = scanner.nextLine().trim();
        if ("2".equals(choice)) {
            startWatchMode(scanner);
        }
    }
    
    private void startWatchMode(Scanner scanner) {
        System.out.println("\n=== Mode Surveillance Continue ===");
        System.out.println("Mise à jour toutes les 3 secondes... (Appuyez sur Entrée pour arrêter)");
        
        // Thread pour vérifier l'entrée utilisateur
        Thread inputThread = new Thread(() -> {
            try {
                scanner.nextLine();
            } catch (Exception e) {
                // Ignorer
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();

        while (inputThread.isAlive()) {
            try {
                clearScreen();
                
                System.out.println("=== Informations STATIC - Mode Surveillance ===");
                System.out.println("Mise à jour temps réel... (Appuyez sur Entrée pour arrêter)\n");
                
                // Mettre à jour SystemData avec les nouvelles données
                updateSystemData();
                
                // Afficher les informations mises à jour
                displaySystemInfo();
                displayHardwareInfo();
                displayNetworkInfo();
                displayDiskInfo();
                
                System.out.println("\n[Dernière mise à jour: " + 
                    java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                    ) + "]");
                
                Thread.sleep(3000); // Attendre 3 secondes
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erreur lors de la mise à jour: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void updateSystemData() {
        try {
            // Récupérer les données statiques des moniteurs et les stocker dans SystemData
            systemData.updateStaticData(osMonitor.getSystemInfo());
            systemData.updateStaticData(hwMonitor.getSystemInfo());
            
            // Récupérer et mettre à jour les données dynamiques aussi pour cohérence
            com.pcpeek.monitors.dynamicinfo.ProbeMonitor probeMonitor = new com.pcpeek.monitors.dynamicinfo.ProbeMonitor();
            com.pcpeek.monitors.dynamicinfo.ResourceMonitor resourceMonitor = new com.pcpeek.monitors.dynamicinfo.ResourceMonitor();
            
            systemData.updateDynamicData(probeMonitor.getProbeInfo());
            systemData.updateDynamicData(resourceMonitor.getResourceInfo());
            
            // Ajouter des informations Java supplémentaires
            systemData.setOsName(System.getProperty("os.name"));
            systemData.setOsVersion(systemData.getOsVersion().orElse(System.getProperty("os.version")));
            systemData.setOsArchitecture(systemData.getOsArchitecture().orElse(System.getProperty("os.arch")));
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des données: " + e.getMessage());
        }
    }
    
    private void displaySystemInfo() {
        System.out.println("=== Informations Système ===");
        
        // Check if we have errors from monitors
        if (systemData.hasData("error")) {
            System.out.println("Erreur lors de la récupération des informations système : " + 
                             systemData.getString("error").orElse("Erreur inconnue"));
        }
        
        // Informations OS depuis SystemData
        systemData.getOsCaption().ifPresentOrElse(
            caption -> System.out.println("Système : " + caption),
            () -> System.out.println("Système d'exploitation : " + systemData.getOsName().orElse("Inconnu"))
        );
        
        systemData.getOsVersion().ifPresent(version -> 
            System.out.println("Version : " + version));
            
        systemData.getOsArchitecture().ifPresent(arch -> 
            System.out.println("Architecture : " + arch));
        
        // Informations fabricant/modèle système
        systemData.getSystemManufacturer().ifPresent(manufacturer -> 
            System.out.println("Fabricant : " + manufacturer));
            
        systemData.getSystemModel().ifPresent(model -> 
            System.out.println("Modèle : " + model));
            
        systemData.getSystemType().ifPresent(type -> 
            System.out.println("Type : " + type));
        
        // Informations OS avancées
        systemData.getOsSerial().ifPresent(serial -> 
            System.out.println("Numéro de série OS : " + serial));
            
        systemData.getOsLicense().ifPresent(license -> 
            System.out.println("Licence : " + license));
            
        // Informations Java/Runtime
        System.out.println("Version Java : " + System.getProperty("java.version") + 
                          " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Utilisateur : " + System.getProperty("user.name"));
        System.out.println("Répertoire de travail : " + System.getProperty("user.dir"));
        System.out.println("Processeurs disponibles : " + Runtime.getRuntime().availableProcessors());
        
        // Temps de fonctionnement système
        systemData.getSystemUptime().ifPresent(uptime -> 
            System.out.println("Temps de fonctionnement système : " + formatUptime(uptime)));
            
        systemData.getBootTime().ifPresent(bootTime -> 
            System.out.println("Heure de démarrage : " + bootTime));
        
        // Mémoire Java (pour référence)
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("Mémoire JVM totale : " + formatSize(totalMemory));
        System.out.println("Mémoire JVM utilisée : " + formatSize(usedMemory));
    }
    
    private void displayHardwareInfo() {
        // CPU - Ajouter plus d'informations
        systemData.getCpuName().ifPresentOrElse(name -> {
            System.out.println("\n=== Processeur ===");
            System.out.println("Modèle : " + name);
        }, () -> {
            // Fallback vers processor_name si cpu.name n'est pas disponible
            systemData.getProcessorName().ifPresent(processorName -> {
                System.out.println("\n=== Processeur ===");
                System.out.println("Modèle : " + processorName);
            });
        });
        
        systemData.getCpuCores().ifPresent(cores -> 
            System.out.println("Cœurs physiques : " + cores));
            
        systemData.getCpuThreads().ifPresent(threads -> 
            System.out.println("Threads logiques : " + threads));
            
        systemData.getCpuCurrentSpeed().ifPresent(speed -> 
            System.out.println("Fréquence actuelle : " + speed + " MHz"));
            
        systemData.getCpuMaxSpeed().ifPresent(speed -> 
            System.out.println("Fréquence maximale : " + speed + " MHz"));
        
        // Mémoire système (formatée correctement)
        if (systemData.getMemoryTotal().isPresent()) {
            System.out.println("\n=== Mémoire Système ===");
            long totalMem = systemData.getMemoryTotal().get();
            System.out.println("Capacité totale : " + formatSize(totalMem));
            
            // Afficher mémoire libre/occupée si disponible
            systemData.getMemoryFree().ifPresent(freeMem -> {
                long usedMem = totalMem - freeMem;
                double usedPercent = (usedMem * 100.0) / totalMem;
                double freePercent = (freeMem * 100.0) / totalMem;
                
                System.out.println("Mémoire occupée : " + formatSize(usedMem) + 
                                 String.format(" (%.1f%%)", usedPercent));
                System.out.println("Mémoire libre : " + formatSize(freeMem) + 
                                 String.format(" (%.1f%%)", freePercent));
            });
            
            // Informations techniques mémoire
            systemData.getMemorySpeed().ifPresent(speed -> 
                System.out.println("Vitesse : " + speed));
                
            systemData.getMemoryManufacturer().ifPresent(manufacturer -> 
                System.out.println("Fabricant : " + manufacturer));
                
            systemData.getMemoryPart().ifPresent(part -> 
                System.out.println("Référence : " + part));
        } else {
            // Fallback vers getTotalMemory/getAvailableMemory si memory.total n'est pas disponible
            systemData.getTotalMemory().ifPresent(totalMem -> {
                System.out.println("\n=== Mémoire Système ===");
                System.out.println("Capacité totale : " + formatSize(totalMem));
                
                systemData.getAvailableMemory().ifPresent(availableMem -> {
                    long usedMem = totalMem - availableMem;
                    double usedPercent = (usedMem * 100.0) / totalMem;
                    double freePercent = (availableMem * 100.0) / totalMem;
                    
                    System.out.println("Mémoire occupée : " + formatSize(usedMem) + 
                                     String.format(" (%.1f%%)", usedPercent));
                    System.out.println("Mémoire disponible : " + formatSize(availableMem) + 
                                     String.format(" (%.1f%%)", freePercent));
                });
            });
        }
        
        // Carte mère - Ajouter plus d'informations
        if (systemData.getBoardManufacturer().isPresent() || systemData.getBoardModel().isPresent()) {
            System.out.println("\n=== Carte Mère ===");
            systemData.getBoardManufacturer().ifPresent(manufacturer -> 
                System.out.println("Fabricant : " + manufacturer));
            systemData.getBoardModel().ifPresent(model -> 
                System.out.println("Modèle : " + model));
            systemData.getBoardVersion().ifPresent(version -> 
                System.out.println("Version : " + version));
            systemData.getBoardSerial().ifPresent(serial -> 
                System.out.println("Numéro de série : " + serial));
        }
        
        // Stockage principal
        if (systemData.getDiskModel().isPresent() || systemData.getDiskSize().isPresent()) {
            System.out.println("\n=== Stockage Principal ===");
            systemData.getDiskModel().ifPresent(model -> 
                System.out.println("Modèle : " + model));
            systemData.getDiskSize().ifPresent(size -> 
                System.out.println("Capacité : " + formatSize(size)));
            systemData.getDiskType().ifPresent(type -> 
                System.out.println("Type : " + type));
            systemData.getDiskStatus().ifPresent(status -> 
                System.out.println("État : " + status));
        }
    }
    
    private void displayNetworkInfo() {
        System.out.println("\n=== Informations Réseau ===");
        try {
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            System.out.println("Nom d'hôte : " + hostname);
            
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp()) {
                    System.out.println("\nInterface : " + networkInterface.getName());
                    System.out.println("  Nom : " + networkInterface.getDisplayName());
                    
                    byte[] mac = networkInterface.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder macAddr = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            macAddr.append(String.format("%02X", mac[i]));
                            if (i < mac.length - 1) {
                                macAddr.append("-");
                            }
                        }
                        System.out.println("  Adresse MAC : " + macAddr.toString());
                    } else {
                        System.out.println("  Adresse MAC : Non disponible");
                    }
                    
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        System.out.println("  Adresse IP : " + address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération des informations réseau : " + e.getMessage());
        }
    }
    
    private void displayDiskInfo() {
        System.out.println("\n=== Disques ===");
        
        // Afficher les informations de disque depuis SystemData si disponibles
        if (systemData.getDiskModel().isPresent() || systemData.getDiskSize().isPresent()) {
            systemData.getDiskModel().ifPresent(model -> 
                System.out.println("Modèle : " + model));
            systemData.getDiskSize().ifPresent(size -> 
                System.out.println("Taille : " + formatSize(size)));
            systemData.getDiskType().ifPresent(type -> 
                System.out.println("Type : " + type));
            systemData.getDiskStatus().ifPresent(status -> 
                System.out.println("État : " + status));
        } else {
            // Afficher les informations de base des disques
            File[] roots = File.listRoots();
            for (File root : roots) {
                System.out.println("Lecteur : " + root.getAbsolutePath());
                System.out.println("  Espace total : " + formatSize(root.getTotalSpace()));
                System.out.println("  Espace libre : " + formatSize(root.getFreeSpace()));
                System.out.println("  Espace utilisé : " + formatSize(root.getTotalSpace() - root.getFreeSpace()));
            }
        }
        
        if (systemData.hasData("error")) {
            System.out.println("Erreur lors de la récupération des informations disque");
        }
    }
    
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String formatUptime(long uptimeSeconds) {
        long days = uptimeSeconds / (24 * 3600);
        long hours = (uptimeSeconds % (24 * 3600)) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        StringBuilder uptime = new StringBuilder();
        if (days > 0) {
            uptime.append(days).append(" jour").append(days > 1 ? "s" : "").append(" ");
        }
        if (hours > 0) {
            uptime.append(hours).append(" heure").append(hours > 1 ? "s" : "").append(" ");
        }
        if (minutes > 0) {
            uptime.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        }
        if (seconds > 0 || uptime.length() == 0) {
            uptime.append(seconds).append(" seconde").append(seconds > 1 ? "s" : "");
        }
        
        return uptime.toString().trim();
    }
}
