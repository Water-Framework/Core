/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.core.api.notification.email;

import java.util.List;

/**
 * @Author Aristide Cittadino.
 * This interfaces is associated with a specific service with the responsability of sending internal email.
 * Mail notifications are important for registration, pwd recovery so the framework give the possibility to define
 * an internal service with this purpose.
 * A standard module providing this service is it.water.notification.email, please check on github.
 */
public interface EmailNotificationService {
    /**
     * @return Sender name shown inside email
     */
    String getSystemSenderName();

    /**
     * Sends the email.
     *
     * @param from
     * @param recipients
     * @param ccRecipients
     * @param bccRecipients
     * @param subject
     * @param body
     * @param attachments
     */
    void sendMail(String from, List<String> recipients, List<String> ccRecipients,
                  List<String> bccRecipients, String subject, String body, List<byte[]> attachments);

    /**
     * Sends a mail giving the basic template name that will be processed to the endsystem
     * @param templateName
     * @param from
     * @param recipients
     * @param ccRecipients
     * @param bccRecipients
     * @param subject
     * @param attachments
     */
    void sendMail(String templateName, String from, List<String> recipients, List<String> ccRecipients,
                  List<String> bccRecipients, String subject, List<byte[]> attachments);
}
