import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class TenantOffboardingTest {
    public static void main(String[] args) throws Exception {
        List<String> actions = new ArrayList<>();
        TenantCredentialLifecycle.offboard("tenant-key-7", "user-9", "operator-key-1", new TenantCredentialLifecycle.OffboardingGateway() {
            public void revokeKey(String id) { actions.add("revoke:" + id); }
            public void deleteUser(String id) { actions.add("delete:" + id); }
        });
        if (!actions.equals(List.of("revoke:tenant-key-7", "delete:user-9"))) {
            throw new AssertionError("Offboarding must revoke the tenant credential before deleting its owner: " + actions);
        }
        try {
            TenantCredentialLifecycle.offboard("operator-key-1", "user-9", "operator-key-1", new NoopGateway());
            throw new AssertionError("Operator key must not be selected for revocation");
        } catch (IllegalArgumentException expected) {
            System.out.println("Tenant offboarding decision verified.");
        }
    }

    static final class NoopGateway implements TenantCredentialLifecycle.OffboardingGateway {
        public void revokeKey(String id) throws IOException { }
        public void deleteUser(String id) throws IOException { }
    }
}
