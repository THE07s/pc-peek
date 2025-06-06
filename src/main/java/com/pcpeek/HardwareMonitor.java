<<<<<<< Updated upstream:src/main/java/com/pcpeek/HardwareMonitor.java
package com.pcpeek;
=======
spackage com.pcpeek.src;
>>>>>>> Stashed changes:src/main/java/com/pcpeek/src/HardwareMonitor.java

import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Moniteur matériel qui hérite de Monitor pour gérer les informations sur le matériel
 */
public class HardwareMonitor extends Monitor {
    private static final String[] WMIC_COMMANDS = {
        "wmic cpu get name,numberofcores,numberoflogicalprocessors,currentclockspeed,maxclockspeed",
        "wmic memorychip get capacity,speed,manufacturer,partnumber",
        "wmic diskdrive get model,size,mediatype,status",
        "wmic baseboard get manufacturer,product,version,serialnumber"
    };

    @Override
    protected Map<String, Object> initializeSystemInfo() {
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
                        if (line.trim().isEmpty() || line.contains("Name") || line.contains("Capacity")) continue;
                        processHardwareInfo(line.trim(), info);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des informations matérielles: " + e.getMessage());
            info.put("error", "Erreur lors de la récupération des informations matérielles");
        }
        return info;
    }

    @Override
    protected void performUpdate() {
        // Les informations matérielles sont statiques, pas besoin de mise à jour
    }

    @Override
    protected void displayContent() {
        if (systemInfo.containsKey("error")) {
            System.out.println("Erreur : " + systemInfo.get("error"));
            return;
        }

        displayCPUInfo();
        displayMemoryInfo();
        displayDiskInfo();
        displayMotherboardInfo();
    }

    @Override
    protected String getMonitorName() {
        return "Moniteur Matériel";
    }

    private void displayCPUInfo() {
        System.out.println("\nProcesseur :");
        System.out.println("------------");
        if (systemInfo.containsKey("cpu.name")) {
            System.out.println("Modèle : " + systemInfo.get("cpu.name"));
        }
        if (systemInfo.containsKey("cpu.cores")) {
            System.out.println("Cœurs physiques : " + systemInfo.get("cpu.cores"));
        }
        if (systemInfo.containsKey("cpu.threads")) {
            System.out.println("Threads logiques : " + systemInfo.get("cpu.threads"));
        }
        if (systemInfo.containsKey("cpu.current.speed")) {
            System.out.println("Fréquence actuelle : " + systemInfo.get("cpu.current.speed") + " MHz");
        }
        if (systemInfo.containsKey("cpu.max.speed")) {
            System.out.println("Fréquence maximale : " + systemInfo.get("cpu.max.speed") + " MHz");
        }
    }

    private void displayMemoryInfo() {
        System.out.println("\nMémoire :");
        System.out.println("---------");
        if (systemInfo.containsKey("memory.total")) {
            System.out.println("Capacité totale : " + formatSize((Long)systemInfo.get("memory.total")));
        }
        if (systemInfo.containsKey("memory.speed")) {
            System.out.println("Vitesse : " + systemInfo.get("memory.speed") + " MHz");
        }
        if (systemInfo.containsKey("memory.manufacturer")) {
            System.out.println("Fabricant : " + systemInfo.get("memory.manufacturer"));
        }
        if (systemInfo.containsKey("memory.part")) {
            System.out.println("Référence : " + systemInfo.get("memory.part"));
        }
    }

    private void displayDiskInfo() {
        System.out.println("\nDisques :");
        System.out.println("---------");
        if (systemInfo.containsKey("disk.model")) {
            System.out.println("Modèle : " + systemInfo.get("disk.model"));
        }
        if (systemInfo.containsKey("disk.size")) {
            System.out.println("Taille : " + formatSize((Long)systemInfo.get("disk.size")));
        }
        if (systemInfo.containsKey("disk.type")) {
            System.out.println("Type : " + systemInfo.get("disk.type"));
        }
        if (systemInfo.containsKey("disk.status")) {
            System.out.println("État : " + systemInfo.get("disk.status"));
        }
    }

    private void displayMotherboardInfo() {
        System.out.println("\nCarte mère :");
        System.out.println("------------");
        if (systemInfo.containsKey("board.manufacturer")) {
            System.out.println("Fabricant : " + systemInfo.get("board.manufacturer"));
        }
        if (systemInfo.containsKey("board.model")) {
            System.out.println("Modèle : " + systemInfo.get("board.model"));
        }
        if (systemInfo.containsKey("board.version")) {
            System.out.println("Version : " + systemInfo.get("board.version"));
        }
        if (systemInfo.containsKey("board.serial")) {
            System.out.println("Numéro de série : " + systemInfo.get("board.serial"));
        }
    }

    private void processHardwareInfo(String line, Map<String, Object> info) {
        if (line.contains("Name")) {
            info.put("cpu.name", line.split("Name")[1].trim());
        } else if (line.contains("NumberOfCores")) {
            info.put("cpu.cores", line.split("NumberOfCores")[1].trim());
        } else if (line.contains("NumberOfLogicalProcessors")) {
            info.put("cpu.threads", line.split("NumberOfLogicalProcessors")[1].trim());
        } else if (line.contains("CurrentClockSpeed")) {
            info.put("cpu.current.speed", line.split("CurrentClockSpeed")[1].trim());
        } else if (line.contains("MaxClockSpeed")) {
            info.put("cpu.max.speed", line.split("MaxClockSpeed")[1].trim());
        } else if (line.contains("Capacity")) {
            info.put("memory.total", Long.parseLong(line.split("Capacity")[1].trim()));
        } else if (line.contains("Speed")) {
            info.put("memory.speed", line.split("Speed")[1].trim());
        } else if (line.contains("Manufacturer")) {
            if (line.contains("memory")) {
                info.put("memory.manufacturer", line.split("Manufacturer")[1].trim());
            } else {
                info.put("board.manufacturer", line.split("Manufacturer")[1].trim());
            }
        } else if (line.contains("PartNumber")) {
            info.put("memory.part", line.split("PartNumber")[1].trim());
        } else if (line.contains("Model")) {
            info.put("disk.model", line.split("Model")[1].trim());
        } else if (line.contains("Size")) {
            info.put("disk.size", Long.parseLong(line.split("Size")[1].trim()));
        } else if (line.contains("MediaType")) {
            info.put("disk.type", line.split("MediaType")[1].trim());
        } else if (line.contains("Status")) {
            info.put("disk.status", line.split("Status")[1].trim());
        } else if (line.contains("Product")) {
            info.put("board.model", line.split("Product")[1].trim());
        } else if (line.contains("Version")) {
            info.put("board.version", line.split("Version")[1].trim());
        } else if (line.contains("SerialNumber")) {
            info.put("board.serial", line.split("SerialNumber")[1].trim());
        }
    }

    public boolean hasInfo() {
        return systemInfo != null && !systemInfo.isEmpty();
    }
} 