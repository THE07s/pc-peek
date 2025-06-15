package com.pcpeek;

/**
 * Test rapide du formatage des octets pour vérifier que les valeurs 
 * sont bien converties en unités lisibles (GB, MB, etc.)
 */
public class FormatBytesTest {
    
    public static void main(String[] args) {
        System.out.println("=== Test du formatage des octets ===\n");
        
        // Tester la méthode de formatage de DashboardView
        testFormatBytes();
        
        System.out.println("\n=== Test terminé ===");
    }
    
    private static void testFormatBytes() {
        System.out.println("Test de formatBytes (méthode DashboardView) :");
        
        // Valeurs de test
        long[] testValues = {
            0L,                      // 0 B
            1023L,                   // 1023 B
            1024L,                   // 1.0 KB
            1048576L,                // 1.0 MB
            1073741824L,             // 1.0 GB
            25769803776L,            // ~24.0 GB (valeur problématique du user)
            7368933376L,             // ~6.9 GB (valeur problématique du user)
            Runtime.getRuntime().totalMemory()  // Mémoire JVM actuelle
        };
        
        for (long value : testValues) {
            String formatted = formatBytes(value);
            System.out.println(String.format("  %,15d bytes → %s", value, formatted));
        }
    }
    
    // Copie de la méthode formatBytes de DashboardView pour test
    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024.0;
            unitIndex++;
        }
        
        // Formater avec 1 décimale sauf pour les bytes
        if (unitIndex == 0) {
            return String.format("%.0f %s", size, units[unitIndex]);
        } else {
            return String.format("%.1f %s", size, units[unitIndex]);
        }
    }
}
