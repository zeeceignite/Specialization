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
        
        Specialization.logger.info("[LocalNameGenerator] Initializing name generator...");
        Specialization.logger.info("[LocalNameGenerator] Reading first names from: " + fnFile.getAbsolutePath());
        Specialization.logger.info("[LocalNameGenerator] Reading last names from: " + lnFile.getAbsolutePath());
        
        this.firstNames = Files.readAllLines(fnFile.toPath());
        this.lastNames = Files.readAllLines(lnFile.toPath());
        
        Specialization.logger.info("[LocalNameGenerator] Loaded " + firstNames.size() + " first names and " + lastNames.size() + " last names");
        Specialization.logger.info("[LocalNameGenerator] Total possible combinations: " + (firstNames.size() * lastNames.size()));
        
        // Load existing names from world playerdata to prevent duplicates
        loadExistingNamesFromWorld();
        
        Specialization.logger.info("[LocalNameGenerator] Initialization complete. " + usedNames.size() + " existing names loaded.");
    }

    /**
     * Loads existing player names from world playerdata files to prevent duplicates
     */
    private void loadExistingNamesFromWorld() {
        Specialization.logger.info("[LocalNameGenerator] Starting to load existing names from world playerdata...");
        
        try {
            // Get the world's playerdata directory
            File worldContainer = Bukkit.getWorldContainer();
            Specialization.logger.info("[LocalNameGenerator] World container path: " + worldContainer.getAbsolutePath());
            
            File[] worldDirs = worldContainer.listFiles(File::isDirectory);
            
            if (worldDirs != null) {
                Specialization.logger.info("[LocalNameGenerator] Found " + worldDirs.length + " world directories");
                for (File worldDir : worldDirs) {
                    Specialization.logger.info("[LocalNameGenerator] Checking world directory: " + worldDir.getName());
                    File playerdataDir = new File(worldDir, "playerdata");
                    if (playerdataDir.exists() && playerdataDir.isDirectory()) {
                        Specialization.logger.info("[LocalNameGenerator] Found playerdata directory in: " + worldDir.getName());
                        // Read existing player names from CustomPlayer data
                        loadNamesFromPlayerdata(playerdataDir);
                    } else {
                        Specialization.logger.info("[LocalNameGenerator] No playerdata directory found in: " + worldDir.getName());
                    }
                }
            } else {
                Specialization.logger.warning("[LocalNameGenerator] No world directories found in world container");
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
        Specialization.logger.info("[LocalNameGenerator] Loading names from playerdata directory: " + playerdataDir.getAbsolutePath());
        
        try {
            // Look for CustomPlayer data files in the MinecraftCivilizationsCore plugin directory
            File pluginDataDir = new File(Bukkit.getWorldContainer(), "plugins/MinecraftCivilizationsCore");
            Specialization.logger.info("[LocalNameGenerator] Looking for CustomPlayer data in: " + pluginDataDir.getAbsolutePath());
            
            if (pluginDataDir.exists()) {
                Specialization.logger.info("[LocalNameGenerator] MinecraftCivilizationsCore plugin directory found");
                File customPlayersDir = new File(pluginDataDir, "CustomPlayers");
                Specialization.logger.info("[LocalNameGenerator] Checking CustomPlayers directory: " + customPlayersDir.getAbsolutePath());
                
                if (customPlayersDir.exists() && customPlayersDir.isDirectory()) {
                    Specialization.logger.info("[LocalNameGenerator] CustomPlayers directory found");
                    File[] playerFiles = customPlayersDir.listFiles((dir, name) -> name.endsWith(".json"));
                    
                    if (playerFiles != null) {
                        Specialization.logger.info("[LocalNameGenerator] Found " + playerFiles.length + " player JSON files");
                        
                        for (File playerFile : playerFiles) {
                            Specialization.logger.info("[LocalNameGenerator] Processing file: " + playerFile.getName());
                            try {
                                String content = Files.readString(playerFile.toPath());
                                Specialization.logger.info("[LocalNameGenerator] File content length: " + content.length() + " characters");
                                
                                // Extract name from JSON - look for "name" field
                                String namePattern = "\"name\":\"";
                                int nameStart = content.indexOf(namePattern);
                                if (nameStart != -1) {
                                    nameStart += namePattern.length();
                                    int nameEnd = content.indexOf("\"", nameStart);
                                    if (nameEnd != -1) {
                                        String existingName = content.substring(nameStart, nameEnd);
                                        Specialization.logger.info("[LocalNameGenerator] Found name in JSON: '" + existingName + "'");
                                        
                                        // Convert from display name format to internal format
                                        String internalName = extractInternalName(existingName);
                                        if (internalName != null && !internalName.isEmpty()) {
                                            boolean wasNew = usedNames.add(internalName);
                                            if (wasNew) {
                                                Specialization.logger.info("[LocalNameGenerator] Added existing name to used set: " + internalName);
                                            } else {
                                                Specialization.logger.info("[LocalNameGenerator] Name already in used set: " + internalName);
                                            }
                                        } else {
                                            Specialization.logger.warning("[LocalNameGenerator] Failed to extract internal name from: '" + existingName + "'");
                                        }
                                    } else {
                                        Specialization.logger.warning("[LocalNameGenerator] Could not find end quote for name in file: " + playerFile.getName());
                                    }
                                } else {
                                    Specialization.logger.info("[LocalNameGenerator] No name field found in file: " + playerFile.getName());
                                }
                            } catch (Exception e) {
                                Specialization.logger.warning("[LocalNameGenerator] Failed to read player file " + playerFile.getName() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Specialization.logger.info("[LocalNameGenerator] No JSON files found in CustomPlayers directory");
                    }
                } else {
                    Specialization.logger.info("[LocalNameGenerator] CustomPlayers directory does not exist: " + customPlayersDir.getAbsolutePath());
                }
            } else {
                Specialization.logger.info("[LocalNameGenerator] MinecraftCivilizationsCore plugin directory does not exist: " + pluginDataDir.getAbsolutePath());
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
        Specialization.logger.info("[LocalNameGenerator] Extracting internal name from: '" + displayName + "'");
        
        if (displayName == null || displayName.isEmpty()) {
            Specialization.logger.warning("[LocalNameGenerator] Display name is null or empty");
            return null;
        }
        
        // Remove any JSON formatting or color codes
        String cleanName = displayName.replaceAll("\\{[^}]*\\}", "").trim();
        Specialization.logger.info("[LocalNameGenerator] After removing JSON formatting: '" + cleanName + "'");
        
        // If it already contains underscore, it might be in internal format
        if (cleanName.contains("_")) {
            Specialization.logger.info("[LocalNameGenerator] Name already contains underscore, using as-is: '" + cleanName + "'");
            return cleanName;
        }
        
        // If it contains space, convert to underscore format
        if (cleanName.contains(" ")) {
            String result = cleanName.replace(" ", "_");
            Specialization.logger.info("[LocalNameGenerator] Converted spaces to underscores: '" + result + "'");
            return result;
        }
        
        Specialization.logger.info("[LocalNameGenerator] No conversion needed, returning: '" + cleanName + "'");
        return cleanName;
    }

    /** @return a randomly-picked "FirstName LastName" */
    public String nextName() throws NoSuchElementException {
        Specialization.logger.info("[LocalNameGenerator] Generating new name...");
        Specialization.logger.info("[LocalNameGenerator] Current used names count: " + usedNames.size());
        Specialization.logger.info("[LocalNameGenerator] Total possible combinations: " + (firstNames.size() * lastNames.size()));
        
        if (usedNames.size() >= firstNames.size() * lastNames.size()) {
            Specialization.logger.severe("[LocalNameGenerator] All possible name combinations have been used!");
            throw new NoSuchElementException("All possible name combinations have been used");
        }
        
        int attempts = 0;
        int maxAttempts = firstNames.size() * lastNames.size() * 2; // Safety limit
        Specialization.logger.info("[LocalNameGenerator] Maximum attempts allowed: " + maxAttempts);
        
        while (attempts < maxAttempts) {
            String first = firstNames.get(random.nextInt(firstNames.size()));
            String last = lastNames.get(random.nextInt(lastNames.size()));
            String name = first + "_" + last;
            
            Specialization.logger.info("[LocalNameGenerator] Attempt " + (attempts + 1) + ": Generated name '" + name + "'");
            
            if (usedNames.add(name)) {
                Specialization.logger.info("[LocalNameGenerator] SUCCESS: Generated unique name '" + name + "' after " + (attempts + 1) + " attempts");
                Specialization.logger.info("[LocalNameGenerator] Updated used names count: " + usedNames.size());
                return name;
            } else {
                Specialization.logger.info("[LocalNameGenerator] Name '" + name + "' already exists, trying again...");
            }
            attempts++;
        }
        
        Specialization.logger.severe("[LocalNameGenerator] FAILED: Could not generate a unique name after " + maxAttempts + " attempts");
        Specialization.logger.severe("[LocalNameGenerator] Used names: " + usedNames.size() + "/" + (firstNames.size() * lastNames.size()));
        throw new NoSuchElementException("Could not generate a unique name after " + maxAttempts + " attempts");
    }
}
