package com.pcpeek;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Objects;

public class SystemData {
    private final Map<String, Object> staticData = new ConcurrentHashMap<>();
    private final Map<String, Object> dynamicData = new ConcurrentHashMap<>();
    private LocalDateTime lastUpdate = LocalDateTime.now();
    
    // Interface pour observer les changements
    public interface SystemDataObserver {
        void onDataChanged(SystemData systemData, Set<String> changedFields);
    }
    
    private final List<SystemDataObserver> observers = new ArrayList<>();
    
    public void addObserver(SystemDataObserver observer) {
        observers.add(observer);
    }
    
    // Getters typés avec Optional
    public OptionalDouble getDouble(String key) {
        Object value = getValue(key);
        if (value instanceof Number) {
            return OptionalDouble.of(((Number) value).doubleValue());
        }
        return OptionalDouble.empty();
    }
    
    public Optional<String> getString(String key) {
        Object value = getValue(key);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }
    
    public Optional<Long> getLong(String key) {
        Object value = getValue(key);
        if (value instanceof Number) {
            return Optional.of(((Number) value).longValue());
        }
        return Optional.empty();
    }
    
    private Object getValue(String key) {
        Object value = dynamicData.get(key);
        return value != null ? value : staticData.get(key);
    }
    
    // Getters spécialisés pour OS/Système
    public Optional<String> getOsCaption() { return getString("os.caption"); }
    public Optional<String> getOsVersion() { return getString("os.version"); }
    public Optional<String> getOsArchitecture() { return getString("os.architecture"); }
    public Optional<String> getOsSerial() { return getString("os.serial"); }
    public Optional<String> getOsLicense() { return getString("os.license"); }
    public Optional<String> getSystemModel() { return getString("system.model"); }
    public Optional<String> getSystemManufacturer() { return getString("system.manufacturer"); }
    public Optional<String> getSystemType() { return getString("system.type"); }
    
    // Getters spécialisés pour CPU
    public OptionalDouble getCpuLoad() { return getDouble("cpu_load"); }
    public OptionalDouble getCpuLoadAvg() { return getDouble("cpu_load_avg"); }
    public OptionalDouble getCpuTemperature() { return getDouble("cpu_temperature"); }
    public Optional<String> getCpuName() { return getString("cpu.name"); }
    public Optional<String> getProcessorName() { return getString("processor_name"); }
    public Optional<Long> getCpuCores() { return getLong("cpu.cores"); }
    public Optional<Long> getCpuThreads() { return getLong("cpu.threads"); }
    public Optional<Long> getCpuCurrentSpeed() { return getLong("cpu.current.speed"); }
    public Optional<Long> getCpuMaxSpeed() { return getLong("cpu.max.speed"); }
    public Optional<double[]> getCpuLoadsPerCore() { 
        Object value = getValue("cpu_loads_per_core");
        return value instanceof double[] ? Optional.of((double[]) value) : Optional.empty();
    }
    
    // Getters spécialisés pour GPU
    public OptionalDouble getGpuLoad() { return getDouble("gpu_load"); }
    public OptionalDouble getGpuTemperature() { return getDouble("gpu_temperature"); }
    
    // Getters spécialisés pour mémoire
    public Optional<Long> getTotalMemory() { return getLong("total_memory"); }
    public Optional<Long> getAvailableMemory() { return getLong("available_memory"); }
    public Optional<Long> getMemoryTotal() { return getLong("memory.total"); }
    public Optional<Long> getMemoryFree() { return getLong("memory.free"); }
    public Optional<String> getMemorySpeed() { return getString("memory.speed"); }
    public Optional<String> getMemoryManufacturer() { return getString("memory.manufacturer"); }
    public Optional<String> getMemoryPart() { return getString("memory.part"); }
    
