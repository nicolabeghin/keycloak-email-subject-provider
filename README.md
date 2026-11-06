[![Build, test and release](https://github.com/nicolabeghin/keycloak-email-subject-provider/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/nicolabeghin/keycloak-email-subject-provider/actions/workflows/build-and-release.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

# keycloak-email-subject-provider

A Keycloak email template provider that puts the **realm name in the subject of every email**, and
lets the subject of the *execute-actions* email say what is being asked:

```
Example Institute - Action required: Update Password
Example Institute - Reset your password
Example Institute - Your password was changed
```

instead of generic subjects that do not say who is writing, or the same "Update Your Account" for
every action.

## Why this exists

Keycloak formats the subject of the execute-actions email with an **empty argument list**, so a
subject message such as `executeActionsSubject=Action required: {0}` is sent with the placeholder unfilled.
The requested actions are only visible to the email *body* template. There is
no theme-only way around it.

This provider is the stock FreeMarker provider with two changes to subjects (bodies are never
touched):

1. **Realm prefix, on every email.** The subject becomes `<realm> - <subject>` (password reset, email
   verification, event emails, ...). Nothing is added when the realm has no name to show. It cannot be
   turned off: it is what the provider is for.
2. **Arguments for `executeActionsSubject`**, described below.

| Argument | Value |
|---|---|
| `{0}` | the requested required actions, translated with the theme's `requiredAction.<ID>` messages and joined with `, `. An action without a translation is shown by its id. |

## Install

1. Download `email-subject-provider.jar` from the [latest release](../../releases/latest) and copy it
   into `<keycloak>/providers/`.
2. For an optimized/production image run `kc.sh build`, then `kc.sh start --optimized`. In dev mode
   just restart.

No server option is needed: the provider registers with a higher `order()` than the stock one, so
Keycloak picks it by default. (`freemarker-subject-args` is its id, should you want to pin it
explicitly.)

## Use

Write the subject in your email theme's `messages_<locale>.properties`, using the argument:

```properties
executeActionsSubject=Action required: {0}
```

Or, without touching the theme, override it per realm with the admin API (realm localization),
which takes effect immediately:

```
PUT /admin/realms/{realm}/localization/{locale}/executeActionsSubject
```

If the message does not use `{0}` the subject is the stock one, with the realm prefix.

## Compatibility

The provider extends Keycloak's `FreeMarkerEmailTemplateProvider`, an internal class that Keycloak
does not promise to keep stable. It is built and tested against one Keycloak version at a time:

| Provider | Keycloak |
|---|---|
| 0.2.0 | 26.8.1 |

When you move to another Keycloak version, change `keycloak.version` in `pom.xml` and run the tests:
the integration test fails loudly if the provider is no longer registered or no longer takes effect.

## Tests

- **Unit tests** cover the argument logic and need nothing but a JDK.
- **An integration test** starts a real embedded Keycloak with the provider deployed, sends an
  execute-actions email through the admin API and reads it from an in-JVM SMTP server (GreenMail,
  from the Keycloak test framework). No Docker image, no external mail catcher.

```bash
make test    # whole build in a throwaway Maven container (isolated network), needs Docker
make build   # same on the host; the embedded Keycloak binds localhost:8080, which must be free
```

The Keycloak test framework hardcodes `localhost:8080` for the embedded server, which is why
`make test` runs in a container: nothing on your machine can collide with it.

## Releasing

Push a tag; the GitHub Action builds, runs the tests and attaches `email-subject-provider.jar` to
the release.

```bash
git tag 0.2.0 && git push origin 0.2.0
```

## License

Apache License 2.0, see [LICENSE](LICENSE).
