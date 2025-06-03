package com.pcpeek.src;// src/Main.java
import java.util.Scanner;
import java.io.IOException;
import java.io.File;

public class Main {
    private static Process openHardwareMonitorProcess; // Ajout d'une variable pour suivre le processus
    private static final String OHM_EXE_NAME = "OpenHardwareMonitor.exe";
    private static final String OHM_RELATIVE_PATH = "OpenHardwareMonitor/" + OHM_EXE_NAME;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            clearScreen();
            System.out.println("╔════════════════════════════════════╗");
            System.out.println("║           PC PEEK RTS              ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║ 1. Mode Temps Réel (RT)            ║");
            System.out.println("║ 2. Mode Statique                   ║");
            System.out.println("║ 3. Quitter                         ║");
            System.out.println("╚════════════════════════════════════╝");
            System.out.print("\nVotre choix : ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        startRTMode(scanner);
                        break;
                    case 2:
                        startStaticMode(scanner);
                        break;
                    case 3:
                        // Fermer OpenHardwareMonitor avant de quitter
                        stopOpenHardwareMonitor();
                        running = false;
                        System.out.println("\nAu revoir !");
                        break;
                    default:
                        System.out.println("\nChoix invalide. Appuyez sur Entrée...");
                        scanner.nextLine();
                }
            } catch (NumberFormatException e) {
                System.out.println("\nVeuillez entrer un nombre valide. Appuyez sur Entrée...");
                scanner.nextLine();
            }
        }
        scanner.close();
    }

    private static String findOpenHardwareMonitor() {
        // Chemin relatif depuis le package com.pcpeek
        String path = "com/pcpeek/OpenHardwareMonitor/OpenHardwareMonitor.exe";
        File file = new File(path);
        
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        // Si non trouvé, essayer avec le chemin complet depuis src
        path = "src/main/java/com/pcpeek/OpenHardwareMonitor/OpenHardwareMonitor.exe";
        file = new File(path);
        
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        // Si toujours non trouvé, afficher un message
        System.out.println("\nOpenHardwareMonitor.exe non trouvé!");
        System.out.println("Veuillez vérifier que le fichier existe dans:");
        System.out.println("1. " + new File("com/pcpeek/OpenHardwareMonitor/OpenHardwareMonitor.exe").getAbsolutePath());
        System.out.println("2. " + new File("src/main/java/com/pcpeek/OpenHardwareMonitor/OpenHardwareMonitor.exe").getAbsolutePath());
        return null;
    }

    private static void startRTMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Temps Réel ===");
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

            // Lance OpenHardwareMonitor
            try {
                ProcessBuilder pb = new ProcessBuilder(ohmPath);
                pb.directory(new File(ohmPath).getParentFile());
                openHardwareMonitorProcess = pb.start();
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("Erreur lors du lancement d'OpenHardwareMonitor: " + e.getMessage());
                System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
                scanner.nextLine();
                return;
            }

            // Initialise et affiche les informations en temps réel
            SystemMonitor monitor = new SystemMonitor();
            HWMonitor hwMonitor = new HWMonitor(monitor.getSystemInfo());

            boolean monitoring = true;
            while (monitoring) {
                clearScreen();
                System.out.println("=== Mode Temps Réel ===");
                System.out.println("Appuyez sur Entrée pour revenir au menu...");
                hwMonitor.printDetailedInfo();

                // Attendre l'entrée de l'utilisateur
                if (scanner.hasNextLine()) {
                    scanner.nextLine(); // Lire l'entrée (n'importe quelle touche + Entrée)
                    monitoring = false;
                }
                Thread.sleep(100); // Petit délai pour ne pas surcharger le CPU
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du mode RT: " + e.getMessage());
            e.printStackTrace(); // Afficher la stack trace pour le débogage
            System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
            scanner.nextLine();
        }
    }

    private static void startStaticMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Statique ===");
        System.out.println("=== Informations sur l'OS ===");
        OSInfo.displayOSInfo();

        System.out.println("\n=== Informations Matérielles ===");
        HWInfo.displayHWInfo();

        System.out.println("\nAppuyez sur Entrée pour revenir au menu...");
        scanner.nextLine();
    }

    private static void stopOpenHardwareMonitor() {
        try {
            if (openHardwareMonitorProcess != null) {
                openHardwareMonitorProcess.destroy();
            }
            // S'assurer que le processus est bien fermé
            Runtime.getRuntime().exec("taskkill /F /IM OpenHardwareMonitor.exe");
            System.out.println("Arrêt d'OpenHardwareMonitor...");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'arrêt d'OpenHardwareMonitor: " + e.getMessage());
        }
    }

    private static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Si le clear screen échoue, on affiche juste quelques lignes vides
            System.out.println("\n\n\n\n\n");
        }
    }
}