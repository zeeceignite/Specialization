package com.minecraftcivilizations.specialization.Player;

import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class LocalNameGenerator {

    private final List<String> firstNames;
    private final List<String> lastNames;
    private final Set<String> usedNames = new java.util.HashSet<>();
    private final Random random = new Random();
    private static File fnFile;
    private static File lnFile;

    /**
     * @throws IOException if either file can't be read
     */
    public LocalNameGenerator() throws IOException {
        fnFile = new File(Specialization.getInstance().getDataFolder(), "first_names.txt");
        lnFile = new File(Specialization.getInstance().getDataFolder(), "last_names.txt");
        
        this.firstNames = Files.readAllLines(fnFile.toPath());
        this.lastNames = Files.readAllLines(lnFile.toPath());
        
        Specialization.logger.info("[LocalNameGenerator] Loaded " + firstNames.size() + " first names and " + lastNames.size() + " last names");
        
        // Load existing names from world playerdata to prevent duplicates
        loadExistingNamesFromWorld();
        
        Specialization.logger.info("[LocalNameGenerator] Initialization complete. " + usedNames.size() + " existing names loaded.");
    }

    /**
     * Loads existing player names from world playerdata files to prevent duplicates
     */
    private void loadExistingNamesFromWorld() {
        try {
            // Get the world's playerdata directory
            File worldContainer = Bukkit.getWorldContainer();
            
            File[] worldDirs = worldContainer.listFiles(File::isDirectory);
            
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    File playerdataDir = new File(worldDir, "playerdata");
                    if (playerdataDir.exists() && playerdataDir.isDirectory()) {
                        // Read existing player names from CustomPlayer data
                        loadNamesFromPlayerdata(playerdataDir);
                    }
                }
            }
        } catch (Exception e) {
            Specialization.logger.warning("[LocalNameGenerator] Failed to load existing names from world playerdata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads names from playerdata directory by checking CustomPlayer files
     */
    private void loadNamesFromPlayerdata(File playerdataDir) {
        try {
            // Look for CustomPlayer data files in the MinecraftCivilizationsCore plugin directory
            File pluginDataDir = new File(Bukkit.getWorldContainer(), "plugins/MinecraftCivilizationsCore");
            
            if (pluginDataDir.exists()) {
                File customPlayersDir = new File(pluginDataDir, "CustomPlayers");
                
                if (customPlayersDir.exists() && customPlayersDir.isDirectory()) {
                    File[] playerFiles = customPlayersDir.listFiles((dir, name) -> name.endsWith(".json"));
                    
                    if (playerFiles != null) {
                        for (File playerFile : playerFiles) {
                            try {
                                String content = Files.readString(playerFile.toPath());
                                
                                // Extract name from JSON - look for "name" field
                                String namePattern = "\"name\":\"";
                                int nameStart = content.indexOf(namePattern);
                                if (nameStart != -1) {
                                    nameStart += namePattern.length();
                                    int nameEnd = content.indexOf("\"", nameStart);
                                    if (nameEnd != -1) {
                                        String existingName = content.substring(nameStart, nameEnd);
                                        
                                        // Convert from display name format to internal format
                                        String internalName = extractInternalName(existingName);
                                        if (internalName != null && !internalName.isEmpty()) {
                                            boolean wasNew = usedNames.add(internalName);
                                            if (wasNew) {
                                                Specialization.logger.info("[LocalNameGenerator] Added existing name to used set: " + internalName);
                                            }
                                        } else {
                                            Specialization.logger.warning("[LocalNameGenerator] Failed to extract internal name from: '" + existingName + "'");
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Specialization.logger.warning("[LocalNameGenerator] Failed to read player file " + playerFile.getName() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Specialization.logger.warning("[LocalNameGenerator] Failed to load names from playerdata: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Extracts the internal name format (FirstName_LastName) from a display name
     * @param displayName The display name that might contain formatting
     * @return The internal name format or null if extraction fails
     */
    private String extractInternalName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            Specialization.logger.warning("[LocalNameGenerator] Display name is null or empty");
            return null;
        }
        
        // Remove any JSON formatting or color codes
        String cleanName = displayName.replaceAll("\\{[^}]*\\}", "").trim();
        
        // If it already contains underscore, it might be in internal format
        if (cleanName.contains("_")) {
            return cleanName;
        }
        
        // If it contains space, convert to underscore format
        if (cleanName.contains(" ")) {
            return cleanName.replace(" ", "_");
        }
        
        return cleanName;
    }

    /** @return a randomly-picked "FirstName LastName" */
    public String nextName() throws NoSuchElementException {
        Specialization.logger.info("[LocalNameGenerator] Generating new name...");
        
        if (usedNames.size() >= firstNames.size() * lastNames.size()) {
            Specialization.logger.severe("[LocalNameGenerator] All possible name combinations have been used!");
            throw new NoSuchElementException("All possible name combinations have been used");
        }
        
        int attempts = 0;
        int maxAttempts = firstNames.size() * lastNames.size() * 2; // Safety limit
        
        while (attempts < maxAttempts) {
            String first = firstNames.get(random.nextInt(firstNames.size()));
            String last = lastNames.get(random.nextInt(lastNames.size()));
            String name = first + "_" + last;
            
            if (usedNames.add(name)) {
                return name;
            }
            attempts++;
        }
        
        Specialization.logger.severe("[LocalNameGenerator] FAILED: Could not generate a unique name after " + maxAttempts + " attempts");
        throw new NoSuchElementException("Could not generate a unique name after " + maxAttempts + " attempts");
    }
}
