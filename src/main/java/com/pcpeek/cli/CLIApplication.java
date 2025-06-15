package com.pcpeek.cli;

import com.pcpeek.SystemData;
import com.pcpeek.monitors.staticinfo.OSLevelMonitor;
import com.pcpeek.monitors.staticinfo.HardwareLevelMonitor;
import java.util.Scanner;

public class CLIApplication {
    private final SystemData systemData;
    private final CLIMenuHandler menuHandler;

    // Moniteurs
    private final OSLevelMonitor osMonitor;
    private final HardwareLevelMonitor hwMonitor;

    public CLIApplication() {
        this.systemData = new SystemData();
        this.menuHandler = new CLIMenuHandler(systemData);

        this.osMonitor = new OSLevelMonitor();
        this.hwMonitor = new HardwareLevelMonitor();

        collectStaticData();
    }

    public void run() {
        System.out.println("=== PC-Peek ===");
        System.out.println("Mode Console Activé");
        System.out.println("Initialisation des moniteurs système...\n");

        Scanner scanner = new Scanner(System.in);
        menuHandler.showMainMenu(scanner);

        scanner.close();
    }

    private void collectStaticData() {
        osMonitor.getSystemInfo().forEach(systemData::putStatic);

        hwMonitor.getSystemInfo().forEach(systemData::putStatic);
    }
}
