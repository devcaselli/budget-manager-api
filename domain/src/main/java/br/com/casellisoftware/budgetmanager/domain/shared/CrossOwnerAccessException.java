package br.com.casellisoftware.budgetmanager.domain.shared;

import java.util.Objects;

public class CrossOwnerAccessException extends RuntimeException {

    private final String actorOwnerId;
    private final String targetOwnerId;
    private final String resourceType;
    private final String resourceId;

    public CrossOwnerAccessException(String actorOwnerId,
                                     String targetOwnerId,
                                     String resourceType,
                                     String resourceId) {
        super("Cross-owner access denied");
        this.actorOwnerId = requireNonBlank(actorOwnerId, "actorOwnerId");
        this.targetOwnerId = requireNonBlank(targetOwnerId, "targetOwnerId");
        this.resourceType = requireNonBlank(resourceType, "resourceType");
        this.resourceId = requireNonBlank(resourceId, "resourceId");
    }

    public String getActorOwnerId() {
        return actorOwnerId;
    }

    public String getTargetOwnerId() {
        return targetOwnerId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
