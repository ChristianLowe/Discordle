package io.chrislowe.discordle.database.repository;

import io.chrislowe.discordle.database.dbo.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {

}
