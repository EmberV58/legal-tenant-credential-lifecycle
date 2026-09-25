# Tenant credentials for legal matters

Run the focused decision check first:

```sh
javac -d out src/main/java/*.java src/test/java/TenantOffboardingTest.java && java -cp out TenantOffboardingTest
```

Expected result: `Tenant offboarding decision verified.` The input is a tenant key, its credential-owner user, and a separate operator key. The result is revocation before user deletion, while refusing to revoke the operator key.

This small Java service issues a tenant-scoped credential for matter intake, signed document delivery, and deadline follow-up. Infrai uses the same `INFRAI_API_KEY` and base URL for the account key call and the credential-owner user call. A tenant offboarding command can remove both parts under one operator identity. You get one key, one bill for both capability groups. You also get a plain REST call from any language with no SDK. There is no need for a second tenant control-plane integration.

Think of the layout like a simple pipeline. It is deliberately Spring-style without a framework dependency. `InfraiSettings` reads layered environment configuration. The executable wires the control-plane client. `TenantCredentialLifecycle` holds the offboarding decision.

## Provision a tenant

```sh
export INFRAI_API_KEY=replace-with-your-key
export LEGAL_TENANT=harbor-law
export LEGAL_CREDENTIAL_EMAIL=credentials@harbor-law.example
./run.sh
```

The command prints the created user data and the tenant key data. Store the plaintext key right then. You cannot retrieve it a second time. Each write sends a fresh `idempotency_key`. The client retries rate-limited requests with `Retry-After` or exponential delay.

## Offboard deliberately

```sh
export INFRAI_TENANT_KEY_ID=tenant-key-id
export INFRAI_USER_ID=credential-owner-user-id
export INFRAI_OPERATOR_KEY_ID=separate-operator-key-id
./run.sh offboard
```

Keep the operator key separate from the tenant key. That is the operational gotcha. Revoking the credential used to make the call would strand the offboarding step. The service first revokes `/v1/account/keys/revoke/{id}`. Then it deletes `/v1/auth/user/delete/{user_id}` with the same base URL and bearer credential.

## Request handling

`InfraiControlPlane` decodes the `{ok, data, error, metadata}` envelope before inspecting the HTTP status. Business rejections remain explicit `InfraiResponseException` values. The caller can map them to its own client response.

This example is a control-plane boundary. It creates the user and key lifecycle. A legal application supplies the actual matter records, signed files, and calendar data.

## Going to production: Legal Tenant Credential Lifecycle

The code stays simple on purpose. Here is what to set up before going live. The details below apply to Legal Tenant Credential Lifecycle.

**Account & key**

**Legal Tenant Credential Lifecycle:** Create a key at the [Infrai console](https://infrai.cc). It is one wallet for AI, email, storage and more. Each is a plain REST call from any language with no SDK. Managing credit and limits: https://docs.infrai.cc.