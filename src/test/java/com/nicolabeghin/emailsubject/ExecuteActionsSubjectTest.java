package com.nicolabeghin.emailsubject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * End to end against a real (embedded) Keycloak: the provider is picked up through
 * {@code META-INF/services}, the admin API sends the email, and an in-JVM SMTP server (GreenMail,
 * wired to the realm by the test framework) receives it. No Docker, no external mail catcher.
 */
@KeycloakIntegrationTest(config = ExecuteActionsSubjectTest.ProviderServer.class)
class ExecuteActionsSubjectTest {

    /** The test framework only deploys providers it is told about: this project, packaged as a jar. */
    static class ProviderServer implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependencyCurrentProject();
        }
    }

    private static final String SUBJECT_KEY = "executeActionsSubject";
    private static final String REALM_DISPLAY_NAME = "Istituto di Prova";

    @InjectRealm
    ManagedRealm realm;

    @InjectMailServer
    MailServer mailServer;

    @InjectAdminClient
    Keycloak adminClient;

    private String userId;

    @BeforeEach
    void createUser() {
        RealmRepresentation rep = realm.admin().toRepresentation();
        rep.setDisplayName(REALM_DISPLAY_NAME);
        realm.admin().update(rep);

        UserRepresentation user = new UserRepresentation();
        user.setUsername("subject-user");
        user.setEmail("subject-user@example.org");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEnabled(true);
        try (Response response = realm.admin().users().create(user)) {
            userId = CreatedResponseUtil.getCreatedId(response);
        }
    }

    @AfterEach
    void cleanUp() {
        realm.admin().users().get(userId).remove();
        try {
            realm.admin().localization().deleteRealmLocalizationTexts("en");
        } catch (NotFoundException noOverridesForThisTest) {
            // a test that never saved an override has nothing to delete
        }
    }

    @Test
    void providerIsRegisteredAndOutranksTheStockOne() {
        Map<String, ProviderRepresentation> providers = adminClient.serverInfo().getInfo()
                .getProviders().get("emailTemplate").getProviders();

        assertTrue(providers.containsKey(SubjectArgsEmailTemplateProviderFactory.ID),
                "factory not registered, email template providers: " + providers.keySet());
        assertTrue(providers.get(SubjectArgsEmailTemplateProviderFactory.ID).getOrder()
                        > providers.get("freemarker").getOrder(),
                "stock provider would win: " + providers.keySet());
    }

    @Test
    void subjectCarriesRealmNameAndTheRequestedAction() throws MessagingException {
        overrideSubject("{0} - Action required: {1}");

        realm.admin().users().get(userId).executeActionsEmail(List.of("UPDATE_PASSWORD"));

        assertEquals("Istituto di Prova - Action required: Update Password", receivedSubject());
    }

    @Test
    void severalActionsAreListedInRequestOrder() throws MessagingException {
        overrideSubject("{0} - Action required: {1}");

        realm.admin().users().get(userId).executeActionsEmail(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));

        assertEquals("Istituto di Prova - Action required: Verify Email, Update Password", receivedSubject());
    }

    @Test
    void stockSubjectIsUnchangedWhenTheThemeDoesNotUseTheArguments() throws MessagingException {
        realm.admin().users().get(userId).executeActionsEmail(List.of("UPDATE_PASSWORD"));

        assertEquals("Update Your Account", receivedSubject());
    }

    @Test
    void otherEmailsAreNotTouched() throws MessagingException {
        overrideSubject("{0} - Action required: {1}");

        realm.admin().users().get(userId).sendVerifyEmail();

        assertEquals("Verify email", receivedSubject());
    }

    private void overrideSubject(String text) {
        realm.admin().localization().saveRealmLocalizationText("en", SUBJECT_KEY, text);
    }

    private String receivedSubject() throws MessagingException {
        assertTrue(mailServer.waitForIncomingEmail(10_000, 1), "no email received within 10s");
        return mailServer.getLastReceivedMessage().getSubject();
    }
}
