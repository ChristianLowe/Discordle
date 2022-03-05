package io.chrislowe.discordle.database.repository;

import io.chrislowe.discordle.database.dbo.Guild;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface GuildRepository extends CrudRepository<Guild, String> {

    @Modifying
    @Query("update Guild g set g.hasCurrentGame=false")
    void resetActiveGuilds();
}