    // Getters spécialisés pour disque
    public Optional<String> getDiskModel() { return getString("disk.model"); }
    public Optional<Long> getDiskSize() { return getLong("disk.size"); }
    public Optional<String> getDiskType() { return getString("disk.type"); }
    public Optional<String> getDiskStatus() { return getString("disk.status"); }
    
    // Getters spécialisés pour carte mère
    public Optional<String> getBoardManufacturer() { return getString("board.manufacturer"); }
    public Optional<String> getBoardModel() { return getString("board.model"); }
    public Optional<String> getBoardVersion() { return getString("board.version"); }
    public Optional<String> getBoardSerial() { return getString("board.serial"); }
    
    // Getters spécialisés pour ventilateurs et capteurs
    public Optional<int[]> getFanSpeeds() { 
        Object value = getValue("fan_speeds");
        return value instanceof int[] ? Optional.of((int[]) value) : Optional.empty();
    }
    
    // Getters spécialisés pour système et temps
    public Optional<Long> getSystemUptime() { return getLong("system_uptime"); }
    public Optional<String> getBootTime() { return getString("boot_time"); }
    public Optional<String> getOsName() { return getString("os_name"); }
    
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    
    public void putStatic(String key, Object value) {
        staticData.put(key, value);
    }
    
    public void putDynamic(String key, Object value) {
        dynamicData.put(key, value);
        lastUpdate = LocalDateTime.now();
    }
    
    public Map<String, Object> getStaticData() {
        return new ConcurrentHashMap<>(staticData);
    }
    
    public Map<String, Object> getDynamicData() {
        return new ConcurrentHashMap<>(dynamicData);
    }
    
    // Méthodes de mise à jour pour les données statiques OS/Système
    public void setOsCaption(String value) { putStatic("os.caption", value); }
    public void setOsVersion(String value) { putStatic("os.version", value); }
    public void setOsArchitecture(String value) { putStatic("os.architecture", value); }
    public void setOsSerial(String value) { putStatic("os.serial", value); }
    public void setOsLicense(String value) { putStatic("os.license", value); }
    public void setSystemModel(String value) { putStatic("system.model", value); }
    public void setSystemManufacturer(String value) { putStatic("system.manufacturer", value); }
    public void setSystemType(String value) { putStatic("system.type", value); }
    
    // Méthodes de mise à jour pour les données statiques CPU
    public void setCpuName(String value) { putStatic("cpu.name", value); }
    public void setCpuCores(Long value) { putStatic("cpu.cores", value); }
    public void setCpuThreads(Long value) { putStatic("cpu.threads", value); }
    public void setCpuCurrentSpeed(Long value) { putStatic("cpu.current.speed", value); }
    public void setCpuMaxSpeed(Long value) { putStatic("cpu.max.speed", value); }
    
    // Méthodes de mise à jour pour les données statiques mémoire
    public void setMemoryTotal(Long value) { putStatic("memory.total", value); }
    public void setMemorySpeed(String value) { putStatic("memory.speed", value); }
    public void setMemoryManufacturer(String value) { putStatic("memory.manufacturer", value); }
    public void setMemoryPart(String value) { putStatic("memory.part", value); }
    
    // Méthodes de mise à jour pour les données statiques disque
    public void setDiskModel(String value) { putStatic("disk.model", value); }
    public void setDiskSize(Long value) { putStatic("disk.size", value); }
    public void setDiskType(String value) { putStatic("disk.type", value); }
    public void setDiskStatus(String value) { putStatic("disk.status", value); }
    
    // Méthodes de mise à jour pour les données statiques carte mère
    public void setBoardManufacturer(String value) { putStatic("board.manufacturer", value); }
    public void setBoardModel(String value) { putStatic("board.model", value); }
    public void setBoardVersion(String value) { putStatic("board.version", value); }
    public void setBoardSerial(String value) { putStatic("board.serial", value); }
    
