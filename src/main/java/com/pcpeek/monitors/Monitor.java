package com.pcpeek.monitors;

import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Classe abstraite représentant un moniteur système.
 * Cette classe définit les comportements communs pour tous les types de
 * monitoring.
 */
public abstract class Monitor {
    protected Map<String, Object> systemInfo;
    protected boolean isInitialized;
    protected long lastUpdateTime;

    /**
     * Initialise le moniteur avec les informations système de base
     */
    public Monitor() {
        systemInfo = new HashMap<>();
        isInitialized = false;
        lastUpdateTime = 0;
        try {
            systemInfo = initializeSystemInfo();
            isInitialized = true;
        } catch (Exception e) {
            System.err.println("Erreur d'initialisation: " + e.getMessage());
        }
    }

    /**
     * Initialise les informations système de base
     * 
     * @return Map contenant les informations système
     */
    protected abstract Map<String, Object> initializeSystemInfo();

    /**
     * Met à jour les informations du moniteur
     */
    public void update() {
        if (!isInitialized) {
            System.err.println("Le moniteur n'est pas initialisé");
            return;
        }

        try {
            performUpdate();
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("Erreur de mise à jour: " + e.getMessage());
        }
    }

    /**
     * Méthode abstraite pour la mise à jour spécifique à chaque type de moniteur
     */
    protected abstract void performUpdate();

    /**
     * Affiche les informations du moniteur
     */
    public void display() {
        if (!isInitialized) {
            System.err.println("Le moniteur n'est pas initialisé");
            return;
        }

        try {
            System.out.println("\n=== " + getMonitorName() + " ===");
            displayContent();
        } catch (Exception e) {
            System.err.println("Erreur d'affichage: " + e.getMessage());
        }
    }

    /**
     * Affiche le contenu des informations
     */
    protected abstract void displayContent();

    /**
     * Récupère le nom du moniteur
     * 
     * @return Nom du moniteur
     */
    protected abstract String getMonitorName();

    /**
     * Récupère les informations système actuelles
     * 
     * @return Map contenant les informations système
     */
    public Map<String, Object> getSystemInfo() {
        return new HashMap<>(systemInfo);
    }

    /**
     * Vérifie si le système d'exploitation est compatible
     * 
     * @return true si le système est compatible, false sinon
     */
    public boolean isCompatibleOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows");
    }

    /**
     * Formate une taille en octets en une chaîne lisible
     * 
     * @param bytes Taille en octets
     * @return Chaîne formatée
     */
    protected String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Formate un pourcentage
     * 
     * @param value Valeur entre 0 et 1
     * @return Chaîne formatée avec le symbole %
     */
    protected String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * Crée une barre de progression visuelle
     * 
     * @param value  Valeur entre 0 et 1
     * @param length Longueur de la barre
     * @return Barre de progression sous forme de chaîne
     */
    protected String createProgressBar(double value, int length) {
        int filledLength = (int) (value * length);
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                if (value >= 0.8) {
                    bar.append("█");
                } else if (value >= 0.5) {
                    bar.append("▓");
                } else {
                    bar.append("░");
                }
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        return bar.toString();
    }

    /**
     * Exécute une commande WMIC et traite les résultats
     * Cette méthode doit être surchargée par les classes filles pour traiter les lignes
     * 
     * @param commands Liste des commandes WMIC à exécuter
     * @param skipPatterns Patterns à ignorer dans les résultats
     * @return Map contenant les informations récupérées
     */
    protected Map<String, Object> executeWmicCommands(String[] commands, String... skipPatterns) {
        Map<String, Object> info = new HashMap<>();
        
        if (!isCompatibleOS()) {
            info.put("error", "Système d'exploitation non supporté");
            return info;
        }

        try {
            for (String command : commands) {
                Process process = Runtime.getRuntime().exec(command);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (shouldSkipLine(line, skipPatterns)) {
                            continue;
                        }
                        processWmicLine(line.trim(), info);
                    }
                }
            }
        } catch (Exception e) {
            handleError(info, e, "lors de l'exécution des commandes WMIC");
        }
        
        return info;
    }

    /**
     * Traite une ligne de sortie WMIC - à implémenter par les classes filles
     * 
     * @param line Ligne nettoyée de la sortie WMIC
     * @param info Map où stocker les informations extraites
     */
    protected void processWmicLine(String line, Map<String, Object> info) {
        // Implémentation par défaut vide - à surcharger
    }

    /**
     * Vérifie si une ligne doit être ignorée selon les patterns fournis
     */
    private boolean shouldSkipLine(String line, String... skipPatterns) {
        if (line.trim().isEmpty()) {
            return true;
        }
        
        for (String pattern : skipPatterns) {
            if (line.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Efface l'écran de la console (multi-plateforme)
     */
    protected void clearScreen() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: imprimer des lignes vides
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Formate un temps de fonctionnement en chaîne lisible
     * 
     * @param uptimeSeconds Temps en secondes
     * @return Chaîne formatée (jours, heures, minutes, secondes)
     */
    protected String formatUptime(long uptimeSeconds) {
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

    /**
     * Gère les erreurs de façon standardisée pour les moniteurs
     * 
     * @param info Map où ajouter l'erreur
     * @param exception Exception capturée
     * @param context Contexte de l'erreur
     */
    protected void handleError(Map<String, Object> info, Exception exception, String context) {
        String errorMsg = "Erreur " + context + " pour " + getMonitorName();
        System.err.println(errorMsg + ": " + exception.getMessage());
        info.put("error", errorMsg);
    }
}