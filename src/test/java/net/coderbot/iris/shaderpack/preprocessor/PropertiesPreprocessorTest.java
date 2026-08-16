package net.coderbot.iris.shaderpack.preprocessor;

import net.coderbot.iris.shaderpack.StringPair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that lexer warnings during property macro preprocessing are not fatal.
 *
 * <p>jcpp throws a {@code LexerException} for warnings (e.g. "Decimal constant starts with 0, but
 * not octal") only while no listener is installed. Iris installs its listeners inside
 * {@code process()}, so macro values added before that point could fail the whole pack load.
 * Complementary Unbound r5.8.1 triggers exactly this warning, which broke shader toggling
 * (issue #31).</p>
 */
class PropertiesPreprocessorTest {
    @Test
    void zeroPrefixedDecimalInEnvironmentDefineValueIsNotFatal() {
        // The macro value is lexed while the preprocessor has no listener installed (issue #31).
        assertDoesNotThrow(() -> PropertiesPreprocessor.preprocessSource(
            "someKey = someValue\n",
            List.of(new StringPair("COMPLEMENTARY", "00002178"))
        ));
    }

    @Test
    void zeroPrefixedDecimalInSourceContentIsNotFatal() {
        // Lexing the source itself goes through the collecting listener; warnings are logged, not thrown.
        String result = PropertiesPreprocessor.preprocessSource(
            "profile.TEST = SHADOW_QUALITY=00002178\nnextKey = nextValue\n",
            List.of()
        );

        // The line survives preprocessing (the token stream is not interrupted by the warning).
        assertEquals(true, result.contains("SHADOW_QUALITY=00002178"));
        assertEquals(true, result.contains("nextKey = nextValue"));
    }
}