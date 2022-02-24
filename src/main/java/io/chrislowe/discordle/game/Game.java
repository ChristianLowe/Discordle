package io.chrislowe.discordle.game;

import io.chrislowe.discordle.game.guess.WordGuess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Game {
    public enum GameStatus {
        PLAYING,
        WON,
        LOST
    }

    private final String targetWord;
    private final List<WordGuess> wordGuesses;

    public Game(String targetWord) {
        this.targetWord = targetWord;
        this.wordGuesses = new ArrayList<>();
    }

    public GameStatus addGuess(String guess) {
        var wordGuess = new WordGuess(guess, targetWord);
        wordGuesses.add(wordGuess);

        if (wordGuess.isCorrectAnswer()) {
            return GameStatus.WON;
        } else if (wordGuesses.size() == 6) {
            return GameStatus.LOST;
        } else {
            return GameStatus.PLAYING;
        }
    }

    public List<WordGuess> getWordGuesses() {
        return wordGuesses;
    }

    public String getTargetWord() {
        return targetWord;
    }

    @Override
    public String toString() {
        return wordGuesses.stream().map(WordGuess::toString).collect(Collectors.joining("\n"));
    }
}
