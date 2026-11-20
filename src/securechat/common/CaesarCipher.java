package securechat.common;

/**
 * Classical Caesar cipher (EDUCATIONAL ONLY - not secure for real secrets).
 *
 * Only the English letters A-Z and a-z are shifted. Everything else
 * (spaces, digits, punctuation, accented letters such as é or ç, emoji...)
 * is copied unchanged.
 */
public final class CaesarCipher {

    private static final int ALPHABET_SIZE = 26;

    private CaesarCipher() { } // utility class: nobody should create an instance

    /** Encrypts by moving every letter 'shift' places forward. */
    public static String encrypt(String message, int shift) {
        return transform(message, shift);
    }

    /** Decrypts by moving every letter 'shift' places backward. */
    public static String decrypt(String message, int shift) {
        return transform(message, -(long) shift);
    }

    private static String transform(String message, long shift) {
        if (message == null) {
            return "";
        }
        // floorMod gives a result between 0 and 25 even for negative shifts.
        int s = (int) Math.floorMod(shift, (long) ALPHABET_SIZE);
        StringBuilder result = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                result.append((char) ('A' + (c - 'A' + s) % ALPHABET_SIZE));
            } else if (c >= 'a' && c <= 'z') {
                result.append((char) ('a' + (c - 'a' + s) % ALPHABET_SIZE));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
