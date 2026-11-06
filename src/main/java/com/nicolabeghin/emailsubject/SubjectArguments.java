/*
 * Copyright 2026 Nicola Beghin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nicolabeghin.emailsubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the {@link java.text.MessageFormat} arguments of an email subject. Kept free of Keycloak
 * types so it can be unit tested without a server.
 */
public final class SubjectArguments {

    /** Subject message key of the execute-actions email. */
    public static final String EXECUTE_ACTIONS_SUBJECT = "executeActionsSubject";

    /** Template attribute holding the requested required-action ids (see {@code executeActions.ftl}). */
    public static final String REQUIRED_ACTIONS_ATTRIBUTE = "requiredActions";

    private static final String ACTION_LABEL_PREFIX = "requiredAction.";

    private SubjectArguments() {
    }

    /**
     * Arguments of {@code executeActionsSubject}: {@code {0}} is the realm name (the display name
     * when set, the same value the email template shows as {@code realmName}) and {@code {1}} the
     * requested actions, translated and joined with {@code ", "}.
     */
    public static List<Object> executeActions(String realmName, List<String> actionIds, Function<String, String> labelOf) {
        String actions = actionIds.stream().map(labelOf).collect(Collectors.joining(", "));
        return List.of(realmName, actions);
    }

    /**
     * Resolves an action id to its translation ({@code requiredAction.<ID>} in the email theme
     * messages). An id without a translation is shown as is, so a custom required action still
     * produces a meaningful subject.
     */
    public static Function<String, String> labels(Properties messages) {
        return id -> messages.getProperty(ACTION_LABEL_PREFIX + id, id);
    }

    /**
     * The {@code requiredActions} template attribute as a list of ids. Absent means no actions,
     * which is how {@code executeActions.ftl} treats it too; any other type is a programming error
     * on the caller's side and is reported instead of hidden.
     */
    public static List<String> actionIds(Object attribute) {
        if (attribute == null) {
            return List.of();
        }
        if (!(attribute instanceof Iterable<?> items)) {
            throw new IllegalArgumentException("Expected '" + REQUIRED_ACTIONS_ATTRIBUTE
                    + "' to be a list of action ids, got " + attribute.getClass().getName());
        }
        List<String> ids = new ArrayList<>();
        for (Object item : items) {
            ids.add(String.valueOf(item));
        }
        return ids;
    }
}
