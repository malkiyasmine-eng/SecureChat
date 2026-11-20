package securechat.common;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Constants shared by the client and the server, so both always agree.
 *
 * Every frame is a sequence of writeUTF() strings. The FIRST string is the
 * frame type; the fields that follow depend on the type:
 *
 *   Client -> Server
 *     JOIN   : type, username
 *     MSG    : type, username, timestamp, encryptedContent
 *     LEAVE  : type
 *
 *   Server -> Client
 *     OK     : type                       (JOIN accepted)
 *     ERR    : type, reason               (JOIN refused, then server closes)
 *     MSG    : type, username, timestamp, encryptedContent  (broadcast)
 *     SYS    : type, text                 (plain server notice, e.g. "Ali joined")
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 5000;
    public static final int DEFAULT_SHIFT = 3;

    public static final String JOIN = "JOIN";
    public static final String OK = "OK";
    public static final String ERR = "ERR";
    public static final String MSG = "MSG";
    public static final String SYS = "SYS";
    public static final String LEAVE = "LEAVE";

    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MAX_MESSAGE_LENGTH = 500;          // plaintext typed by the user
    public static final int MAX_ENCRYPTED_LENGTH = 2000;       // what the server accepts
    public static final int MAX_TIMESTAMP_LENGTH = 30;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Protocol() { }

    /** Current local time as text, e.g. "14:05:09". */
    public static String now() {
        return LocalTime.now().format(TIME_FORMAT);
    }

    /** True if the username is acceptable (1-20 visible characters). */
    public static boolean isValidUsername(String name) {
        if (name == null) {
            return false;
        }
        String n = name.trim();
        if (n.isEmpty() || n.length() > MAX_USERNAME_LENGTH) {
            return false;
        }
        for (int i = 0; i < n.length(); i++) {
            if (Character.isISOControl(n.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
