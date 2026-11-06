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

import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Replaces the stock {@code freemarker} email template provider. Keycloak uses the factory with the
 * highest {@link #order()} when no provider is configured explicitly, so no server option is needed.
 */
public class SubjectArgsEmailTemplateProviderFactory extends FreeMarkerEmailTemplateProviderFactory {

    public static final String ID = "freemarker-subject-args";

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        return new SubjectArgsEmailTemplateProvider(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    /** Above the stock provider (0), so this one is the default. */
    @Override
    public int order() {
        return 10;
    }
}
