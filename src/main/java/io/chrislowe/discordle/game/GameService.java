package io.chrislowe.discordle.game;

import com.google.common.collect.Lists;
import io.chrislowe.discordle.database.dbo.Game;
import io.chrislowe.discordle.database.dbo.GameMove;
import io.chrislowe.discordle.database.dbo.User;
import io.chrislowe.discordle.database.enums.GameStatus;
import io.chrislowe.discordle.database.service.DatabaseService;
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

        logger.info("Player {} guesses {}", discordId, guess);
        databaseService.submitWord(game, user, guess);

        var wordGuess = new WordGuess(guess, game.getWord());

        GameStatus gameStatus;
        if (wordGuess.isCorrectAnswer()) {
            gameStatus = GameStatus.WIN;
        } else if (game.getGameMoves().size() == 6) {
            gameStatus = GameStatus.LOSE;
        } else {
            gameStatus = GameStatus.ACTIVE;
        }

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

    @Autowired
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Autowired
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
}
