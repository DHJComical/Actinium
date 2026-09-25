package com.dhj.actinium.gui;

import net.coderbot.iris.shaderpack.LanguageMap;

import java.util.Locale;
import java.util.Map;

/**
 * Lookup of shader pack language entries, delegated to by {@code LocaleIrisMixin}.
 * <p>
 * Motivation: keys provided by a shader pack's own lang files (en_us.lang, zh_CN.lang, ...)
 * such as option.*, value.*, screen.* and profile.* are absent from the vanilla language
 * table, so {@link LanguageMap} must be wired into the vanilla translation lookup chain for
 * the shader pack GUI to follow the game language. The logic lives outside the mixin package
 * to honor the "mixins only inject" convention and to keep the lookup directly unit-testable.
 */
public final class ShaderPackTranslationLookup {

	private ShaderPackTranslationLookup() {}

	/**
	 * Looks up a translation in the shader pack language table.
	 *
	 * Motivation: vanilla {@code Locale.translateKeyPrivate/hasKey} needs a side lookup that
	 * returns null on a miss, so shader pack translations can be layered on top without
	 * polluting the vanilla translation table.
	 *
	 * @param languageMap language table of the currently loaded shader pack; null (pack not
	 *                    loaded or disabled) provides no side lookup
	 * @param vanillaTranslations translations already present in the vanilla Locale; keys
	 *                            resolved by vanilla must be returned untouched and must not
	 *                            be overridden by a same-named shader pack entry
	 * @param key language key to look up
	 * @param preferredLanguageCodes language lookup order, tried in sequence, first hit wins
	 * @return the translation, or null when the pack does not provide the key (the caller
	 *         then falls back to vanilla behavior)
	 */
	public static String lookup(LanguageMap languageMap, Map<String, String> vanillaTranslations, String key, Iterable<String> preferredLanguageCodes) {
		if (languageMap == null || vanillaTranslations.containsKey(key)) {
			return null;
		}

		for (String code : preferredLanguageCodes) {
			Map<String, String> translations = languageMap.getTranslations(normalizeLanguageCode(code));
			if (translations == null) {
				continue;
			}

			String translation = translations.get(key);
			if (translation != null) {
				return translation;
			}
		}

		return null;
	}

	/**
	 * Normalizes a language code to match {@link LanguageMap} keys.
	 *
	 * Motivation: LanguageMap keys are lang file names lower-cased as a whole
	 * (en_US.lang -> "en_us"), while language codes from settings or caller constants may
	 * carry case variants; lower-casing them makes the lookup hit.
	 */
	static String normalizeLanguageCode(String code) {
		return code.toLowerCase(Locale.ROOT);
	}
}
