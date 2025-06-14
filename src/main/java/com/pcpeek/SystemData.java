package com.pcpeek;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalDouble;

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
    
    // Getters spécialisés
    public OptionalDouble getCpuLoad() { return getDouble("cpu_load"); }
    public OptionalDouble getCpuTemperature() { return getDouble("cpu_temperature"); }
    public OptionalDouble getGpuLoad() { return getDouble("gpu_load"); }
    public OptionalDouble getGpuTemperature() { return getDouble("gpu_temperature"); }
    public Optional<String> getCpuName() { return getString("cpu_name"); }
    public Optional<String> getOsName() { return getString("os_name"); }
    public Optional<Long> getTotalMemory() { return getLong("total_memory"); }
    public Optional<Long> getAvailableMemory() { return getLong("available_memory"); }
    
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
}
