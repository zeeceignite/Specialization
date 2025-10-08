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
     * Loads names from MinecraftCivilizationsCore directory by checking all UUID files
     */
    private void loadNamesFromPlayerdata(File playerdataDir) {
        try {
            // Look for MinecraftCivilizationsCore plugin directory
            File pluginDataDir = new File(Bukkit.getWorldContainer(), "plugins/MinecraftCivilizationsCore");
            
            if (pluginDataDir.exists() && pluginDataDir.isDirectory()) {
                Specialization.logger.info("[LocalNameGenerator] Scanning MinecraftCivilizationsCore directory: " + pluginDataDir.getAbsolutePath());
                
                // Get all files in the directory except db.properties
                File[] allFiles = pluginDataDir.listFiles((dir, name) -> 
                    !name.equals("db.properties") && !name.startsWith("."));
                
                if (allFiles != null) {
                    Specialization.logger.info("[LocalNameGenerator] Found " + allFiles.length + " files to scan");
                    
                    int filesProcessed = 0;
                    int namesFound = 0;
                    
                    for (File file : allFiles) {
                        if (file.isFile()) {
                            try {
                                String content = Files.readString(file.toPath());
                                int namesInThisFile = 0;
                                // Look for the name field with nested JSON: "name": "{...}"
                                String namePattern = "\"name\":\\s*\"\\{";
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(namePattern);
                                java.util.regex.Matcher matcher = pattern.matcher(content);
                                
                                while (matcher.find()) {
                                    int nameStart = matcher.end() - 1; // Start at the opening brace
                                    
                                    // Find the matching closing brace and quote
                                    int braceCount = 0;
                                    int pos = nameStart;
                                    boolean inString = false;
                                    boolean escaped = false;
                                    
                                    while (pos < content.length()) {
                                        char c = content.charAt(pos);
                                        
                                        if (escaped) {
                                            escaped = false;
                                        } else if (c == '\\') {
                                            escaped = true;
                                        } else if (c == '"' && !escaped) {
                                            inString = !inString;
                                        } else if (!inString) {
                                            if (c == '{') {
                                                braceCount++;
                                            } else if (c == '}') {
                                                braceCount--;
                                                if (braceCount == 0) {
                                                    // Found the end of the JSON object
                                                    pos++; // Include the closing brace
                                                    break;
                                                }
                                            }
                                        }
                                        pos++;
                                    }
                                    
                                    if (braceCount == 0 && pos < content.length()) {
                                        // Extract the JSON string
                                        String jsonString = content.substring(nameStart, pos);
                                        
                                        // Parse the nested JSON to extract the "text" field
                                        String extractedName = extractTextFromNestedJson(jsonString);
                                        
                                        if (extractedName != null && !extractedName.isEmpty()) {
                                            // Convert to internal format
                                            String internalName = extractInternalName(extractedName);
                                            if (internalName != null && !internalName.isEmpty()) {
                                                boolean wasNew = usedNames.add(internalName);
                                                if (wasNew) {
                                                    namesInThisFile++;
                                                    namesFound++;
                                                    Specialization.logger.info("[LocalNameGenerator] Found name in " + file.getName() + ": " + internalName);
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                filesProcessed++;
                                if (namesInThisFile == 0) {
                                    Specialization.logger.fine("[LocalNameGenerator] No names found in file: " + file.getName());
                                }
                                
                            } catch (Exception e) {
                                Specialization.logger.warning("[LocalNameGenerator] Failed to read file " + file.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    Specialization.logger.info("[LocalNameGenerator] Scan complete - Processed " + filesProcessed + " files, found " + namesFound + " unique names");
                    
                } else {
                    Specialization.logger.warning("[LocalNameGenerator] No files found in MinecraftCivilizationsCore directory");
                }
            } else {
                Specialization.logger.warning("[LocalNameGenerator] MinecraftCivilizationsCore directory not found: " + pluginDataDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Specialization.logger.warning("[LocalNameGenerator] Failed to load names from MinecraftCivilizationsCore: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Extracts the "text" field value from a nested JSON string
     * @param jsonString The JSON string like {"italic":false,"color":"white","text":"Participant_973"}
     * @return The text value or null if extraction fails
     */
    private String extractTextFromNestedJson(String jsonString) {
        try {
            Specialization.logger.info("[LocalNameGenerator] Raw JSON string to parse: " + jsonString);

            String textPattern = "\\\\\"text\\\\\"\\s*:\\s*\\\\\"([^\\\\\"]+)\\\\\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(textPattern);
            java.util.regex.Matcher matcher = pattern.matcher(jsonString);

            if (matcher.find()) {
                String textValue = matcher.group(1);
                Specialization.logger.info("[LocalNameGenerator] Extracted text from JSON: " + textValue);
                return textValue;
            } else {
                String altPattern = "\"text\"\\s*:\\s*\"([^\"]+)\"";
                java.util.regex.Pattern altRegex = java.util.regex.Pattern.compile(altPattern);
                java.util.regex.Matcher altMatcher = altRegex.matcher(jsonString);

                if (altMatcher.find()) {
                    String textValue = altMatcher.group(1);
                    Specialization.logger.info("[LocalNameGenerator] Extracted text using alternative pattern: " + textValue);
                    return textValue;
                }

                Specialization.logger.warning("[LocalNameGenerator] No 'text' field found in JSON: " + jsonString);
                return null;
            }
        } catch (Exception e) {
            Specialization.logger.warning("[LocalNameGenerator] Failed to extract text from JSON: " + jsonString + " - " + e.getMessage());
            return null;
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

            if (name.length() <= 16 && usedNames.add(name)) {
                return name;
            }

            attempts++;
        }

        Specialization.logger.severe("[LocalNameGenerator] FAILED: Could not generate a unique valid name after " + maxAttempts + " attempts");
        throw new NoSuchElementException("Could not generate a unique valid name after " + maxAttempts + " attempts");
    }

}
