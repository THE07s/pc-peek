# Tests Unitaires PC-Peek

Ce dossier contient les tests unitaires pour l'application PC-Peek. Les tests sont écrits avec JUnit 5 et couvrent les fonctionnalités essentielles de l'application.

## Structure des Tests

### MonitorTest.java
Tests de la classe de base `Monitor` :
- Test de compatibilité système (`isCompatibleOS`)
- Test de formatage des tailles (`formatSize`)
- Test des méthodes de base (`update`, `display`)
- Test du nom du moniteur (`getMonitorName`)

### OHMMonitorTest.java
Tests du moniteur de température OpenHardwareMonitor :
- Test de la connexion à OHM
- Test de la récupération de la température CPU
- Test de la mise à jour des capteurs
- Test de l'état de connexion

### OSMonitorTest.java
Tests du moniteur système en temps réel :
- Test de la charge CPU
- Test de l'utilisation mémoire
- Test de la fréquence CPU
- Test des informations sur les cœurs
- Test des méthodes de mise à jour et d'affichage

## Prérequis

- Java 11 ou supérieur
- JUnit 5
- Maven (pour la gestion des dépendances)

## Configuration Maven

Ajoutez les dépendances suivantes dans votre `pom.xml` :

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Exécution des Tests

### Via Maven
```bash
mvn test
```

### Via IDE
- Eclipse : Clic droit sur le dossier `src/test/java` > Run As > JUnit Test
- IntelliJ IDEA : Clic droit sur le dossier `test` > Run 'Tests in pc-peek'

## Notes Importantes

1. Certains tests nécessitent Windows pour fonctionner correctement (notamment les tests OHM)
2. Les tests de température peuvent retourner -1 si OpenHardwareMonitor n'est pas disponible
3. Les tests de performance système (CPU, mémoire) peuvent varier selon la charge du système

## Stratégie de Test

1. **Tests Unitaires** : Test des composants individuels
   - Vérification des méthodes de base
   - Validation des calculs et formatages
   - Gestion des erreurs

2. **Tests d'Intégration** : Test des interactions entre composants
   - Communication entre moniteurs
   - Gestion des ressources système
   - Synchronisation des mises à jour

3. **Tests de Compatibilité** : Test des fonctionnalités spécifiques à Windows
   - Vérification de la détection du système
   - Test des fonctionnalités OHM
   - Gestion des privilèges administrateur

## Maintenance

- Ajouter de nouveaux tests pour chaque nouvelle fonctionnalité
- Mettre à jour les tests existants lors de modifications majeures
- Vérifier la couverture de code régulièrement
- Documenter les cas de test particuliers 