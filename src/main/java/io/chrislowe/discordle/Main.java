package io.chrislowe.discordle;

import com.google.common.base.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.chrislowe.discordle.database.dbo.Game;
import io.chrislowe.discordle.database.dbo.GameMove;
import io.chrislowe.discordle.database.dbo.User;
import io.chrislowe.discordle.database.dto.UserStats;
import io.chrislowe.discordle.database.service.DatabaseService;
import io.chrislowe.discordle.game.GameService;
import io.chrislowe.discordle.game.SubmissionOutcome;
import io.chrislowe.discordle.game.guess.LetterGuess;
import io.chrislowe.discordle.game.guess.LetterState;
import io.chrislowe.discordle.game.guess.WordGuess;
import io.chrislowe.discordle.game.words.Dictionary;
import io.chrislowe.discordle.game.words.WordList;
import io.chrislowe.discordle.util.FixedTimeScheduler;
import io.chrislowe.discordle.util.WordGraphicBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@SpringBootApplication
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private WordList wordList;
    private Dictionary dictionary;
    private GameService gameService;
    private DatabaseService databaseService;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public GatewayDiscordClient gateway(@Value("${DISCORDLE_TOKEN}") String token) {
        if (Strings.isNullOrEmpty(token)) {
            throw new RuntimeException("DISCORDLE_TOKEN must be set in your environmental variables");
        }

        GatewayDiscordClient gateway = DiscordClient.create(token).login().block(Duration.ofSeconds(30));
        if (gateway == null) {
            throw new RuntimeException("Failed to instantiate client gateway");
        }

        registerCommands(gateway);
        return gateway;
    }

    @Bean
    public FixedTimeScheduler scheduler() {
        var scheduler = new FixedTimeScheduler(() -> logger.debug("keepalive"));
        scheduler.addDailyExecution(LocalTime.MIDNIGHT);
        return scheduler;
    }

    public void registerCommands(GatewayDiscordClient gateway) {
        long applicationId = gateway.getRestClient().getApplicationId().blockOptional().orElseThrow();

        ApplicationCommandRequest restartCommandRequest = ApplicationCommandRequest.builder()
                .name("restart")
                .description("Manually resets the state of the games for all guilds")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("rebuild-stats")
                        .description("Rebuild database recorded statistics")
                        .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                        .required(false)
                        .build())
                .build();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, restartCommandRequest)
                .subscribe();

        ApplicationCommandRequest submitCommandRequest = ApplicationCommandRequest.builder()
                .name("submit")
                .description("Submit a wordle!")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("word")
                        .description("A 5-letter word of your favor")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, submitCommandRequest)
                .subscribe();

        ApplicationCommandRequest pollCommandRequest = ApplicationCommandRequest.builder()
                .name("poll")
                .description("Start a poll to add a word to the dictionary!")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("word")
                        .description("A 5-letter word of your favor")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, pollCommandRequest)
                .subscribe();

        ApplicationCommandRequest keyboardCommandRequest = ApplicationCommandRequest.builder()
            .name("keyboard")
            .description("View the current keyboard!")
            .build();
        gateway.getRestClient().getApplicationService()
            .createGlobalApplicationCommand(applicationId, keyboardCommandRequest)
            .subscribe();

        ApplicationCommandRequest userCommandRequest = ApplicationCommandRequest.builder()
                .name("user")
                .description("View game stats for a given user")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("User to view (defaults to yourself)")
                        .type(ApplicationCommandOption.Type.USER.getValue())
                        .required(false)
                        .build())
                .build();
        gateway.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, userCommandRequest)
                .subscribe();

        ApplicationCommandRequest recentCommandRequest = ApplicationCommandRequest.builder()
            .name("recent")
            .description("View recent games!")
            .build();
        gateway.getRestClient().getApplicationService()
            .createGlobalApplicationCommand(applicationId, recentCommandRequest)
            .subscribe();

        gateway.on(ChatInputInteractionEvent.class, event -> handleInteractionEvent(event, gateway)).subscribe();
    }

    public Mono<Void> handleInteractionEvent(ChatInputInteractionEvent event, GatewayDiscordClient gateway) {
        String command = event.getCommandName();
        logger.info("Command received: {}", command);

        String discordId = event.getInteraction().getUser().getId().asString();

        return switch (command) {
            case "restart" -> {
                User user = databaseService.getUser(discordId);
                if (user.isAdmin() != null && user.isAdmin()) {
                    event.deferReply();

                    boolean rebuildStats = event.getOption("rebuild-stats")
                                    .flatMap(ApplicationCommandInteractionOption::getValue)
                                    .map(ApplicationCommandInteractionOptionValue::asBoolean)
                                    .orElse(false);
                    if (rebuildStats) {
                        gameService.rebuildDatabaseStats();
                        yield event.reply("Database stats rebuilt");
                    } else {
                        databaseService.resetActiveGames();
                        yield event.reply("Games reset");
                    }
                } else {
                    yield event.reply("Unauthorized to perform this action");
                }
            }
            case "submit" -> {
                String guildId = event.getInteraction().getGuildId().map(Snowflake::asString).orElse(null);
                if (guildId == null) {
                    yield event.reply("You must run this command in a discord server");
                }

                String word = event
                        .getOption("word").orElseThrow()
                        .getValue().orElseThrow()
                        .asString().toUpperCase(Locale.ROOT);

                SubmissionOutcome outcome = gameService.submitGuess(guildId, discordId, word);
                String response = switch (outcome) {
                    case ACCEPTED, GAME_WON, GAME_LOST -> null;
                    case INVALID_WORD -> "Your word is not in the dictionary.";
                    case GUILD_COOLDOWN -> "You've already submitted recently in this guild!";
                    case ALREADY_SUBMITTED -> "You've already submitted a word for this game.";
                    case NOT_ENOUGH_LETTERS, TOO_MANY_LETTERS -> "Your submission must have 5 letters";
                };

                if (response != null) {
                    if (outcome == SubmissionOutcome.INVALID_WORD) {
                        yield event.reply(response).withEphemeral(true);
                    }
                    if (outcome == SubmissionOutcome.GUILD_COOLDOWN) {
                        User user = databaseService.getUser(discordId);
                        Game game = databaseService.getActiveGuildGame(guildId);
                        Duration remaining = gameService.remainingCooldown(
                            user,
                            game.getGuild());
                        yield event.reply(
                                response + format(" Remaining: %02d:%02d",
                                                  remaining.toHours(),
                                                  remaining.toMinutesPart()))
                            .withEphemeral(true);
                    }
                    yield event.reply(response);
                } else {
                    String description = getDescriptionForOutcome(outcome, word, guildId);
                    yield event.deferReply().then(createGameBoardFollowup(event, guildId, description));
                }
            }
            case "poll" -> {
                String guildId = event.getInteraction().getGuildId().map(Snowflake::asString).orElse(null);
                if (guildId == null) {
                    yield event.reply("You must run this command in a discord server");
                }

                String word = event
                        .getOption("word").orElseThrow()
                        .getValue().orElseThrow()
                        .asString().toUpperCase(Locale.ROOT);
                if (word.length() != 5) {
                    yield event.reply("Your requested word must be 5 characters long")
                            .withEphemeral(true);
                }

                boolean inWordList = wordList.isValidWord(word);
                boolean inDictionary = dictionary.isValidWord(word);
                if (inWordList && inDictionary) {
                    yield event.reply("This word is already in the dictionary and word list")
                            .withEphemeral(true);
                }

                String userId = event.getOption("user")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(value -> value.asUser().block())
                        .map(user -> user.getId().asString())
                        .orElse(discordId);
                String userName = gateway.getRestClient()
                        .getUserById(Snowflake.of(userId))
                        .getData().block()
                        .username();

                StringJoiner response = new StringJoiner("\n");
                response.add(userName + " wants to add the word " + word + " to Discordle!");
                if (inDictionary) {
                    response.add("This word is currently valid to submit, but will never be chosen as the solution.");
                } else {
                    response.add("This word currently can neither be submitted nor randomly chosen as the solution.");
                }
                response.add("Please vote whether you think this is a good word to add.");

                event.reply(response.toString()).subscribe();
                var message = event.getReply().block();
                message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4D")).block(); // Thumbs Up
                message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4E")).block(); // Thumbs Down
                yield Mono.empty();
            }
            case "keyboard" -> {
                String guildId = event.getInteraction().getGuildId().map(Snowflake::asString).orElse(null);
                if (guildId == null) {
                    yield event.reply("You must run this command in a discord server");
                }

                yield event.deferReply().then(createKeyBoardFollowup(event, guildId));
            }
            case "user" -> {
                String userId = event.getOption("user")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(value -> value.asUser().block())
                        .map(user -> user.getId().asString())
                        .orElse(discordId);
                String userName = gateway.getRestClient()
                        .getUserById(Snowflake.of(userId))
                        .getData().block()
                        .username();

                UserStats userStats = databaseService.getUserStats(userId);
                int totalGames = userStats.getGamesLost() + userStats.getGamesWon();
                float winPercent = 100 * (userStats.getGamesWon() / (float)((totalGames != 0) ? totalGames : 1));

                StringJoiner response = new StringJoiner("\n");
                response.add("Statistics for user " + userName);
                response.add(userStats.getYellowsGuessed() + " new yellows guessed");
                response.add(userStats.getGreensGuessed() + " new greens guessed");
                response.add(userStats.getGolfScore() + " away from par (4)");
                if (userStats.getTopWord() != null) {
                    Pair<String, Integer> topWord = userStats.getTopWord();
                    response.add(format(
                        "Guessed %s %d times",
                        topWord.getFirst(),
                        topWord.getSecond()));
                }
                response.add(format("%d/%d games won (%.2f%%).",
                    userStats.getGamesWon(),
                    totalGames,
                    winPercent));
                yield event.reply(response.toString());
            }
            case "recent" -> {
                String guildId = event.getInteraction().getGuildId().map(Snowflake::asString).orElse(null);
                if (guildId == null) {
                    yield event.reply("You must run this command in a discord server");
                }
                StringJoiner response = new StringJoiner("\n");
                response.add("Recent games:");
                List<Game> recentGames = databaseService.getRecentCompletedGames(guildId);
                if (recentGames.isEmpty()) {
                    response.add("No recent games found!");
                }
                for (Game game : recentGames) {
                    List<GameMove> gameMoves = game.getGameMoves();
                    response.add(
                        format("%d guesses ending with %s, answer was %s",
                            gameMoves.size(),
                            gameMoves.get(gameMoves.size() - 1).getWord(),
                            game.getWord()));
                }
                yield event.reply(response.toString());
            }
            default -> throw new UnsupportedOperationException("Unknown command: " + command);
        };
    }

    public String getDescriptionForOutcome(SubmissionOutcome outcome, String submissionWord, String guildId) {
        var description = new StringBuilder();
        if (outcome == SubmissionOutcome.GAME_WON) {
            description.append("Winner winner chicken dinner! 🎉\n\n");
        } else if (outcome == SubmissionOutcome.GAME_LOST) {
            String targetWord = gameService.getTargetWord(guildId);
            description.append("The correct word was ").append(targetWord).append(".\n\n");
            description.append(dictionary.getDefinition(targetWord)).append("\n\n");
        }
        description.append(dictionary.getDefinition(submissionWord));

        return description.toString();
    }

    public Mono<Void> createGameBoardFollowup(ChatInputInteractionEvent event, String guildId, String response) {
        byte[] gameImage = new WordGraphicBuilder(5, 6)
                .addWordGuesses(gameService.getWordGuesses(guildId))
                .buildAsPng();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .image("attachment://game-board.png")
                .description(response)
                .build();

        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                .addFile("game-board.png", new ByteArrayInputStream(gameImage))
                .addEmbed(embed)
                .build()).then();
    }

    public Mono<Void> createKeyBoardFollowup(ChatInputInteractionEvent event, String guildId) {
        String[] keyboardRows = {
            "QWERTYUIOP",
            "ASDFGHJKL",
            "ZXCVBNM"
        };

        var letterStates = new HashMap<Character, LetterState>();
        for (var guess : gameService.getWordGuesses(guildId)) {
            for (LetterGuess letterGuess : guess) {
                letterStates.merge(letterGuess.letter(), letterGuess.state(), (a, b) -> {
                    if (a == LetterState.CORRECT || b == LetterState.CORRECT) {
                        return LetterState.CORRECT;
                    } else if (a == LetterState.MISMATCH || b == LetterState.MISMATCH) {
                        return LetterState.MISMATCH;
                    } else {
                        return LetterState.MISSING;
                    }
                });
            }
        }

        byte[] gameImage = new WordGraphicBuilder(10, 3)
            .addWordGuesses(Arrays.stream(keyboardRows).sequential().map(keyRow ->
                new WordGuess(keyRow.chars()
                    .mapToObj((int ch) -> new LetterGuess((char) ch,
                        letterStates.getOrDefault((char) ch, null)))
                    .toArray(LetterGuess[]::new)
                )).collect(Collectors.toList()))
            .buildAsPng();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
            .image("attachment://key-board.png")
            .build();

        return event.createFollowup(InteractionFollowupCreateSpec.builder()
            .addFile("key-board.png", new ByteArrayInputStream(gameImage))
            .addEmbed(embed)
            .build()).then();
    }

    @Autowired
    public void setWordList(WordList wordList) {
        this.wordList = wordList;
    }

    @Autowired
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Autowired
    public void setGameService(GameService gameService) {
        this.gameService = gameService;
    }

    @Autowired
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
}
