package io.chrislowe.discordle.game.guess;

import com.google.common.base.CharMatcher;
import java.util.Arrays;
import java.util.Iterator;

public class WordGuess implements Iterable<LetterGuess> {
    private static final CharMatcher wordMatcher = CharMatcher.ascii().and(CharMatcher.inRange('A', 'Z'));

    private final LetterGuess[] letterGuesses;

    public WordGuess(String guess, String target) {
        if (isInvalidWord(guess) || isInvalidWord(target) || guess.length() != target.length()) {
            throw new RuntimeException("Invalid guess/target: " + guess + "/" + target);
        }

        int[] letterCount = new int[26];
        for (char letter : target.toCharArray()) {
            letterCount[letter - 'A']++;
        }

        int n = guess.length();
        letterGuesses = new LetterGuess[n];

        for (int i = 0; i < n; i++) {
            char letter = guess.charAt(i);
            if (letter == target.charAt(i)) {
                letterGuesses[i] = new LetterGuess(letter, LetterState.CORRECT);
                letterCount[letter - 'A']--;
            }
        }

        for (int i = 0; i < n; i++) {
            if (letterGuesses[i] == null) {
                char letter = guess.charAt(i);
                if (letterCount[letter - 'A']-- > 0) {
                    letterGuesses[i] = new LetterGuess(letter, LetterState.MISMATCH);
                } else {
                    letterGuesses[i] = new LetterGuess(letter, LetterState.MISSING);
                }
            }
        }
    }
    
    public WordGuess(LetterGuess[] letterGuesses) {
        this.letterGuesses = letterGuesses;
    }

    public boolean isCorrectAnswer() {
        return Arrays.stream(letterGuesses).map(LetterGuess::state).allMatch(LetterState.CORRECT::equals);
    }

    public LetterGuess getLetterGuess(int letterIdx) {
        return letterGuesses.length > letterIdx ? letterGuesses[letterIdx] : null;
    }
    
    public int size() {
        return letterGuesses.length;
    }

    @Override
    public Iterator<LetterGuess> iterator() {
        return Arrays.stream(letterGuesses).iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("`");
        for (LetterGuess letterGuess : letterGuesses) {
            char letter = letterGuess.letter();
            LetterState state = letterGuess.state();
            if (state == LetterState.CORRECT) {
                sb.append(' ').append(letter).append(' ');
            } else if (state == LetterState.MISMATCH) {
                sb.append('(').append(Character.toLowerCase(letter)).append(')');
            } else if (state == LetterState.MISSING) {
                sb.append('~').append(Character.toLowerCase(letter)).append('~');
            }
            sb.append(' ');
        }
        return sb.append('`').toString();
    }

    private boolean isInvalidWord(String word) {
        if (word == null) {
            return false;
        } else {
            return !wordMatcher.matchesAllOf(word);
        }
    }
}
