package com.pcpeek;

import java.time.Clock;

// Imports pour Spring Boot et Vaadin
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Theme("default")
public class Main implements AppShellConfigurator {

    public static void main(String[] args) {
        System.out.println("=== PC Peek - Système de Monitoring ===\n");
        
        // Vérifier les arguments de lancement
        if (hasCliArgument(args)) {
            startCLI();
        } else {
            startGUI(args);
        }
    }

    private static boolean hasCliArgument(String[] args) {
        return args.length > 0 && ("--cli".equals(args[0]) || "-c".equals(args[0]) || "cli".equals(args[0]));
    }

    private static void startCLI() {
        try {
            com.pcpeek.cli.CLIApplication cliApp = new com.pcpeek.cli.CLIApplication();
            cliApp.run();
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du mode console : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startGUI(String[] args) {
        System.out.println("🌐 Mode Interface Web Activé");
        System.out.println("Démarrage du serveur web...");
        System.out.println("L'application sera accessible sur : http://localhost:8080\n");
        
        try {
            SpringApplication.run(Main.class, args);
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du mode web : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}