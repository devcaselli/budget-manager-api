package br.com.casellisoftware.budgetmanager.persistence.user.mappers;

import br.com.casellisoftware.budgetmanager.domain.user.User;
import br.com.casellisoftware.budgetmanager.persistence.user.UserDocument;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserPersistenceMapper {

    default UserDocument toDocument(User user) {
        if (user == null) {
            return null;
        }
        UserDocument doc = new UserDocument();
        doc.setId(user.getId());
        doc.setEmail(user.getEmail());
        doc.setPasswordHash(user.getPasswordHash());
        doc.setCreatedAt(user.getCreatedAt());
        return doc;
    }

    default User toDomain(UserDocument document) {
        if (document == null) {
            return null;
        }
        return new User(
                document.getId(),
                document.getEmail(),
                document.getPasswordHash(),
                document.getCreatedAt()
        );
    }
}
