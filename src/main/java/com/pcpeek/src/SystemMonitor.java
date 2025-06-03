package com.pcpeek.src;// src/SystemMonitor.java
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

public class SystemMonitor {
    private final SystemInfo si;
    private final HardwareAbstractionLayer hal;
    private final OperatingSystem os;

    public SystemMonitor() {
        this.si = new SystemInfo();
        this.hal = si.getHardware();
        this.os = si.getOperatingSystem();
    }

    public SystemInfo getSystemInfo() {
        return si;
    }

    public HardwareAbstractionLayer getHardware() {
        return hal;
    }

    public OperatingSystem getOperatingSystem() {
        return os;
    }
}