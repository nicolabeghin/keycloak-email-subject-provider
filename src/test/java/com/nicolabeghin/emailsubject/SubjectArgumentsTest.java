package com.nicolabeghin.emailsubject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class SubjectArgumentsTest {

    private static Properties messages() {
        Properties p = new Properties();
        p.setProperty("requiredAction.UPDATE_PASSWORD", "Aggiornamento password");
        p.setProperty("requiredAction.VERIFY_EMAIL", "Verifica dell'indirizzo email");
        return p;
    }

    @Test
    void translatedAction() {
        List<Object> args = SubjectArguments.executeActions(List.of("UPDATE_PASSWORD"),
                SubjectArguments.labels(messages()));

        assertEquals(List.of("Aggiornamento password"), args);
    }

    @Test
    void severalActionsAreJoinedInRequestOrder() {
        List<Object> args = SubjectArguments.executeActions(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"),
                SubjectArguments.labels(messages()));

        assertEquals("Verifica dell'indirizzo email, Aggiornamento password", args.get(0));
    }

    @Test
    void actionWithoutTranslationFallsBackToItsId() {
        List<Object> args = SubjectArguments.executeActions(List.of("MY_CUSTOM_ACTION"),
                SubjectArguments.labels(messages()));

        assertEquals("MY_CUSTOM_ACTION", args.get(0));
    }

    @Test
    void argumentsFillTheSubjectMessage() {
        List<Object> args = SubjectArguments.executeActions(List.of("UPDATE_PASSWORD"),
                SubjectArguments.labels(messages()));

        String subject = new MessageFormat("Azione richiesta: {0}", Locale.ITALIAN).format(args.toArray());

        assertEquals("Azione richiesta: Aggiornamento password", subject);
    }

    @Test
    void missingActionsAttributeMeansNoActions() {
        assertEquals(List.of(), SubjectArguments.actionIds(null));
    }

    @Test
    void actionIdsAcceptsAnyCollection() {
        assertEquals(List.of("A", "B"), SubjectArguments.actionIds(new java.util.LinkedHashSet<>(List.of("A", "B"))));
    }

    @Test
    void aNonListAttributeIsReportedNotHidden() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SubjectArguments.actionIds("UPDATE_PASSWORD"));

        assertEquals("Expected 'requiredActions' to be a list of action ids, got java.lang.String", e.getMessage());
    }

    @Test
    void realmNameIsPrependedToASubject() {
        assertEquals("Example Institute - Reset your password",
                SubjectArguments.withRealmPrefix("Example Institute", "Reset your password"));
    }

    @Test
    void noPrefixWithoutARealmName() {
        assertEquals("Reset your password", SubjectArguments.withRealmPrefix("", "Reset your password"));
        assertEquals("Reset your password", SubjectArguments.withRealmPrefix(null, "Reset your password"));
    }
}
