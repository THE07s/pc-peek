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
            System.out.println("║ 3. Mode Température CPU            ║");
            System.out.println("║ 4. Quitter                         ║");
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
                        startCPUTempMode(scanner);
                        break;
                    case 4:
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
        // Vérifier le système d'exploitation
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("linux")) {
            System.out.println("\n⚠️ ATTENTION: OpenHardwareMonitor n'est pas compatible avec votre système d'exploitation.");
            System.out.println("OpenHardwareMonitor est un programme Windows (.exe) qui ne fonctionne que sur Windows.");
            System.out.println("\nSystème détecté: " + System.getProperty("os.name"));
            System.out.println("Le mode Temps Réel (RT) n'est pas disponible sur votre système.");
            System.out.println("Veuillez utiliser le mode Statique à la place.");
            return null;
        }

        // Chemin absolu vers OpenHardwareMonitor
        String absolutePath = "D:\\DATA - Java E3e\\pc-peek\\src\\main\\java\\com\\pcpeek\\OpenHardwareMonitor\\OpenHardwareMonitor.exe";
        File file = new File(absolutePath);
        
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        // Si non trouvé, afficher un message
        System.out.println("\nOpenHardwareMonitor.exe non trouvé!");
        System.out.println("Chemin attendu: " + absolutePath);
        return null;
    }

    private static void startRTMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Temps Réel ===");
        
        // Vérifier le système d'exploitation avant de continuer
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("linux")) {
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

            // Lance OpenHardwareMonitor
            try {
                ProcessBuilder pb = new ProcessBuilder(ohmPath);
                // Définir le répertoire de travail comme étant le dossier parent d'OpenHardwareMonitor
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

    private static void startCPUTempMode(Scanner scanner) {
        clearScreen();
        System.out.println("=== Mode Température CPU ===");
        
        // Vérifier le système d'exploitation
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("linux")) {
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

            // Initialise le moniteur
            SystemMonitor monitor = new SystemMonitor();
            HWMonitor hwMonitor = new HWMonitor(monitor.getSystemInfo());

            // Variable pour contrôler la boucle de mise à jour
            final boolean[] monitoring = {true};
            
            // Thread pour la mise à jour de la température
            Thread updateThread = new Thread(() -> {
                while (monitoring[0]) {
                    try {
                        // Nettoyer l'écran et repositionner le curseur
                        clearScreen();
                        
                        // Construire tout l'affichage dans un StringBuilder
                        StringBuilder display = new StringBuilder();
                        display.append("=== Mode Température CPU ===\n");
                        display.append("Appuyez sur Entrée pour revenir au menu...\n\n");
                        
                        // Afficher la température CPU
                        double temp = hwMonitor.getCpuTemperature();
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

                        // Afficher tout d'un coup
                        System.out.print(display.toString());
                        System.out.flush();

                        // Attendre 1 seconde
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        if (monitoring[0]) {
                            System.err.println("Erreur lors de la mise à jour: " + e.getMessage());
                        }
                    }
                }
            });

            // Démarrer le thread de mise à jour
            updateThread.start();

            // Attendre que l'utilisateur appuie sur Entrée
            scanner.nextLine();
            
            // Arrêter le thread de mise à jour
            monitoring[0] = false;
            updateThread.join(1000); // Attendre que le thread se termine (max 1 seconde)

        } catch (Exception e) {
            System.err.println("Erreur lors du mode température: " + e.getMessage());
        } finally {
            // S'assurer qu'OpenHardwareMonitor est fermé
            stopOpenHardwareMonitor();
        }
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
}