import java.io.IOException;
import java.util.UUID;

public final class LegalTenantProvisioning {
    public static void main(String[] args) throws Exception {
        InfraiSettings settings = InfraiSettings.fromEnvironment();
        InfraiControlPlane infrai = new InfraiControlPlane(settings.baseUrl(), settings.apiKey());
        if (args.length == 1 && args[0].equals("offboard")) {
            offboard(infrai);
            return;
        }
        provision(infrai);
    }

    private static void provision(InfraiControlPlane infrai) throws IOException, InterruptedException {
        String tenant = InfraiSettings.require("LEGAL_TENANT");
        String email = InfraiSettings.require("LEGAL_CREDENTIAL_EMAIL");
        String user = "{\"email\":\"" + escape(email) + "\",\"name\":\"" + escape(tenant)
                + " credential owner\",\"idempotency_key\":\"" + UUID.randomUUID() + "\"}";
        String userData = infrai.post("/v1/auth/user/create", user);
        String key = "{\"name\":\"" + escape(tenant)
                + " matter intake, signed delivery, deadline follow-up\",\"scopes\":[\"matter:intake\",\"document:signed-delivery\",\"deadline:follow-up\"],\"idempotency_key\":\""
                + UUID.randomUUID() + "\"}";
        String keyData = infrai.post("/v1/account/keys/create", key);
        System.out.println("Created credential owner: " + userData);
        System.out.println("Created tenant key (record the plaintext now; it is not returned again): " + keyData);
    }

    private static void offboard(InfraiControlPlane infrai) throws IOException, InterruptedException {
        TenantCredentialLifecycle.offboard(InfraiSettings.require("INFRAI_TENANT_KEY_ID"), InfraiSettings.require("INFRAI_USER_ID"),
                InfraiSettings.require("INFRAI_OPERATOR_KEY_ID"), new TenantCredentialLifecycle.OffboardingGateway() {
                    public void revokeKey(String keyId) throws IOException, InterruptedException {
                        infrai.delete("/v1/account/keys/revoke/" + keyId);
                    }
                    public void deleteUser(String userId) throws IOException, InterruptedException {
                        infrai.delete("/v1/auth/user/delete/" + userId);
                    }
                });
        System.out.println("Tenant credential and owner were offboarded.");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
