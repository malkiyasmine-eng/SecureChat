package securechat.common;

public class CaesarCipherTest {

    private static int failures = 0;

    public static void main(String[] args) {
        check("abc xyz", 3, "def abc");
        check("ABC XYZ", 3, "DEF ABC");
        check("Hello, World! 123", 3, "Khoor, Zruog! 123");
        check("élève çà", 3, "éoèyh çà"); // accents are untouched, only plain letters move
        check("Hello", -3, "Ebiil"); // negative shift
        check("Hello", 29, "Khoor"); // 29 behaves like 3
        check("Hello", 0, "Hello");

        String text = "The Quick Brown Fox, 2026! Ça marche ?";
        for (int shift = -60; shift <= 60; shift++) {
            String back = CaesarCipher.decrypt(CaesarCipher.encrypt(text, shift), shift);
            if (!back.equals(text)) {
                failures++;
                System.out.println("FAIL round trip with shift " + shift);
            }
        }
        System.out.println(failures == 0 ? "All Caesar tests passed." : failures + " test(s) FAILED.");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String plain, int shift, String expected) {
        String actual = CaesarCipher.encrypt(plain, shift);
        if (!actual.equals(expected)) {
            failures++;
            System.out.println("FAIL encrypt(\"" + plain + "\", " + shift + ") = \"" + actual
                    + "\", expected \"" + expected + "\"");
        }
    }
}
