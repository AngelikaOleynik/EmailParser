import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


public class CheckEmail {
    public static final String MAIL_DEBUG = "mail.debug";
    public static final String FALSE = "false";
    public static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    public static final String IMAPS = "imaps";
    public static final String MAIL_IMAPS_SSL_ENABLE = "mail.imaps.ssl.enable";
    public static final String TRUE = "true";
    public static final String MAIL_IMAPS_PORT = "mail.imaps.port";
    public static final String PORT_VALUE = "993";
    private static final String IMAP_YANDEX_COM = "imap.yandex.com";

    private static final String EMAIL_ADDRESS = "sperasoft.qa@yandex.ru";
    private static final String PASSWORD = "qa.sperasoft";

    public static void checkEmail() {

        Properties properties = System.getProperties();
        properties.put(MAIL_DEBUG, FALSE);
        properties.put(MAIL_STORE_PROTOCOL, IMAPS);
        properties.put(MAIL_IMAPS_SSL_ENABLE, TRUE);
        properties.put(MAIL_IMAPS_PORT, PORT_VALUE);

        Authenticator auth = new EmailAuthenticator(EMAIL_ADDRESS, PASSWORD);
        Session session = Session.getDefaultInstance(properties, auth);
        session.setDebug(false);

        try {
            Store store = session.getStore();
            store.connect(IMAP_YANDEX_COM, EMAIL_ADDRESS, PASSWORD);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            List<Message> messagesList = new LinkedList<>();
            for (int i = 1; i < inbox.getMessageCount(); i++) {
                messagesList.add(inbox.getMessage(i));
            }

            if (messagesList.get(3).isMimeType("multipart/alternative")) {

                Multipart multipart = (Multipart) messagesList.get(3).getContent();

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    //Для html-сообщений создается две части, "text/plain" и "text/html" (для клиентов без возможности чтения html сообщений), так что если нам не важна разметка:
                    if (part.isMimeType("text/plain")) {
                        System.out.println(part.getContent().toString());
                    }
                    // Проверяем является ли part вложением
                    else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        // Опускаю проверку на совпадение имен. Имя может быть закодировано, используем decode
                        String fileName = MimeUtility.decodeText(part.getFileName());

                        //InputStream is = part.getInputStream();
                        // Далее можем записать файл, или что-угодно от нас требуется
                    }
                }
            } else if (messagesList.get(3).isMimeType("text/plain")) {
                System.out.println(messagesList.get(3).getContent().toString());
            }

            System.out.println(messagesList.get(3).getSubject() + "  === " + messagesList.get(3).getContent());

            Message inboxMessages[] = inbox.getMessages();

            Message firstMessage = (Message) Arrays.stream(Arrays.stream(inboxMessages).toArray()).findFirst().get();

            String messageSubject = firstMessage.getSubject().replaceAll("<(.)+?>", "").trim();
            //оставляет русский текст и символы
            String messageContent = firstMessage.getContent().toString().replaceAll("[[\\S]&&[^А-Яа-я-.?!)(,:]]", "").trim();

            System.out.println("Message subject " + messageSubject);
            System.out.println("======================================================================================");
            System.out.println("Message subject " + messageContent);
            System.out.println("======================================================================================");

            System.out.println("========================================================================================");
            String content = inboxMessages[0].getAllRecipients().toString();
            if (content.length() > 500) {
                content = content.substring(0, 500);
            }
            System.out.print(content);

            inbox.close(false);
            store.close();

        } catch (NoSuchProviderException noSuchProviderException) {
            noSuchProviderException.printStackTrace();
        } catch (MessagingException messagingException) {
            messagingException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break;
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }
}
