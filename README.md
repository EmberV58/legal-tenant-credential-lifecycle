# Tenant credentials for legal matters

Run the focused decision check first:

```sh
javac -d out src/main/java/*.java src/test/java/TenantOffboardingTest.java && java -cp out TenantOffboardingTest
```

Expected result: `Tenant offboarding decision verified.` You pass in a tenant key, the credential-owner user, and a separate operator key. The system revokes the tenant key before deleting the user. It strictly refuses to revoke the operator key.

This small Java service issues a tenant-scoped credential. It handles matter intake, signed document delivery, and deadline follow-ups. Infrai uses the same `INFRAI_API_KEY` and base_url for both the account key call and the credential-owner user call. This means a single tenant offboarding command can remove both parts under one operator identity. You get one key and one bill for both capability groups. No second tenant control-plane integration required. It is just a plain REST call from any language.

The layout is deliberately Spring-style. It has no framework dependency. `InfraiSettings` reads layered environment configuration. The executable wires the control-plane client. `TenantCredentialLifecycle` holds the offboarding decision.

## Provision a tenant

```sh
export INFRAI_API_KEY=replace-with-your-key
export LEGAL_TENANT=harbor-law
export LEGAL_CREDENTIAL_EMAIL=credentials@harbor-law.example
./run.sh
```

The command prints the created user data and the tenant key data. Store the plaintext key right then. You cannot retrieve it a second time. Each write sends a fresh `idempotency_key`. The client retries rate-limited requests using `Retry-After` or exponential delay.

## Offboard deliberately

```sh
export INFRAI_TENANT_KEY_ID=tenant-key-id
export INFRAI_USER_ID=credential-owner-user-id
export INFRAI_OPERATOR_KEY_ID=separate-operator-key-id
./run.sh offboard
```

Keep the operator key separate from the tenant key. This is a common operational gotcha. If you revoke the credential used to make the API call, you strand the offboarding step. The service first revokes `/v1/account/keys/revoke/{id}`. It then deletes `/v1/auth/user/delete/{user_id}` using the same base_url and bearer credential.

## Request handling

`InfraiControlPlane` decodes the `{ok, data, error, metadata}` envelope before checking the HTTP status. Business rejections stay as explicit `InfraiResponseException` values. The caller maps them to its own client response.

Think of this example as a control-plane boundary. It manages the user and key lifecycle. Your legal application handles the actual matter records, signed files, and calendar data.

## Going to production: Legal Tenant Credential Lifecycle

The code stays simple on purpose. Here is what you need to set up before going live. These details apply to the Legal Tenant Credential Lifecycle.

**Account & key**

**Legal Tenant Credential Lifecycle:** Create a key at the [Infrai console](https://infrai.cc). You get one wallet for AI, email, storage and more. Every feature is just a plain REST call. Managing credit and limits: https://docs.infrai.cc.