import java.io.IOException;
import java.util.Objects;

final class TenantCredentialLifecycle {
    interface OffboardingGateway {
        void revokeKey(String keyId) throws IOException, InterruptedException;
        void deleteUser(String userId) throws IOException, InterruptedException;
    }

    static void offboard(String tenantKeyId, String userId, String operatorKeyId, OffboardingGateway gateway)
            throws IOException, InterruptedException {
        if (tenantKeyId.isBlank() || userId.isBlank()) {
            throw new IllegalArgumentException("Tenant key id and user id are required");
        }
        if (Objects.equals(tenantKeyId, operatorKeyId)) {
            throw new IllegalArgumentException("Use an operator key distinct from the tenant key being revoked");
        }
        gateway.revokeKey(tenantKeyId);
        gateway.deleteUser(userId);
    }
}
