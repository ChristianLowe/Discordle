package io.chrislowe.discordle.database.dbo;

import io.chrislowe.discordle.database.enums.GameStatus;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id", nullable = false)
    private Integer gameId;

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER)
    @OrderBy
    private List<GameMove> gameMoves;

    @Column(name = "word", nullable = false)
    private String word;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @Column(name = "datetime_created", nullable = false)
    private Instant datetimeCreated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Game game = (Game) o;
        return Objects.equals(gameId, game.gameId);
    }

    @Override
    public int hashCode() {
        return gameId != null ? gameId.hashCode() : 0;
    }

    public Integer getGameId() {
        return gameId;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public void setGameMoves(List<GameMove> gameMoves) {
        this.gameMoves = gameMoves;
    }

    public List<GameMove> getGameMoves() {
        return gameMoves;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Instant getDatetimeCreated() {
        return datetimeCreated;
    }

    public void setDatetimeCreated(Instant datetimeCreated) {
        this.datetimeCreated = datetimeCreated;
    }
}
