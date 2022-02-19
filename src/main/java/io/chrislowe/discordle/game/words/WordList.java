package io.chrislowe.discordle.game.words;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WordList {
    private final List<String> words;

    public WordList() {
        try {
            InputStream input = WordList.class.getResourceAsStream("/wordlist.txt");
            if (input == null) {
                throw new IOException("wordlist.txt not found in resources");
            }
            var scanner = new Scanner(input);

            words = new ArrayList<>();
            while (scanner.hasNext()) {
                words.add(scanner.next());
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read wordlist from resources", e);
        }
    }

    public String getRandomWord() {
        int index = ThreadLocalRandom.current().nextInt(words.size());
        return words.get(index).toUpperCase(Locale.ROOT);
    }
}
