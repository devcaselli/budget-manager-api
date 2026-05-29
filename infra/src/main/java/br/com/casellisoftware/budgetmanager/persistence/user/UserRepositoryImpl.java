package br.com.casellisoftware.budgetmanager.persistence.user;

import br.com.casellisoftware.budgetmanager.domain.user.User;
import br.com.casellisoftware.budgetmanager.domain.user.UserRepository;
import br.com.casellisoftware.budgetmanager.persistence.user.mappers.UserPersistenceMapper;

import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    private final UserMongoRepository userMongoRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryImpl(UserMongoRepository userMongoRepository, UserPersistenceMapper mapper) {
        this.userMongoRepository = userMongoRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserDocument saved = userMongoRepository.save(mapper.toDocument(user));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userMongoRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMongoRepository.existsByEmail(email);
    }
}
