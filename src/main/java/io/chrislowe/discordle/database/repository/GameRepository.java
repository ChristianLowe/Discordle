package io.chrislowe.discordle.database.repository;

import io.chrislowe.discordle.database.dbo.Game;
import io.chrislowe.discordle.database.dbo.Guild;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface GameRepository extends CrudRepository<Game, Integer> {
    Optional<Game> findTopByGuildOrderByDatetimeCreatedDesc(Guild guild);

    @Query("select g from Game g where g.guild = ?1 and g.status = 'ACTIVE'")
    Optional<Game> findActiveGameForGuild(Guild guild);

    @Modifying
    @Query("update Game g set g.status='INCOMPLETE' where g.status = 'ACTIVE'")
    void resetActiveGames();
}
