package io.chrislowe.discordle.database.repository;

import io.chrislowe.discordle.database.dbo.GameMove;
import org.springframework.data.repository.CrudRepository;

public interface GameMoveRepository extends CrudRepository<GameMove, Integer> {

}
