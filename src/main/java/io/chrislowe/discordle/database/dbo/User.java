package io.chrislowe.discordle.database.dbo;

import com.google.common.base.Objects;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "discord_id", nullable = false)
    private String discordId;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;

    @Column(name = "datetime_created", nullable = false)
    private Instant datetimeCreated;

    @OneToMany(mappedBy = "user")
    @OrderBy
    private List<GameMove> gameMoves;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equal(discordId, user.discordId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(discordId);
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public Boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Instant getDatetimeCreated() {
        return datetimeCreated;
    }

    public void setDatetimeCreated(Instant datetimeCreated) {
        this.datetimeCreated = datetimeCreated;
    }

    public List<GameMove> getGameMoves() {
        return gameMoves;
    }

    public void setGameMoves(List<GameMove> gameMoves) {
        this.gameMoves = gameMoves;
    }
}
