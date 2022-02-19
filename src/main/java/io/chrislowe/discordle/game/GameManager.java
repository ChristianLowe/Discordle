package io.chrislowe.discordle.game;

import io.chrislowe.discordle.game.words.Dictionary;
import io.chrislowe.discordle.game.words.WordList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class GameManager {
    private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

    private final Dictionary dictionary;
    private final WordList wordList;
    private Set<String> activePlayerIds;
    private Game game;

    public GameManager() {
        dictionary = new Dictionary();
        wordList = new WordList();
        startNewGame();
    }

    public void startNewGame() {
        logger.info("Starting new game");
        activePlayerIds = new HashSet<>();
        game = new Game(wordList.getRandomWord());
    }

    public SubmissionOutcome submitGuess(String playerId, String guess) {
        if (game == null) {
            return SubmissionOutcome.GAME_UNAVAILABLE;
        } else if (activePlayerIds.contains(playerId)) {
            return SubmissionOutcome.ALREADY_SUBMITTED;
        } else if (guess == null || guess.length() < 5) {
            return SubmissionOutcome.NOT_ENOUGH_LETTERS;
        } else if (guess.length() > 5) {
            return SubmissionOutcome.TOO_MANY_LETTERS;
        } else if (!dictionary.isValidWord(guess)) {
            return SubmissionOutcome.INVALID_WORD;
        }

        logger.info("Player {} guesses {}", playerId, guess);
        activePlayerIds.add(playerId);

        Game.GameStatus gameStatus = game.addGuess(guess);
        if (gameStatus == Game.GameStatus.PLAYING) {
            return SubmissionOutcome.ACCEPTED;
        } else {
            game = null;
            if (gameStatus == Game.GameStatus.WON) {
                return SubmissionOutcome.GAME_WON;
            } else {
                return SubmissionOutcome.GAME_LOST;
            }
        }
    }

    public String getGuesses() {
        return game.toString();
    }
}
