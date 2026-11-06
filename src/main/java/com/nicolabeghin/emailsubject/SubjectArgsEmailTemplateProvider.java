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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;

/**
 * Stock FreeMarker email provider with two changes to email subjects:
 *
 * <ul>
 *   <li>the subject of the execute-actions email receives arguments (Keycloak itself formats it with
 *       an empty argument list, so the realm name and the requested actions are only reachable from
 *       the body);
 *   <li>every subject is prefixed with the realm name.
 * </ul>
 *
 * Bodies are never touched.
 */
public class SubjectArgsEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    public SubjectArgsEmailTemplateProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    protected EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template,
            Map<String, Object> attributes) throws EmailException {
        EmailTemplate email = SubjectArguments.EXECUTE_ACTIONS_SUBJECT.equals(subjectKey)
                ? processExecuteActions(subjectKey, template, attributes)
                : super.processTemplate(subjectKey, subjectAttributes, template, attributes);
        return new EmailTemplate(SubjectArguments.withRealmPrefix(getRealmName(), email.getSubject()),
                email.getTextBody(), email.getHtmlBody());
    }

    private EmailTemplate processExecuteActions(String subjectKey, String template, Map<String, Object> attributes)
            throws EmailException {
        try {
            // same locale and messages the parent will use to format the subject
            Locale locale = session.getContext().resolveLocale(user,
                    Boolean.parseBoolean(String.valueOf(attributes.get(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER))));
            Properties messages = getTheme().getEnhancedMessages(realm, locale);
            List<Object> arguments = SubjectArguments.executeActions(
                    SubjectArguments.actionIds(attributes.get(SubjectArguments.REQUIRED_ACTIONS_ATTRIBUTE)),
                    SubjectArguments.labels(messages));
            return super.processTemplate(subjectKey, arguments, template, attributes);
        } catch (IOException e) {
            throw new EmailException("Failed to resolve the subject arguments of " + subjectKey, e);
        }
    }
}
