package io.chrislowe.discordle.database.dto;

import org.springframework.data.util.Pair;

import java.util.Objects;
import javax.annotation.Nullable;

public class UserStats {
    private final String discordId;
    private int yellowsGuessed;
    private int greensGuessed;
    private int gamesLost;
    private int gamesWon;

    private int golfScore;
    
    private @Nullable Pair<String, Integer>topWord;

    public UserStats(String discordId) {
        this.discordId = discordId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStats userStats = (UserStats) o;
        return discordId.equals(userStats.discordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discordId);
    }

    public String getDiscordId() {
        return discordId;
    }

    public int getYellowsGuessed() {
        return yellowsGuessed;
    }

    public void setYellowsGuessed(int yellowsGuessed) {
        this.yellowsGuessed = yellowsGuessed;
    }

    public int getGreensGuessed() {
        return greensGuessed;
    }

    public void setGreensGuessed(int greensGuessed) {
        this.greensGuessed = greensGuessed;
    }

    public int getGolfScore() {
        return golfScore;
    }

    public void setGolfScore(int golfScore) {
        this.golfScore = golfScore;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }


    public @Nullable Pair<String, Integer> getTopWord() {
        return topWord;
    }

    public void setTopWord(@Nullable Pair<String, Integer> topWord) {
        this.topWord = topWord;
    }
}
