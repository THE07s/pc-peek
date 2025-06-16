package com.pcpeek.monitors.staticinfo;

import com.pcpeek.monitors.Monitor;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OSLevelMonitor extends Monitor {
    private static final String[] WMIC_COMMANDS = {
            "wmic os get caption,version,osarchitecture,serialnumber",
            "wmic computersystem get model,manufacturer,systemtype",
            "wmic path win32_operatingsystem get caption,version,osarchitecture,serialnumber,licensedatetime"
    };

    public OSLevelMonitor() {
        super();
    }

    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        if (!isCompatibleOS()) {
            info.put("error", "Système d'exploitation non supporté");
            return info;
        }

        try {
            for (String command : WMIC_COMMANDS) {
                Process process = Runtime.getRuntime().exec(command);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || line.contains("Caption") || line.contains("Version"))
                            continue;
                        processSystemInfo(line.trim(), info);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des informations système: " + e.getMessage());
            info.put("error", "Erreur lors de la récupération des informations système");
        }
        return info;
    }

    private void processSystemInfo(String line, Map<String, Object> info) {
        if (line.contains("Caption")) {
            info.put("os.caption", line.split("Caption")[1].trim());
        } else if (line.contains("Version")) {
            info.put("os.version", line.split("Version")[1].trim());
        } else if (line.contains("OSArchitecture")) {
            info.put("os.architecture", line.split("OSArchitecture")[1].trim());
        } else if (line.contains("SerialNumber")) {
            info.put("os.serial", line.split("SerialNumber")[1].trim());
        } else if (line.contains("LicenseDateTime")) {
            info.put("os.license", line.split("LicenseDateTime")[1].trim());
        } else if (line.contains("Model")) {
            info.put("system.model", line.split("Model")[1].trim());
        } else if (line.contains("Manufacturer")) {
            info.put("system.manufacturer", line.split("Manufacturer")[1].trim());
        } else if (line.contains("SystemType")) {
            info.put("system.type", line.split("SystemType")[1].trim());
        }
    }

    public void displaySystemInfo(Map<String, Object> systemInfo) {
        if (systemInfo.containsKey("error")) {
            System.out.println("Erreur : " + systemInfo.get("error"));
            return;
        }

        System.out.println("\nInformations Système :");
        System.out.println("---------------------");
        if (systemInfo.containsKey("os.caption")) {
            System.out.println("Système : " + systemInfo.get("os.caption"));
        }
        if (systemInfo.containsKey("os.version")) {
            System.out.println("Version : " + systemInfo.get("os.version"));
        }
        if (systemInfo.containsKey("os.architecture")) {
            System.out.println("Architecture : " + systemInfo.get("os.architecture"));
        }
        if (systemInfo.containsKey("os.serial")) {
            System.out.println("Numéro de série : " + systemInfo.get("os.serial"));
        }
        if (systemInfo.containsKey("os.license")) {
            System.out.println("Date d'activation : " + systemInfo.get("os.license"));
        }

        System.out.println("\nInformations Matérielles :");
        System.out.println("-------------------------");
        if (systemInfo.containsKey("system.model")) {
            System.out.println("Modèle : " + systemInfo.get("system.model"));
        }
        if (systemInfo.containsKey("system.manufacturer")) {
            System.out.println("Fabricant : " + systemInfo.get("system.manufacturer"));
        }
        if (systemInfo.containsKey("system.type")) {
            System.out.println("Type : " + systemInfo.get("system.type"));
        }
    }

    @Override
    protected Map<String, Object> initializeSystemInfo() {
        return getSystemInfo();
    }

    @Override
    protected void performUpdate() {}

    @Override
    protected void displayContent() {
        displaySystemInfo(getSystemInfo());
    }

    @Override
    protected String getMonitorName() {
        return "Moniteur Système d'Exploitation";
    }
}
