package com.minecraftcivilizations.specialization.Player;

import com.minecraftcivilizations.specialization.Specialization;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocalNameGenerator {

    private List<String> firstNames;
    private List<String> lastNames;
    private final Set<String> usedNames = new java.util.HashSet<>();

    private Map<String, List<String>> grouping = new HashMap<String, List<String>>();
    private final Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]");
    private final Random random = new Random();
    private static File fnFile;
    private static File lnFile;

    /**
     * @throws IOException if either file can't be read
     */
    public LocalNameGenerator() throws IOException {
        fnFile = new File(Specialization.getInstance().getDataFolder(), "first_names.txt");
        lnFile = new File(Specialization.getInstance().getDataFolder(), "last_names.txt");
        firstNames = new ArrayList<>();
        lastNames = new ArrayList<>();
        for (String string : Files.readAllLines(fnFile.toPath())) {
            if (string.isEmpty() || string.startsWith("#")) continue;
            this.firstNames.add(string);
        }


        for (String line : Files.readAllLines(lnFile.toPath())) {
            if (line.isEmpty() || line.startsWith("#")) continue;

            List<String> matches = getGroupPattern(line);
            if (matches.isEmpty()) {
                // Default (ungrouped) last name
                lastNames.add(line.trim());
            } else {
                // Grouped entries
                for (String key : matches) {
                    grouping.computeIfAbsent(key, k -> new ArrayList<>());

                    // Remove [tags] and trim
                    String cleaned = line.replaceAll("\\s*\\[[^\\]]+\\]\\s*", "").trim();
                    grouping.get(key).add(cleaned);
                }
            }
        }

// Debug output
        Specialization.logger.info("[Groups]:");
        for (Map.Entry<String, List<String>> entry : grouping.entrySet()) {
            Specialization.logger.info(entry.getKey() + ": " + entry.getValue());
        }
        Specialization.logger.info("[LocalNameGenerator] Loaded " + firstNames.size() + " first names and " + lastNames.size() + " last names");

        // Load existing names from world playerdata to prevent duplicates
        loadExistingNamesFromWorld();
        generateAllNameCombinations();
        Specialization.logger.info("[LocalNameGenerator] Initialization complete. " + usedNames.size() + " existing names loaded.");
    }
    private void generateAllNameCombinations() {
        File outputFile = new File(Specialization.getInstance().getDataFolder(), "GeneratedNames.txt");
        if (outputFile.exists()) {
            Specialization.logger.info("[LocalNameGenerator] GeneratedNames.txt already exists, skipping generation.");
            return;
        }

        try {
            List<String> allNames = new ArrayList<>();
            int totalCount = 0;

            // --- Parse first names ---
            List<NameLine> firstLines = new ArrayList<>();
            for (String line : firstNames) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String group = extractGroupTag(line);
                String clean = line.replaceAll("\\s*\\[[^\\]]+\\]\\s*", "").trim();
                List<String> variants = expandBraces(clean);

                firstLines.add(new NameLine(variants, group));
                System.out.println("[DEBUG FIRST] line=" + line + " | group=" + group + " | variants=" + variants);
            }

            // --- Parse last names (defaults + grouped) ---
            List<NameLine> lastLines = new ArrayList<>();

            // Default ungrouped last names
            for (String line : lastNames) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                List<String> variants = expandBraces(line);
                lastLines.add(new NameLine(variants, null));
            }

            // Grouped last names from grouping map
            for (Map.Entry<String, List<String>> entry : grouping.entrySet()) {
                String group = entry.getKey();
                for (String val : entry.getValue()) {
                    val = val.trim();
                    if (val.isEmpty()) continue;
                    List<String> variants = expandBraces(val);
                    lastLines.add(new NameLine(variants, group));
                    System.out.println("[DEBUG GROUPED LAST] " + group + " -> " + variants);
                }
            }

            // --- Separate for convenience ---
            List<String> defaultFirsts = firstLines.stream()
                    .filter(l -> l.group == null)
                    .flatMap(l -> l.variants.stream())
                    .toList();

            List<String> defaultLasts = lastLines.stream()
                    .filter(l -> l.group == null)
                    .flatMap(l -> l.variants.stream())
                    .toList();

            // --- Default-first + default-last ---
            for (String f : defaultFirsts) {
                for (String l : defaultLasts) {
                    String name = f + "_" + l;
                    if (name.length() <= 16) {
                        allNames.add(name);
                        totalCount++;
                    }
                }
            }

            // --- Grouped names ---
            Map<String, List<NameLine>> lastLinesByGroup = new HashMap<>();
            for (NameLine l : lastLines) {
                if (l.group != null)
                    lastLinesByGroup.computeIfAbsent(l.group, k -> new ArrayList<>()).add(l);
            }

            for (NameLine firstLine : firstLines) {
                if (firstLine.group == null) continue;

                List<NameLine> matchingLasts = lastLinesByGroup.getOrDefault(firstLine.group, List.of());
                for (String f : firstLine.variants) {
                    for (NameLine lastLine : matchingLasts) {
                        for (String l : lastLine.variants) {
                            String name = f + "_" + l;
                            if (name.length() <= 16) {
                                allNames.add(name);
                                totalCount++;
                            }
                        }
                    }
                }
            }

            // --- Write output ---
            Path path = outputFile.toPath();
            Files.createDirectories(outputFile.getParentFile().toPath());
            allNames.add("Total names: " + totalCount);
            Files.write(path, allNames);

            Specialization.logger.info("[LocalNameGenerator] Generated " + totalCount + " possible name combinations in " + outputFile.getName());
        } catch (IOException e) {
            Specialization.logger.severe("[LocalNameGenerator] Failed to generate all name combinations: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Helper class for first/last lines
    private static class NameLine {
        List<String> variants;
        String group;

        public NameLine(List<String> variants, String group) {
            this.variants = variants;
            this.group = group;
        }
    }


    /**
     * Extract the group tag inside [ ] for a line, or null if none.
     */
    private String extractGroupTag(String line) {
        int start = line.indexOf('[');
        int end = line.indexOf(']');
        if (start == -1 || end == -1 || end <= start) return null;
        return line.substring(start + 1, end).trim();
    }

    /**
     * Expands a string with {option1|option2|option3} into all options.
     * Example: "{Velvety|Plush|Warm}" => ["Velvety","Plush","Warm"]
     */
    private List<String> expandBraces(String input) {
        List<String> results = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(input);

        if (!matcher.find()) {
            results.add(input);
            return results; // no braces
        }

        String before = input.substring(0, matcher.start());
        String after = input.substring(matcher.end());
        String[] options = matcher.group(1).split("\\|");

        for (String option : options) {
            results.addAll(expandBraces(before + option + after)); // recursive for nested braces
        }

        return results;
    }



    private List<String> getGroupPattern(String string) {
        Matcher matcher = pattern.matcher(string);
        List<String> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(matcher.group(1)); // capture content inside []
        }

        return matches;
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
     *
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
     *
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

    /**
     * @return a randomly-picked "FirstName LastName"
     */
    public String nextName() throws NoSuchElementException {
        Specialization.logger.info("[LocalNameGenerator] Generating new name...");

        if (usedNames.size() >= firstNames.size() * lastNames.size()) {
            Specialization.logger.severe("[LocalNameGenerator] All possible name combinations have been used!");
            throw new NoSuchElementException("All possible name combinations have been used");
        }

        int attempts = 0;
        int maxAttempts = firstNames.size() * lastNames.size() * 8; // Safety limit

        while (attempts < maxAttempts) {

            NameRoll first = generateNameRoll(firstNames, null); //initialize with a first name roll
            NameRoll last = generateNameRoll(lastNames, first); //generates the last name with the first name as context
            String name = first.getName() + "_" + last.getName();

            if (name.length() <= 16 && usedNames.add(name)) {
                return name;
            }

            attempts++;
        }

        Specialization.logger.severe("[LocalNameGenerator] FAILED: Could not generate a unique valid name after " + maxAttempts + " attempts");
        throw new NoSuchElementException("Could not generate a unique valid name after " + maxAttempts + " attempts");
    }


    /**
     * A helper method that captures the nested name candidates within brackets
     * for example: {car|bicycle|truck} will have a 33% chance of drawing any of those
     * <p>
     * tags: [nature] [abyss] adding this anywhere to a name entry will assign it to that group.
     * If a last name has this tag, then it is compatible. Not adding a tag will be compatible with anything.
     */
    private NameRoll generateNameRoll(List<String> list, NameRoll roll_context) {
        NameRoll new_roll = new NameRoll();
        String group = null;
        String line;

        if (roll_context == null) {
            //this only happen for first names
            line = list.get(random.nextInt(list.size()));
            List<String> matches = getGroupPattern(line);

            if (!matches.isEmpty()) {
                new_roll.groups = matches;
            }
        } else {
            //this only happens when working on a last name
            group = roll_context.getRandomGroup(); //this gets the groupings set by the first name
            if (group != null && grouping.containsKey(group) && !grouping.get(group).isEmpty()) {
                List<String> options_for_name = grouping.get(group);
                line = options_for_name.get(random.nextInt(options_for_name.size()));
            } else {
                line = list.get(random.nextInt(list.size()));
            }
        }

        line = line.trim();
        if (line.isEmpty()) {
            return generateNameRoll(list, roll_context); // retry if empty
        }
        //Get the contents within brackets.
        if (line.startsWith("{")) {
            line = line.replace("{", "").replace("}", "");
            String[] line_split = line.split("\\|");

            // Remove empty options
            List<String> nonEmptyOptions = new ArrayList<>();
            for (String opt : line_split) {
                if (!opt.trim().isEmpty()) nonEmptyOptions.add(opt.trim());
            }

            int roll_int = new Random().nextInt(nonEmptyOptions.size());
            line = nonEmptyOptions.get(roll_int);
        }

        new_roll.name = line;
        return new_roll;
    }

    protected static class NameRoll {
        @Getter
        String name;
        @Getter
        List<String> groups;

        public String getRandomGroup() {
            if (groups == null || groups.isEmpty()) return null;
            return groups.get(new Random().nextInt(groups.size()));
        }
    }

}
