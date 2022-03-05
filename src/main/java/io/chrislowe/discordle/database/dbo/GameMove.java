package io.chrislowe.discordle.database.dbo;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "game_moves")
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_move_id", nullable = false)
    private Integer gameMoveId;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne
    @JoinColumn(name = "discord_id", nullable = false)
    private User user;

    @Column(name = "word", nullable = false)
    private String word;

    @Column(name = "new_yellows_guessed", nullable = false)
    private Byte newYellowsGuessed;

    @Column(name = "new_greens_guessed", nullable = false)
    private Byte newGreensGuessed;

    @Column(name = "datetime_created", nullable = false)
    private Instant datetimeCreated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GameMove gameMove = (GameMove) o;
        return Objects.equals(gameMoveId, gameMove.gameMoveId);
    }

    @Override
    public int hashCode() {
        return gameMoveId != null ? gameMoveId.hashCode() : 0;
    }

    public Integer getGameMoveId() {
        return gameMoveId;
    }

    public void setGameMoveId(Integer gameMoveId) {
        this.gameMoveId = gameMoveId;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Byte getNewYellowsGuessed() {
        return newYellowsGuessed;
    }

    public void setNewYellowsGuessed(Byte newYellowsGuessed) {
        this.newYellowsGuessed = newYellowsGuessed;
    }

    public Byte getNewGreensGuessed() {
        return newGreensGuessed;
    }

    public void setNewGreensGuessed(Byte newGreensGuessed) {
        this.newGreensGuessed = newGreensGuessed;
    }

    public Instant getDatetimeCreated() {
        return datetimeCreated;
    }

    public void setDatetimeCreated(Instant datetimeCreated) {
        this.datetimeCreated = datetimeCreated;
    }
}
