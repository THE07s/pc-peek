package com.pcpeek.src;// src/HWInfo.java
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class HWInfo {
    public static void displayHWInfo() {
        displayJVMMemory();
        displayDisks();
        displayCPUInfo();
        displayRAMInfo();
        displayBIOSInfo();
        displayPCBrandInfo();
        displayGPUInfo();
        displayPowerStatus();
        displayWindowsActivationStatus();
    }

    private static void displayJVMMemory() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        System.out.println("=== Mémoire JVM ===");
        System.out.println("Mémoire utilisée : " + usedMemory / (1024 * 1024) + " MB");
        System.out.println("Mémoire libre    : " + freeMemory / (1024 * 1024) + " MB");
        System.out.println("Mémoire max      : " + maxMemory / (1024 * 1024) + " MB");
    }

    private static void displayDisks() {
        System.out.println("\n=== Disques ===");
        File[] roots = File.listRoots();
        for (File root : roots) {
            System.out.println("Partition : " + root.getAbsolutePath());
            System.out.println("  Total       : " + root.getTotalSpace() / (1024 * 1024) + " MB");
            System.out.println("  Libre       : " + root.getFreeSpace() / (1024 * 1024) + " MB");
        }
    }

    private static void displayCPUInfo() {
        System.out.println("\n=== Informations CPU ===");
        runWMIC("cpu get Name");
        runWMIC("cpu get Manufacturer");
        runWMIC("cpu get NumberOfCores,NumberOfLogicalProcessors");
        runWMIC("cpu get LoadPercentage");
    }

    private static void displayRAMInfo() {
        System.out.println("\n=== RAM Système ===");
        runWMIC("OS get FreePhysicalMemory,TotalVisibleMemorySize /Value");
    }

    private static void displayBIOSInfo() {
        System.out.println("\n=== BIOS ===");
        runWMIC("bios get SMBIOSBIOSVersion,Manufacturer,ReleaseDate");
    }

    private static void displayPCBrandInfo() {
        System.out.println("\n=== Fabricant / Modèle PC ===");
        runWMIC("computersystem get Manufacturer,Model");
        runWMIC("csproduct get UUID");
    }

    private static void displayGPUInfo() {
        System.out.println("\n=== Carte Graphique (GPU) ===");
        runWMIC("path win32_VideoController get Name");
        System.out.println("--- Mémoire Vidéo (VRAM) ---");
        try {
            Process process = Runtime.getRuntime().exec("wmic path win32_VideoController get AdapterRAM");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+")) {
                    long vramBytes = Long.parseLong(line.trim());
                    long vramMB = vramBytes / (1024 * 1024);
                    System.out.println("VRAM : " + vramMB + " MB");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Erreur VRAM : " + e.getMessage());
        }
    }

    private static void displayPowerStatus() {
        System.out.println("\n=== Alimentation / Batterie ===");
        try {
            Process process = Runtime.getRuntime().exec("wmic path Win32_Battery get EstimatedChargeRemaining");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().matches("\\d+")) {
                    System.out.println("Charge batterie : " + line.trim() + " % ");
                    return;
                }
            }
            System.out.println("Aucune batterie détectée (PC fixe ?)");
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Erreur batterie : " + e.getMessage());
        }
    }

    private static void displayWindowsActivationStatus() {
        System.out.println("\n=== Activation de Windows ===");
        try {
            Process process = Runtime.getRuntime().exec(
                    "wmic path SoftwareLicensingProduct where \"PartialProductKey is not null\" get LicenseStatus");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("1")) {
                    System.out.println("Windows est activé!");
                    return;
                } else if (line.trim().equals("0")) {
                    System.out.println("Windows n'est pas activé");
                    return;
                }
            }
            System.out.println("Impossible de déterminer le statut d'activation.");
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Erreur activation Windows : " + e.getMessage());
        }
    }

    private static void runWMIC(String command) {
        try {
            Process process = Runtime.getRuntime().exec("wmic " + command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty())
                    System.out.println(line.trim());
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Erreur lors de l'exécution : " + e.getMessage());
        }
    }
}