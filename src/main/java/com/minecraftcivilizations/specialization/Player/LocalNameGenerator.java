package com.minecraftcivilizations.specialization.Player;

import com.minecraftcivilizations.specialization.Specialization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

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
    }

    /** @return a randomly-picked “FirstName LastName” */
    public String nextName() throws NoSuchElementException {
        if (usedNames.size() >= firstNames.size() * lastNames.size()) {
            throw new NoSuchElementException();
        }
        while (true) {
            String first = firstNames.get(random.nextInt(firstNames.size()));
            String last = lastNames.get(random.nextInt(lastNames.size()));
            String name = first + "_" + last;
            if (usedNames.add(name)) {
                return name;
            }
        }
    }
}
