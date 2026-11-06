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
    void realmNameFirstThenTranslatedAction() {
        List<Object> args = SubjectArguments.executeActions("Example Institute", List.of("UPDATE_PASSWORD"),
                SubjectArguments.labels(messages()));

        assertEquals(List.of("Example Institute", "Aggiornamento password"), args);
    }

    @Test
    void severalActionsAreJoinedInRequestOrder() {
        List<Object> args = SubjectArguments.executeActions("Example Institute", List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"),
                SubjectArguments.labels(messages()));

        assertEquals("Verifica dell'indirizzo email, Aggiornamento password", args.get(1));
    }

    @Test
    void actionWithoutTranslationFallsBackToItsId() {
        List<Object> args = SubjectArguments.executeActions("Example Institute", List.of("MY_CUSTOM_ACTION"),
                SubjectArguments.labels(messages()));

        assertEquals("MY_CUSTOM_ACTION", args.get(1));
    }

    @Test
    void argumentsFillTheSubjectMessage() {
        List<Object> args = SubjectArguments.executeActions("Istituto di Prova", List.of("UPDATE_PASSWORD"),
                SubjectArguments.labels(messages()));

        String subject = new MessageFormat("{0} - Azione richiesta: {1}", Locale.ITALIAN).format(args.toArray());

        assertEquals("Istituto di Prova - Azione richiesta: Aggiornamento password", subject);
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
}
