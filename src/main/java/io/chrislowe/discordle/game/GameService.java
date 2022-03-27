package io.chrislowe.discordle.game;

import com.google.common.collect.Lists;
import io.chrislowe.discordle.database.dbo.Game;
import io.chrislowe.discordle.database.dbo.GameMove;
import io.chrislowe.discordle.database.dbo.User;
import io.chrislowe.discordle.database.enums.GameStatus;
import io.chrislowe.discordle.database.service.DatabaseService;
import io.chrislowe.discordle.game.guess.LetterGuess;
import io.chrislowe.discordle.game.guess.LetterState;
import io.chrislowe.discordle.game.guess.WordGuess;
import io.chrislowe.discordle.game.words.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private Dictionary dictionary;
    private DatabaseService databaseService;

    public SubmissionOutcome submitGuess(String guildId, String discordId, String guess) {
        logger.info("Player {} guesses {}", discordId, guess);

        User user = databaseService.getUser(discordId);
        Game game = databaseService.getActiveGuildGame(guildId);
        if (game == null) {
            return SubmissionOutcome.GAME_UNAVAILABLE;
        } else if (userAlreadySubmitted(user, game)) {
            return SubmissionOutcome.ALREADY_SUBMITTED;
        } else if (guess == null || guess.length() < 5) {
            return SubmissionOutcome.NOT_ENOUGH_LETTERS;
        } else if (guess.length() > 5) {
            return SubmissionOutcome.TOO_MANY_LETTERS;
        } else if (!dictionary.isValidWord(guess)) {
            return SubmissionOutcome.INVALID_WORD;
        }

        var wordGuess = new WordGuess(guess, game.getWord());

        GameStatus gameStatus;
        if (wordGuess.isCorrectAnswer()) {
            gameStatus = GameStatus.WIN;
        } else if (game.getGameMoves().size() == 6) {
            gameStatus = GameStatus.LOSE;
        } else {
            gameStatus = GameStatus.ACTIVE;
        }

        byte newYellows = getNewLetterStateCount(game.getGameMoves(), wordGuess, LetterState.MISMATCH);
        byte newGreens = getNewLetterStateCount(game.getGameMoves(), wordGuess, LetterState.CORRECT);
        databaseService.submitWord(game, user, guess, newYellows, newGreens);

        game.setStatus(gameStatus);
        return switch (gameStatus) {
            case WIN -> SubmissionOutcome.GAME_WON;
            case LOSE -> SubmissionOutcome.GAME_LOST;
            default -> SubmissionOutcome.ACCEPTED;
        };
    }

    public List<WordGuess> getWordGuesses(String guildId) {
        Game game = databaseService.getLatestGuildGame(guildId);
        if (game == null) {
            return Lists.newArrayList();
        }

        return databaseService.getLatestGuildGame(guildId)
                .getGameMoves().stream()
                .map(GameMove::getWord)
                .map(guess -> new WordGuess(guess, game.getWord()))
                .collect(Collectors.toList());
    }

    public String getTargetWord(String guildId) {
        Game game = databaseService.getLatestGuildGame(guildId);
        return game != null ? game.getWord() : null;
    }

    private boolean userAlreadySubmitted(User user, Game game) {
        return game.getGameMoves().stream()
                .map(GameMove::getUser)
                .anyMatch(user::equals);
    }

    private byte getNewLetterStateCount(List<GameMove> gameMoves, WordGuess wordGuess, LetterState desiredLetterState) {
        byte count = 0;

        wordLetterLoop: for (int i = 0; i < wordGuess.size(); i++) {
            LetterGuess letterGuess = wordGuess.getLetterGuess(i);
            if (letterGuess.state() == desiredLetterState) {
                // Make sure this letter is a novel guess before giving the score
                for (var gameMove : gameMoves) {
                    if (gameMove.getWord().charAt(i) == letterGuess.letter()) {
                        // This letter was already guessed at this position
                        continue wordLetterLoop;
                    }
                }
                count++;
            }
        }

        return count;
    }

    public void rebuildDatabaseStats() {
        for (Game game : databaseService.getAllGames()) {
            List<GameMove> allGameMoves = game.getGameMoves();
            for (int i = 0; i < allGameMoves.size(); i++) {
                GameMove gameMove = allGameMoves.get(i);
                WordGuess wordGuess = new WordGuess(gameMove.getWord(), game.getWord());
                List<GameMove> previousMoves = allGameMoves.subList(0, i);

                gameMove.setNewYellowsGuessed(getNewLetterStateCount(previousMoves, wordGuess, LetterState.MISMATCH));
                gameMove.setNewGreensGuessed(getNewLetterStateCount(previousMoves, wordGuess, LetterState.CORRECT));
            }
            databaseService.updateGame(game);
        }
    }

    @Autowired
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Autowired
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
}
