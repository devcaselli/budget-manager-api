package br.com.casellisoftware.budgetmanager.persistence.wallet;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.persistence.wallet.mappers.WalletPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private final WalletMongoRepository walletMongoRepository;
    private final WalletPersistenceMapper mapper;

    @Override
    public Optional<Wallet> findById(String id) {
        return this.walletMongoRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        Long version = walletMongoRepository.findById(wallet.getId())
                .map(WalletDocument::getVersion)
                .orElse(null);
        WalletDocument document = mapper.toDocument(wallet, version);
        document = this.walletMongoRepository.save(document);
        return  mapper.toDomain(document);
    }
}
