package com.pcpeek;

import com.pcpeek.SystemData;
import com.pcpeek.monitors.staticinfo.OSLevelMonitor;
import com.pcpeek.monitors.staticinfo.HardwareLevelMonitor;
import com.pcpeek.monitors.dynamicinfo.ProbeMonitor;
import com.pcpeek.monitors.dynamicinfo.ResourceMonitor;
import com.pcpeek.views.DashboardView;

/**
 * Test simple pour vérifier l'intégration entre les moniteurs, SystemData et DashboardView
 */
public class SystemDataIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== Test d'intégration SystemData ===\n");
        
        // 1. Test de SystemData avec les moniteurs statiques
        testStaticMonitors();
        
        // 2. Test de SystemData avec les moniteurs dynamiques  
        testDynamicMonitors();
        
        // 3. Test de DashboardView avec SystemData
        testDashboardView();
        
        System.out.println("\n=== Test terminé ===");
    }
    
    private static void testStaticMonitors() {
        System.out.println("1. Test des moniteurs statiques avec SystemData:");
        
        SystemData systemData = new SystemData();
        OSLevelMonitor osMonitor = new OSLevelMonitor();
        HardwareLevelMonitor hwMonitor = new HardwareLevelMonitor();
        
        // Alimenter SystemData depuis les moniteurs
        systemData.updateStaticData(osMonitor.getSystemInfo());
        systemData.updateStaticData(hwMonitor.getSystemInfo());
        
        System.out.println("  - Données statiques collectées: " + systemData.getStaticDataCount());
        System.out.println("  - OS Name: " + systemData.getOsName().orElse("N/A"));
        System.out.println("  - OS Caption: " + systemData.getOsCaption().orElse("N/A"));
        System.out.println("  - CPU Name: " + systemData.getCpuName().orElse("N/A"));
        System.out.println("  - Memory Total: " + systemData.getMemoryTotal().orElse(0L));
        System.out.println("  ✓ Test des moniteurs statiques terminé\n");
    }
    
    private static void testDynamicMonitors() {
        System.out.println("2. Test des moniteurs dynamiques avec SystemData:");
        
        SystemData systemData = new SystemData();
        ProbeMonitor probeMonitor = new ProbeMonitor();
        ResourceMonitor resourceMonitor = new ResourceMonitor();
        
        // Alimenter SystemData depuis les moniteurs dynamiques
        systemData.updateDynamicData(probeMonitor.getProbeInfo());
        systemData.updateDynamicData(resourceMonitor.getResourceInfo());
        
        System.out.println("  - Données dynamiques collectées: " + systemData.getDynamicDataCount());
        System.out.println("  - CPU Load: " + systemData.getCpuLoad().orElse(-1.0));
        System.out.println("  - CPU Temperature: " + systemData.getCpuTemperature().orElse(-1.0));
        System.out.println("  - GPU Load: " + systemData.getGpuLoad().orElse(-1.0));
        System.out.println("  - Total Memory: " + systemData.getTotalMemory().orElse(0L));
        System.out.println("  ✓ Test des moniteurs dynamiques terminé\n");
    }
    
    private static void testDashboardView() {
        System.out.println("3. Test de DashboardView avec SystemData:");
        
        try {
            DashboardView dashboard = new DashboardView();
            SystemData systemData = dashboard.getSystemData();
            
            if (systemData != null) {
                System.out.println("  - SystemData correctement initialisé dans DashboardView");
                System.out.println("  - Données statiques: " + systemData.getStaticDataCount());
                System.out.println("  - Données dynamiques: " + systemData.getDynamicDataCount());
                System.out.println("  - Total des clés: " + systemData.getAllKeys().size());
                System.out.println("  ✓ Test de DashboardView terminé");
            } else {
                System.out.println("  ✗ SystemData est null dans DashboardView");
            }
        } catch (Exception e) {
            System.out.println("  ⚠ Erreur lors du test DashboardView: " + e.getMessage());
            System.out.println("  (Normal en dehors d'un contexte web Vaadin)");
        }
        System.out.println();
    }
}
