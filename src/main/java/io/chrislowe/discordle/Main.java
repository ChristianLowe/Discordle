package io.chrislowe.discordle;

import com.google.common.base.Strings;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.chrislowe.discordle.game.GameManager;
import io.chrislowe.discordle.game.SubmissionOutcome;
import io.chrislowe.discordle.game.guess.LetterGuess;
import io.chrislowe.discordle.game.guess.LetterState;
import io.chrislowe.discordle.game.guess.WordGuess;
import io.chrislowe.discordle.util.FixedTimeScheduler;
import io.chrislowe.discordle.util.WordGraphicBuilder;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private GameManager gameManager;

    @Value("${DISCORDLE_ADMIN_ID}")
    private String adminId;

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

    public void registerCommands(GatewayDiscordClient gateway) {
        long applicationId = gateway.getRestClient().getApplicationId().blockOptional().orElseThrow();

        ApplicationCommandRequest restartCommandRequest = ApplicationCommandRequest.builder()
                .name("restart")
                .description("Manually resets the state of the Game")
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

        ApplicationCommandRequest keyboardCommandRequest = ApplicationCommandRequest.builder()
            .name("keyboard")
            .description("View the current keyboard!")
            .build();
        gateway.getRestClient().getApplicationService()
            .createGlobalApplicationCommand(applicationId, keyboardCommandRequest)
            .subscribe();

        gateway.on(ChatInputInteractionEvent.class, this::handleInteractionEvent).subscribe();
    }

    @Bean
    public FixedTimeScheduler scheduler() {
        var scheduler = new FixedTimeScheduler(gameManager::startNewGame);
        scheduler.addDailyExecution(LocalTime.NOON);
        scheduler.addDailyExecution(LocalTime.MIDNIGHT);
        return scheduler;
    }

    public Mono<Void> handleInteractionEvent(ChatInputInteractionEvent event) {
        String command = event.getCommandName();
        logger.info("Command received: {}", command);
        return switch (command) {
            case "restart" -> {
                String playerId = event.getInteraction().getUser().getId().asString();
                if (!playerId.equals(adminId)) {
                    yield event.reply("Unauthorized to perform this action");
                } else {
                    gameManager.startNewGame();
                    yield event.reply("Game restarted");
                }
            }
            case "submit" -> {
                String playerId = event.getInteraction().getUser().getId().asString();
                String word = event
                        .getOption("word").orElseThrow()
                        .getValue().orElseThrow()
                        .asString().toUpperCase(Locale.ROOT);

                SubmissionOutcome outcome = gameManager.submitGuess(playerId, word);
                String response = switch (outcome) {
                    case ACCEPTED, GAME_WON, GAME_LOST -> null;
                    case INVALID_WORD -> "Your word is not in the dictionary.";
                    case GAME_UNAVAILABLE -> "There is currently no game going on. Games begin at 12AM/12PM PST.";
                    case ALREADY_SUBMITTED -> "You've already submitted a word for this game.";
                    case NOT_ENOUGH_LETTERS, TOO_MANY_LETTERS -> "Your submission must have 5 letters";
                };

                if (response != null) {
                    yield event.reply(response);
                } else {
                    yield event.deferReply().then(createGameBoardFollowup(event, getDescriptionForOutcome(outcome)));
                }
            }
            case "keyboard" -> event.deferReply().then(createKeyBoardFollowup(event));
            default -> throw new UnsupportedOperationException("Unknown command: " + command);
        };
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public String getDescriptionForOutcome(SubmissionOutcome outcome) {
        return switch (outcome) {
            case GAME_LOST -> String.format("The correct word was %s.", gameManager.getTargetWord());
            default -> "";
        };
    }

    public Mono<Void> createGameBoardFollowup(ChatInputInteractionEvent event, String response) {
        byte[] gameImage = new WordGraphicBuilder(5, 6)
                .addWordGuesses(gameManager.getWordGuesses())
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

    public Mono<Void> createKeyBoardFollowup(ChatInputInteractionEvent event) {
        String[] keyboardRows = {
            "QWERTYUIOP",
            "ASDFGHJKL",
            "ZXCVBNM"
        };
        
        var letterStates = new HashMap<Character, LetterState>();
        for (var guess : gameManager.getWordGuesses()) {
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
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
}
