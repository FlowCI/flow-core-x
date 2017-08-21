/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.util;

import com.flow.platform.api.domain.EmailSettingContent;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @author yh@firim
 */
public class SmtpUtil {

    public static void sendEmail(EmailSettingContent emailSetting, String acceptor, String subject, String body) {

        Properties props = buildProperty(emailSetting);
        Session session = Session.getDefaultInstance(props,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailSetting.getUsername(), emailSetting.getPassword());
                }
            });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailSetting.getSender()));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(acceptor));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            System.out.println("Done");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * authentication
     */
    public static Boolean authentication(EmailSettingContent emailSetting) {

        Properties props = buildProperty(emailSetting);

        Session session = Session.getDefaultInstance(props, null);

        try {
            Transport transport = session.getTransport("smtp");
            transport.connect(emailSetting.getSmtpUrl(), emailSetting.getSmtpPort(), emailSetting.getUsername(),
                emailSetting.getPassword());
            transport.close();
            System.out.println("success");
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static Properties buildProperty(EmailSettingContent emailSetting) {
        Properties props = new Properties();
        props.put("mail.smtp.host", emailSetting.getSmtpUrl());
        props.put("mail.smtp.socketFactory.port", emailSetting.getSmtpPort().toString());
        props.put("mail.smtp.socketFactory.class",
            "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", emailSetting.getSmtpPort().toString());
        return props;
    }
}
