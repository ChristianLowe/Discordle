package io.chrislowe.discordle.database.dbo;

import com.google.common.collect.Range;
import discord4j.core.GatewayDiscordClient;
import io.chrislowe.discordle.database.enums.GameStatus;
import io.chrislowe.discordle.database.repository.GameMoveRepository;
import io.chrislowe.discordle.database.repository.GameRepository;
import io.chrislowe.discordle.database.repository.GuildRepository;
import io.chrislowe.discordle.database.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

@SpringBootTest()
class GameMoveTest {

    @MockBean
    GatewayDiscordClient discordClient;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GuildRepository guildRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GameMoveRepository gameMoveRepository;

    AtomicInteger nextUserId;
    AtomicInteger nextGuildId;
    AtomicInteger nextGameId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        nextUserId = new AtomicInteger(1000);
        nextGuildId = new AtomicInteger(2000);
        nextGameId = new AtomicInteger(3000);
    }

    private User createUserForTest(Clock clock) {
        User user = new User();
        user.setAdmin(false);
        user.setDiscordId("" + nextUserId.incrementAndGet());
        user.setDatetimeCreated(clock.instant());
        return userRepository.save(user);
    }

    private Guild createGuildForTest(Clock clock) {
        Guild guild = new Guild();
        guild.setGuildId("" + nextGuildId.incrementAndGet());
        guild.setHasCurrentGame(true);
        guild.setDatetimeCreated(clock.instant());
        return guildRepository.save(guild);
    }

    private Game createGameForTest(Clock clock, Guild guild, String word) {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setWord(word);
        game.setDatetimeCreated(clock.instant());
        game.setGameId(nextGameId.incrementAndGet());
        game.setGuild(guild);
        return gameRepository.save(game);
    }

    private GameMove createGameMoveForTest(Clock clock, User user, Game game,
                                           String word, byte yellow,
                                           byte green) {
        GameMove move = new GameMove();
        move.setUser(user);
        move.setGame(game);
        move.setDatetimeCreated(clock.instant());
        move.setWord(word);
        move.setNewYellowsGuessed(yellow);
        move.setNewGreensGuessed(green);
        return gameMoveRepository.save(move);
    }

    @Test
    public void testUserLastGuess() {
        Clock initialClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        User user = createUserForTest(initialClock);
        Guild guild = createGuildForTest(initialClock);
        Guild otherGuild = createGuildForTest(initialClock);
        Game game = createGameForTest(initialClock, guild, "UNITS");
        Game otherGame = createGameForTest(initialClock, otherGuild, "OTHER");

        Clock earlyMoveClock =
            Clock.offset(initialClock, Duration.ofMinutes(5));
        Clock laterMoveClock =
            Clock.offset(initialClock, Duration.ofMinutes(10));
        Clock wrongMoveClock =
            Clock.offset(initialClock, Duration.ofMinutes(15));

        createGameMoveForTest(earlyMoveClock, user, game, "EARLY", (byte) 0,
                              (byte) 0);

        createGameMoveForTest(laterMoveClock, user, game, "LATER", (byte) 0,
                              (byte) 0);

        createGameMoveForTest(wrongMoveClock, user, otherGame, "WRONG",
                              (byte) 0, (byte) 0);

        Optional<GameMove> lastMove =
            gameMoveRepository.findTopByGame_GuildAndUserOrderByDatetimeCreatedDesc(
                guild, user);

        assertThat(lastMove).isPresent();

        Instant expected = laterMoveClock.instant();
        assertThat(lastMove.get().getDatetimeCreated()).isIn(
            Range.open(expected.minus(Duration.ofMillis(1L)),
                       expected.plus(Duration.ofMillis(1L))));
    }
}
