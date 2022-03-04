package io.chrislowe.discordle.game.words;

import com.google.common.hash.BloomFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;

@SuppressWarnings("UnstableApiUsage")
public class Dictionary {
    private final BloomFilter<String> bloomFilter;

    public Dictionary() {
        try {
            InputStream input = Dictionary.class.getResourceAsStream("/words/dictionary.txt");
            if (input == null) {
                throw new IOException("dictionary.txt not found in resources");
            }
            var scanner = new Scanner(input);

            bloomFilter = BloomFilter.create((word, into) -> into.putUnencodedChars(word), 13_000);
            while (scanner.hasNext()) {
                bloomFilter.put(scanner.next());
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read dictionary from resources", e);
        }
    }

    public boolean isValidWord(String word) {
        return bloomFilter.mightContain(word.toLowerCase(Locale.ROOT));
    }
}
