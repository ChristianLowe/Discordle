package io.chrislowe.discordle.game.words;

import com.google.common.base.Strings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
public class Dictionary {
    private static final int expectedDictionarySize = 13_000;

    private final Map<String, String> dictionary;

    public Dictionary() {
        try {
            InputStream input = Dictionary.class.getResourceAsStream("/words/dictionary.txt");
            if (input == null) {
                throw new IOException("dictionary.txt not found in resources");
            }

            dictionary = new HashMap<>(expectedDictionarySize);

            var scanner = new Scanner(input);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                String word = line.substring(0, 5);
                String definition = line.substring(6).replaceAll("\\\\n", "\n");
                dictionary.put(word, definition);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read dictionary from resources", e);
        }
    }

    public boolean isValidWord(String word) {
        return dictionary.containsKey(word);
    }

    public String getDefinition(String word) {
        String definition = dictionary.get(word);
        if (Strings.isNullOrEmpty(definition)) {
            return word + " is a strange word - I have no idea what it means.";
        } else {
            return word + " definition:\n" + definition;
        }
    }
}
