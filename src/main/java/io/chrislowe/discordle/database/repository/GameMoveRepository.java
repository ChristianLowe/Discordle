package io.chrislowe.discordle.database.repository;

import io.chrislowe.discordle.database.dbo.GameMove;
import io.chrislowe.discordle.database.dbo.Guild;
import io.chrislowe.discordle.database.dbo.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface GameMoveRepository extends CrudRepository<GameMove, Integer> {

  Optional<GameMove> findTopByGame_GuildAndUserOrderByDatetimeCreatedDesc(
      Guild guild, User user);

}
