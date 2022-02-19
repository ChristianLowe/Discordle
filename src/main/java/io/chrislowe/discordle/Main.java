package io.chrislowe.discordle;

import com.google.common.base.Strings;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.chrislowe.discordle.game.GameManager;
import io.chrislowe.discordle.game.SubmissionOutcome;
import io.chrislowe.discordle.util.FixedTimeScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String envTokenName = "DISCORDLE_TOKEN";
    private static final String envAdminIdName = "DISCORDLE_ADMIN_ID";

    private static final GameManager gameManager = new GameManager();

    public static void main(String[] args) {
        String token = System.getenv(envTokenName);
        if (Strings.isNullOrEmpty(token)) {
            throw new RuntimeException(envTokenName + " must be set in your environmental variables");
        }

        GatewayDiscordClient gateway = DiscordClient.create(token).login().block(Duration.ofSeconds(30));
        if (gateway == null) {
            throw new RuntimeException("Failed to instantiate client gateway");
        }

        registerCommands(gateway);

        var scheduler = new FixedTimeScheduler(gameManager::startNewGame);
        scheduler.addDailyExecution(LocalTime.NOON);
        scheduler.addDailyExecution(LocalTime.MIDNIGHT);

        logger.info("Discordle is ready");
        gateway.onDisconnect().block();
    }

    private static void registerCommands(GatewayDiscordClient gateway) {
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

        gateway.on(ChatInputInteractionEvent.class, Main::handleInteractionEvent).subscribe();
    }

    public static Mono<Void> handleInteractionEvent(ChatInputInteractionEvent event) {
        String command = event.getCommandName();
        logger.info("Command received: {}", command);
        return switch (command) {
            case "restart" -> {
                String adminId = System.getenv(envAdminIdName);
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
                    case ACCEPTED -> "Incorrect. Guesses:\n" + gameManager.getGuesses();
                    case GAME_WON -> "Winner winner chicken dinner!\n" + gameManager.getGuesses();
                    case GAME_LOST -> "Incorrect. Better luck next time.\n" + gameManager.getGuesses();
                    case INVALID_WORD -> "Your word is not in the dictionary.";
                    case GAME_UNAVAILABLE -> "There is currently no game going on. Games begin at 12AM/12PM PST.";
                    case ALREADY_SUBMITTED -> "You've already submitted a word for this game.";
                    case NOT_ENOUGH_LETTERS, TOO_MANY_LETTERS -> "Your submission must have 5 letters";
                };

                yield event.reply(response);
            }
            default -> throw new UnsupportedOperationException("Unknown command: " + command);
        };
    }
}
