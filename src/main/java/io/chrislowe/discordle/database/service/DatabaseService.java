package io.chrislowe.discordle.database.service;

import com.google.common.collect.Lists;
import io.chrislowe.discordle.database.dbo.Game;
import io.chrislowe.discordle.database.dbo.GameMove;
import io.chrislowe.discordle.database.dbo.Guild;
import io.chrislowe.discordle.database.dbo.User;
import io.chrislowe.discordle.database.enums.GameStatus;
import io.chrislowe.discordle.database.repository.GameMoveRepository;
import io.chrislowe.discordle.database.repository.GameRepository;
import io.chrislowe.discordle.database.repository.GuildRepository;
import io.chrislowe.discordle.database.repository.UserRepository;
import io.chrislowe.discordle.game.words.WordList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class DatabaseService {
    private UserRepository userRepository;
    private GameRepository gameRepository;
    private GuildRepository guildRepository;
    private GameMoveRepository gameMoveRepository;

    private WordList wordList;

    public void resetActiveGames() {
        gameRepository.resetActiveGames();
        guildRepository.resetActiveGuilds();
    }

    public void submitWord(Game game, User user, String word) {
        var gameMove = new GameMove();
        gameMove.setGame(game);
        gameMove.setUser(user);
        gameMove.setWord(word);
        gameMove.setNewYellowsGuessed((byte)0); // TODO
        gameMove.setNewGreensGuessed((byte)0); // TODO
        gameMove.setDatetimeCreated(Instant.now());
        gameMoveRepository.save(gameMove);

        game.getGameMoves().add(gameMove);
        gameRepository.save(game);
    }

    public Game getActiveGuildGame(String guildId) {
        Guild guild = guildRepository.findById(guildId).orElseGet(() -> addGuild(guildId));
        return gameRepository.findActiveGameForGuild(guild).orElseGet(() -> createGame(guildId));
    }

    public Game getLatestGuildGame(String guildId) {
        Guild guild = guildRepository.findById(guildId).orElseGet(() -> addGuild(guildId));
        return gameRepository.findTopByGuildOrderByDatetimeCreatedDesc(guild).orElseGet(() -> createGame(guildId));
    }

    public User getUser(String discordId) {
        return userRepository.findById(discordId).orElseGet(() -> addUser(discordId));
    }

    private Game createGame(String guildId) {
        Guild guild = guildRepository.findById(guildId).orElseGet(() -> addGuild(guildId));
        if (guild.hasCurrentGame()) {
            return null;
        } else {
            guild.setHasCurrentGame(true);
            guildRepository.save(guild);
        }

        var game = new Game();
        game.setGuild(guild);
        game.setGameMoves(Lists.newArrayList());
        game.setWord(wordList.getRandomWord());
        game.setStatus(GameStatus.ACTIVE);
        game.setDatetimeCreated(Instant.now());
        return gameRepository.save(game);
    }

    private User addUser(String discordId) {
        var user = new User();
        user.setDiscordId(discordId);
        user.setAdmin(false);
        user.setDatetimeCreated(Instant.now());
        return userRepository.save(user);
    }

    private Guild addGuild(String guildId) {
        var guild = new Guild();
        guild.setGuildId(guildId);
        guild.setHasCurrentGame(false);
        guild.setDatetimeCreated(Instant.now());
        return guildRepository.save(guild);
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setGameRepository(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Autowired
    public void setGuildRepository(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    @Autowired
    public void setGameMoveRepository(GameMoveRepository gameMoveRepository) {
        this.gameMoveRepository = gameMoveRepository;
    }

    @Autowired
    public void setWordList(WordList wordList) {
        this.wordList = wordList;
    }

}