    // Méthodes de mise à jour pour les données dynamiques CPU
    public void setCpuLoad(Double value) { putDynamic("cpu_load", value); }
    public void setCpuLoadAvg(Double value) { putDynamic("cpu_load_avg", value); }
    public void setCpuTemperature(Double value) { putDynamic("cpu_temperature", value); }
    public void setProcessorName(String value) { putDynamic("processor_name", value); }
    public void setCpuLoadsPerCore(double[] value) { putDynamic("cpu_loads_per_core", value); }
    
    // Méthodes de mise à jour pour les données dynamiques GPU
    public void setGpuLoad(Double value) { putDynamic("gpu_load", value); }
    public void setGpuTemperature(Double value) { putDynamic("gpu_temperature", value); }
    
    // Méthodes de mise à jour pour les données dynamiques mémoire
    public void setTotalMemory(Long value) { putDynamic("total_memory", value); }
    public void setAvailableMemory(Long value) { putDynamic("available_memory", value); }
    public void setMemoryFree(Long value) { putDynamic("memory.free", value); }
    
    // Méthodes de mise à jour pour les données dynamiques capteurs
    public void setFanSpeeds(int[] value) { putDynamic("fan_speeds", value); }
    
    // Méthodes de mise à jour pour les données dynamiques système
    public void setSystemUptime(Long value) { putDynamic("system_uptime", value); }
    public void setBootTime(String value) { putDynamic("boot_time", value); }
    public void setOsName(String value) { putDynamic("os_name", value); }
    
    // Méthodes utilitaires pour mise à jour en masse
    public void updateStaticData(Map<String, Object> data) {
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                putStatic(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public void updateDynamicData(Map<String, Object> data) {
        if (data != null) {
            Set<String> changedKeys = new HashSet<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object newValue = entry.getValue();
                Object oldValue = dynamicData.get(key);
                
                if (!Objects.equals(oldValue, newValue)) {
                    changedKeys.add(key);
                }
                putDynamic(key, newValue);
            }
            
            // Notifier les observateurs des changements
            if (!changedKeys.isEmpty() && !observers.isEmpty()) {
                for (SystemDataObserver observer : observers) {
                    try {
                        observer.onDataChanged(this, changedKeys);
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la notification d'un observateur: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    // Méthodes pour obtenir toutes les données
    public Map<String, Object> getAllData() {
        Map<String, Object> allData = new HashMap<>(staticData);
        allData.putAll(dynamicData);
        return allData;
    }
    
    // Méthode pour nettoyer les données anciennes
    public void clearDynamicData() {
        dynamicData.clear();
        lastUpdate = LocalDateTime.now();
    }
    
    // Méthodes de validation et de contrôle
    public boolean hasData(String key) {
        return dynamicData.containsKey(key) || staticData.containsKey(key);
    }
    
    public boolean hasStaticData(String key) {
        return staticData.containsKey(key);
    }
    
    public boolean hasDynamicData(String key) {
        return dynamicData.containsKey(key);
    }
    
    // Méthodes pour obtenir les clés disponibles
    public Set<String> getStaticKeys() {
        return new HashSet<>(staticData.keySet());
    }
    
    public Set<String> getDynamicKeys() {
        return new HashSet<>(dynamicData.keySet());
    }
    
    public Set<String> getAllKeys() {
        Set<String> allKeys = new HashSet<>(staticData.keySet());
        allKeys.addAll(dynamicData.keySet());
        return allKeys;
    }
    
    // Méthodes pour obtenir des informations sur l'état des données
    public int getStaticDataCount() {
        return staticData.size();
    }
    
    public int getDynamicDataCount() {
        return dynamicData.size();
    }
    
    public boolean isEmpty() {
        return staticData.isEmpty() && dynamicData.isEmpty();
    }
    
    public boolean hasObservers() {
        return !observers.isEmpty();
    }
    
    public void removeObserver(SystemDataObserver observer) {
        observers.remove(observer);
    }
    
    public void clearObservers() {
        observers.clear();
    }
}
