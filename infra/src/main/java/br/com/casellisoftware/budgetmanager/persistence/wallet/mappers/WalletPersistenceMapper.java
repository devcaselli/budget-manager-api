package br.com.casellisoftware.budgetmanager.persistence.wallet.mappers;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper between {@link Wallet} and {@link WalletDocument}.
 *
 * <p>Both models use {@code Money} directly, so no custom conversion is needed —
 * MapStruct generates the entire mapping.</p>
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR)
public interface WalletPersistenceMapper {

    @Mapping(source = "closed", target = "isClosed")
    @Mapping(target = "version", ignore = true)
    WalletDocument toDocument(Wallet wallet);

    @BeanMapping(ignoreUnmappedSourceProperties = {"version"})
    @Mapping(source = "isClosed", target = "closed")
    @Mapping(target = "debit", ignore = true)
    Wallet toDomain(WalletDocument document);
}
