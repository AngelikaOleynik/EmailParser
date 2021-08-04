import javax.mail.*;
import javax.mail.internet.MimeUtility;
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

            Message lastMessage = messagesList.get(messagesList.size());
            if (lastMessage.isMimeType("multipart/alternative")) {

                Multipart multipart = (Multipart) lastMessage.getContent();

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);

                    if (part.isMimeType("text/plain")) {
                        System.out.println(part.getContent().toString());
                    }
                    else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        String fileName = MimeUtility.decodeText(part.getFileName());
                    }
                }
            } else if (lastMessage.isMimeType("text/plain")) {
                System.out.println(messagesList.get(3).getContent().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
