package com.regiontokens.model;

import java.util.ArrayList;
import java.util.List;

public class TokenTemplate {
    private final String id;
    private final String name;
    private final long durationSeconds;
    private final String assignedZoneId;  // Keep for backward compatibility
    private final List<String> assignedZoneIds;  // New: support multiple zones
    private final String itemMaterial;
    private final boolean glowing;
    private final List<String> lore;

    public TokenTemplate(String id, String name, long durationSeconds, String assignedZoneId, String itemMaterial, boolean glowing) {
        this.id = id;
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.assignedZoneId = assignedZoneId;
        this.itemMaterial = itemMaterial;
        this.glowing = glowing;
        this.lore = new ArrayList<>();
        this.assignedZoneIds = new ArrayList<>();
        
        // Initialize with single zone if provided
        if (assignedZoneId != null && !assignedZoneId.isEmpty()) {
            this.assignedZoneIds.add(assignedZoneId);
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getDurationSeconds() { return durationSeconds; }
    public String getAssignedZoneId() { return assignedZoneId; }
    
    // New methods for multiple zone support
    public List<String> getAssignedZoneIds() { 
        return new ArrayList<>(assignedZoneIds); 
    }
    
    public void addAssignedZone(String zoneId) {
        if (zoneId != null && !zoneId.isEmpty() && !assignedZoneIds.contains(zoneId)) {
            assignedZoneIds.add(zoneId);
        }
    }
    
    public void removeAssignedZone(String zoneId) {
        assignedZoneIds.remove(zoneId);
    }
    
    public boolean isAssignedToZone(String zoneId) {
        return assignedZoneIds.contains(zoneId);
    }
    
    public String getItemMaterial() { return itemMaterial; }
    public boolean isGlowing() { return glowing; }
    public List<String> getLore() { return new ArrayList<>(lore); }

    public void addLoreLine(String line) {
        lore.add(line);
    }

    public void setLoreLine(int index, String line) {
        if (index >= 0 && index < lore.size()) {
            lore.set(index, line);
        }
    }

    public void removeLoreLine(int index) {
        if (index >= 0 && index < lore.size()) {
            lore.remove(index);
        }
    }

    public List<String> getFullLore() {
        List<String> fullLore = new ArrayList<>(lore);
        fullLore.add("Time Left: <countdown>");
        return fullLore;
    }
}
