package com.pcpeek.cli;

import com.pcpeek.SystemData;
import com.pcpeek.cli.modes.StaticInfoMode;
import com.pcpeek.cli.modes.RealTimeMode;
import com.pcpeek.cli.modes.TemperatureMode;
import java.util.Scanner;

public class CLIMenuHandler {
    private final SystemData systemData;
    private final StaticInfoMode staticMode;
    private final RealTimeMode realTimeMode;
    private final TemperatureMode temperatureMode;
    
    public CLIMenuHandler(SystemData systemData) {
        this.systemData = systemData;
        this.staticMode = new StaticInfoMode(systemData);
        this.realTimeMode = new RealTimeMode(systemData);
        this.temperatureMode = new TemperatureMode(systemData);
    }
    
    public void showMainMenu(Scanner scanner) {
        boolean running = true;

        while (running) {
            try {
                displayMenu();
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consommer le retour à la ligne
                
                if (choice == 4) {
                    running = false;
                    System.out.println("Au revoir !");
                } else {
                    handleMenuChoice(choice, scanner);
                }
            } catch (Exception e) {
                System.err.println("Erreur : " + e.getMessage());
                scanner.nextLine(); // Consommer l'entrée invalide
            }
        }
    }
    
    private void displayMenu() {
        System.out.println("\n=== PC-Peek ===");
        System.out.println("1. Mode Statique (snapshot complet)");
        System.out.println("2. Mode Real Time (informations dynamiques)");
        System.out.println("3. Mode Températures (affichage en temps réel)");
        System.out.println("4. Quitter");
        System.out.print("\nVotre choix : ");
    }

    private void handleMenuChoice(int choice, Scanner scanner) {
        switch (choice) {
            case 1:
                staticMode.execute(scanner);
                break;
            case 2:
                realTimeMode.execute(scanner);
                break;
            case 3:
                temperatureMode.execute(scanner);
                break;
            case 4:
                System.out.println("Au revoir !");
                System.exit(0);
                break;
            default:
                System.out.println("Choix invalide !");
        }
    }
}
