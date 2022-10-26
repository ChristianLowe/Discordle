package io.chrislowe.discordle.database.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.chrislowe.discordle.database.dbo.Game;
import io.chrislowe.discordle.database.dbo.GameMove;
import io.chrislowe.discordle.database.dbo.Guild;
import io.chrislowe.discordle.database.dbo.User;
import io.chrislowe.discordle.database.dto.UserStats;
import io.chrislowe.discordle.database.enums.GameStatus;
import io.chrislowe.discordle.database.repository.GameMoveRepository;
import io.chrislowe.discordle.database.repository.GameRepository;
import io.chrislowe.discordle.database.repository.GuildRepository;
import io.chrislowe.discordle.database.repository.UserRepository;
import io.chrislowe.discordle.game.words.WordList;
import liquibase.repackaged.org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    public void submitWord(Game game, User user, String word, byte newYellows, byte newGreens) {
        var gameMove = new GameMove();
        gameMove.setGame(game);
        gameMove.setUser(user);
        gameMove.setWord(word);
        gameMove.setNewYellowsGuessed(newYellows);
        gameMove.setNewGreensGuessed(newGreens);
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

    public List<Game> getAllGames() {
        return Lists.newArrayList(gameRepository.findAll());
    }

    public User getUser(String discordId) {
        return userRepository.findById(discordId).orElseGet(() -> addUser(discordId));
    }

    public UserStats getUserStats(String discordId) {
        User user = getUser(discordId);

        Set<Game> userGames = Sets.newHashSet();

        Map<String, Integer> freqByWord = new HashMap<>();
        int yellowsGuessed = 0, greensGuessed = 0, golfScore = 0;
        for (GameMove gameMove : IterableUtils.emptyIfNull(user.getGameMoves())) {
            userGames.add(gameMove.getGame());
            yellowsGuessed += gameMove.getNewYellowsGuessed();
            greensGuessed += gameMove.getNewGreensGuessed();
            freqByWord.merge(gameMove.getWord(), 1, Math::addExact);
        }
        Pair<String, Integer> topWord = freqByWord.entrySet().stream()
            .max(Entry.comparingByValue())
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .orElse(null);

        int gamesLost = 0, gamesWon = 0;
        for (Game game : userGames) {
            if (game.getStatus() == GameStatus.LOSE) {
                gamesLost++;
                golfScore = golfScore + 4;
            } else if (game.getStatus() == GameStatus.WIN) {
                gamesWon++;
                golfScore = golfScore + game.getGameMoves().size() - 4 ;
            }
        }

        var userStats = new UserStats(discordId);
        userStats.setYellowsGuessed(yellowsGuessed);
        userStats.setGreensGuessed(greensGuessed);
        userStats.setGamesLost(gamesLost);
        userStats.setGamesWon(gamesWon);
        userStats.setGolfScore(golfScore);
        userStats.setTopWord(topWord);
        return userStats;
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

    public void updateGame(Game game) {
        gameMoveRepository.saveAll(game.getGameMoves());
        gameRepository.save(game);
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

    public List<Game> getRecentCompletedGames(String guildId) {
        Guild guild = guildRepository.findById(guildId).orElseGet(() -> addGuild(guildId));
        return gameRepository.findTop5ByGuildAndStatusNotOrderByDatetimeCreatedDesc(guild, GameStatus.ACTIVE);
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
