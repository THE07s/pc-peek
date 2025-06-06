package com.pcpeek.src;

import java.util.Map;
import java.util.HashMap;

/**
 * Classe abstraite représentant un moniteur système.
 * Cette classe définit les comportements communs pour tous les types de monitoring.
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
     * @return Nom du moniteur
     */
    protected abstract String getMonitorName();

    /**
     * Récupère les informations système actuelles
     * @return Map contenant les informations système
     */
    public Map<String, Object> getSystemInfo() {
        return new HashMap<>(systemInfo);
    }

    /**
     * Vérifie si le système d'exploitation est compatible
     * @return true si le système est compatible, false sinon
     */
    public boolean isCompatibleOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows");
    }

    /**
     * Formate une taille en octets en une chaîne lisible
     * @param bytes Taille en octets
     * @return Chaîne formatée
     */
    protected String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Formate un pourcentage
     * @param value Valeur entre 0 et 1
     * @return Chaîne formatée avec le symbole %
     */
    protected String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * Crée une barre de progression visuelle
     * @param value Valeur entre 0 et 1
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
} 