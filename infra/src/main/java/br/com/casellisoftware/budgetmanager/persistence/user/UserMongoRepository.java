package br.com.casellisoftware.budgetmanager.persistence.user;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserMongoRepository extends CrudRepository<UserDocument, String> {

    Optional<UserDocument> findByEmail(String email);

    boolean existsByEmail(String email);
}
