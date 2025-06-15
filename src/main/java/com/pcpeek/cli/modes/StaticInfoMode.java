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

        System.out.println("\nAppuyez sur Entrée pour revenir au menu principal...");
        scanner.nextLine();
    }

    private void updateSystemData() {
        try {
            // Récupérer les données statiques des moniteurs et les stocker dans SystemData
            // Utiliser les méthodes standardisées de Monitor
            osMonitor.update();
            hwMonitor.update();
            systemData.updateStaticData(osMonitor.getSystemInfo());
            systemData.updateStaticData(hwMonitor.getSystemInfo());

            // Récupérer et mettre à jour les données dynamiques aussi pour cohérence
            com.pcpeek.monitors.dynamicinfo.ProbeMonitor probeMonitor = new com.pcpeek.monitors.dynamicinfo.ProbeMonitor();
            com.pcpeek.monitors.dynamicinfo.ResourceMonitor resourceMonitor = new com.pcpeek.monitors.dynamicinfo.ResourceMonitor();

            // Utiliser les méthodes standardisées de Monitor
            probeMonitor.update();
            resourceMonitor.update();
            systemData.updateDynamicData(probeMonitor.getSystemInfo());
            systemData.updateDynamicData(resourceMonitor.getSystemInfo());

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
        if (systemData.getOsCaption().isPresent()) {
            String caption = systemData.getOsCaption().get();
            System.out.println("Système : " + caption);
        } else {
            String osName = systemData.getOsName().isPresent() ? systemData.getOsName().get() : "Inconnu";
            System.out.println("Système d'exploitation : " + osName);
        }

        if (systemData.getOsVersion().isPresent()) {
            String version = systemData.getOsVersion().get();
            System.out.println("Version : " + version);
        }

        if (systemData.getOsArchitecture().isPresent()) {
            String arch = systemData.getOsArchitecture().get();
            System.out.println("Architecture : " + arch);
        }

        // Informations fabricant/modèle système
        if (systemData.getSystemManufacturer().isPresent()) {
            String manufacturer = systemData.getSystemManufacturer().get();
            System.out.println("Fabricant : " + manufacturer);
        }

        if (systemData.getSystemModel().isPresent()) {
            String model = systemData.getSystemModel().get();
            System.out.println("Modèle : " + model);
        }

        if (systemData.getSystemType().isPresent()) {
            String type = systemData.getSystemType().get();
            System.out.println("Type : " + type);
        }

        // Informations OS avancées
        if (systemData.getOsSerial().isPresent()) {
            String serial = systemData.getOsSerial().get();
            System.out.println("Numéro de série OS : " + serial);
        }

        if (systemData.getOsLicense().isPresent()) {
            String license = systemData.getOsLicense().get();
            System.out.println("Licence : " + license);
        }

        // Informations Java/Runtime
        System.out.println("Version Java : " + System.getProperty("java.version") +
                " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Utilisateur : " + System.getProperty("user.name"));
        System.out.println("Répertoire de travail : " + System.getProperty("user.dir"));
        System.out.println("Processeurs disponibles : " + Runtime.getRuntime().availableProcessors());

        // Temps de fonctionnement système
        if (systemData.getSystemUptime().isPresent()) {
            Long uptime = systemData.getSystemUptime().get();
            System.out.println("Temps de fonctionnement système : " + formatUptime(uptime));
        }

        if (systemData.getBootTime().isPresent()) {
            String bootTime = systemData.getBootTime().get();
            System.out.println("Heure de démarrage : " + bootTime);
        }

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
        if (systemData.getCpuName().isPresent()) {
            String name = systemData.getCpuName().get();
            System.out.println("\n=== Processeur ===");
            System.out.println("Modèle : " + name);
        } else {
            // Fallback vers processor_name si cpu.name n'est pas disponible
            if (systemData.getProcessorName().isPresent()) {
                String processorName = systemData.getProcessorName().get();
                System.out.println("\n=== Processeur ===");
                System.out.println("Modèle : " + processorName);
            }
        }

        if (systemData.getCpuCores().isPresent()) {
            Long cores = systemData.getCpuCores().get();
            System.out.println("Cœurs physiques : " + cores);
        }

        if (systemData.getCpuThreads().isPresent()) {
            Long threads = systemData.getCpuThreads().get();
            System.out.println("Threads logiques : " + threads);
        }

        if (systemData.getCpuCurrentSpeed().isPresent()) {
            Long speed = systemData.getCpuCurrentSpeed().get();
            System.out.println("Fréquence actuelle : " + speed + " MHz");
        }

        if (systemData.getCpuMaxSpeed().isPresent()) {
            Long speed = systemData.getCpuMaxSpeed().get();
            System.out.println("Fréquence maximale : " + speed + " MHz");
        }

        // Mémoire système (formatée correctement)
        if (systemData.getMemoryTotal().isPresent()) {
            System.out.println("\n=== Mémoire Système ===");
            long totalMem = systemData.getMemoryTotal().get();
            System.out.println("Capacité totale : " + formatSize(totalMem));

            // Afficher mémoire libre/occupée si disponible
            if (systemData.getMemoryFree().isPresent()) {
                long freeMem = systemData.getMemoryFree().get();
                long usedMem = totalMem - freeMem;
                double usedPercent = (usedMem * 100.0) / totalMem;
                double freePercent = (freeMem * 100.0) / totalMem;

                System.out.println("Mémoire occupée : " + formatSize(usedMem) +
                        String.format(" (%.1f%%)", usedPercent));
                System.out.println("Mémoire libre : " + formatSize(freeMem) +
                        String.format(" (%.1f%%)", freePercent));
            }

            // Informations techniques mémoire
            if (systemData.getMemorySpeed().isPresent()) {
                Object speed = systemData.getMemorySpeed().get();
                System.out.println("Vitesse : " + speed);
            }

            if (systemData.getMemoryManufacturer().isPresent()) {
                String manufacturer = systemData.getMemoryManufacturer().get();
                System.out.println("Fabricant : " + manufacturer);
            }

            if (systemData.getMemoryPart().isPresent()) {
                String part = systemData.getMemoryPart().get();
                System.out.println("Référence : " + part);
            }
        } else {
            // Fallback vers getTotalMemory/getAvailableMemory si memory.total n'est pas
            // disponible
            if (systemData.getTotalMemory().isPresent()) {
                long totalMem = systemData.getTotalMemory().get();
                System.out.println("\n=== Mémoire Système ===");
                System.out.println("Capacité totale : " + formatSize(totalMem));

                if (systemData.getAvailableMemory().isPresent()) {
                    long availableMem = systemData.getAvailableMemory().get();
                    long usedMem = totalMem - availableMem;
                    double usedPercent = (usedMem * 100.0) / totalMem;
                    double freePercent = (availableMem * 100.0) / totalMem;

                    System.out.println("Mémoire occupée : " + formatSize(usedMem) +
                            String.format(" (%.1f%%)", usedPercent));
                    System.out.println("Mémoire disponible : " + formatSize(availableMem) +
                            String.format(" (%.1f%%)", freePercent));
                }
            }
        }

        // Carte mère - Ajouter plus d'informations
        if (systemData.getBoardManufacturer().isPresent() || systemData.getBoardModel().isPresent()) {
            System.out.println("\n=== Carte Mère ===");
            if (systemData.getBoardManufacturer().isPresent()) {
                String manufacturer = systemData.getBoardManufacturer().get();
                System.out.println("Fabricant : " + manufacturer);
            }
            if (systemData.getBoardModel().isPresent()) {
                String model = systemData.getBoardModel().get();
                System.out.println("Modèle : " + model);
            }
            if (systemData.getBoardVersion().isPresent()) {
                String version = systemData.getBoardVersion().get();
                System.out.println("Version : " + version);
            }
            if (systemData.getBoardSerial().isPresent()) {
                String serial = systemData.getBoardSerial().get();
                System.out.println("Numéro de série : " + serial);
            }
        }

        // Stockage principal
        if (systemData.getDiskModel().isPresent() || systemData.getDiskSize().isPresent()) {
            System.out.println("\n=== Stockage Principal ===");
            if (systemData.getDiskModel().isPresent()) {
                String model = systemData.getDiskModel().get();
                System.out.println("Modèle : " + model);
            }
            if (systemData.getDiskSize().isPresent()) {
                Long size = systemData.getDiskSize().get();
                System.out.println("Capacité : " + formatSize(size));
            }
            if (systemData.getDiskType().isPresent()) {
                String type = systemData.getDiskType().get();
                System.out.println("Type : " + type);
            }
            if (systemData.getDiskStatus().isPresent()) {
                String status = systemData.getDiskStatus().get();
                System.out.println("État : " + status);
            }
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
            if (systemData.getDiskModel().isPresent()) {
                String model = systemData.getDiskModel().get();
                System.out.println("Modèle : " + model);
            }
            if (systemData.getDiskSize().isPresent()) {
                Long size = systemData.getDiskSize().get();
                System.out.println("Taille : " + formatSize(size));
            }
            if (systemData.getDiskType().isPresent()) {
                String type = systemData.getDiskType().get();
                System.out.println("Type : " + type);
            }
            if (systemData.getDiskStatus().isPresent()) {
                String status = systemData.getDiskStatus().get();
                System.out.println("État : " + status);
            }
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
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
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
