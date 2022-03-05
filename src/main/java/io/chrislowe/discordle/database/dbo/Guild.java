package io.chrislowe.discordle.database.dbo;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "guilds")
public class Guild {

    @Id
    @Column(name = "guild_id", nullable = false)
    private String guildId;

    @Column(name = "has_current_game", nullable = false)
    private Boolean hasCurrentGame;

    @Column(name = "datetime_created", nullable = false)
    private Instant datetimeCreated;

    @OneToMany(mappedBy = "guild")
    private List<Game> games;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Guild guild = (Guild) o;
        return Objects.equals(guildId, guild.guildId);
    }

    @Override
    public int hashCode() {
        return guildId != null ? guildId.hashCode() : 0;
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public Boolean hasCurrentGame() {
        return hasCurrentGame;
    }

    public void setHasCurrentGame(Boolean hasActiveGame) {
        this.hasCurrentGame = hasActiveGame;
    }

    public Instant getDatetimeCreated() {
        return datetimeCreated;
    }

    public void setDatetimeCreated(Instant datetimeCreated) {
        this.datetimeCreated = datetimeCreated;
    }

    public List<Game> getGames() {
        return games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }
}
