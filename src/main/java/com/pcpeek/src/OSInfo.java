package com.pcpeek.src;

// src/OSInfo.java
public class OSInfo {
    public static void displayOSInfo() {
        System.out.println("Système d'exploitation : " + System.getProperty("os.name"));
        System.out.println("Version : " + System.getProperty("os.version"));
        System.out.println("Architecture : " + System.getProperty("os.arch"));
        System.out.println("Utilisateur : " + System.getProperty("user.name"));
        System.out.println("Répertoire utilisateur : " + System.getProperty("user.home"));
        System.out.println("Répertoire de travail : " + System.getProperty("user.dir"));
    }
}