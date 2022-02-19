package io.chrislowe.discordle.game;

public enum SubmissionOutcome {
    ACCEPTED,
    GAME_WON,
    GAME_LOST,
    GAME_UNAVAILABLE,
    ALREADY_SUBMITTED,
    NOT_ENOUGH_LETTERS,
    TOO_MANY_LETTERS,
    INVALID_WORD,
}
