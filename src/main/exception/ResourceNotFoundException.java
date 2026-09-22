package main.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final String resourceId;

    /**
     * Constructs a ResourceNotFoundException with resource type and ID
     * 
     * @param resourceType Type of resource (e.g., "Account", "Instrument")
     * @param resourceId   ID of the missing resource
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with ID %s not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
