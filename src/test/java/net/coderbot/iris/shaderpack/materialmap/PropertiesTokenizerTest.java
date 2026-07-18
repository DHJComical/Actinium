package net.coderbot.iris.shaderpack.materialmap;

import net.coderbot.iris.shaderpack.materialmap.PropertiesTokenizer.ParsedBlockIdentifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesTokenizerTest {

	@Test
	void parsesEmptySegmentBetweenColonsWithoutCrashing() {
		// Regression: "minecraft::1" used to throw StringIndexOutOfBoundsException
		// because the second colon segment is empty.
		ParsedBlockIdentifier parsed = assertDoesNotThrow(() -> PropertiesTokenizer.parseBlockIdentifier("minecraft::1"));
		assertEquals("minecraft", parsed.id().getNamespace());
		assertEquals(Set.of(1), parsed.metas());
	}

	@Test
	void parsesNamespacedIdWithMeta() {
		ParsedBlockIdentifier parsed = PropertiesTokenizer.parseBlockIdentifier("minecraft:stone:1");
		assertEquals("minecraft", parsed.id().getNamespace());
		assertEquals("stone", parsed.id().getName());
		assertEquals(Set.of(1), parsed.metas());
		assertTrue(parsed.stateProperties().isEmpty());
	}

	@Test
	void parsesBareAndNamespacedIds() {
		ParsedBlockIdentifier bare = PropertiesTokenizer.parseBlockIdentifier("stone");
		assertEquals("minecraft", bare.id().getNamespace());
		assertEquals("stone", bare.id().getName());

		ParsedBlockIdentifier namespaced = PropertiesTokenizer.parseBlockIdentifier("minecraft:stone");
		assertEquals("minecraft", namespaced.id().getNamespace());
		assertEquals("stone", namespaced.id().getName());
	}

	@Test
	void parsesStateProperties() {
		ParsedBlockIdentifier parsed = PropertiesTokenizer.parseBlockIdentifier("minecraft:stone:variant=smooth");
		assertEquals("minecraft", parsed.id().getNamespace());
		assertEquals("stone", parsed.id().getName());
		assertEquals("smooth", parsed.stateProperties().get("variant"));
	}
}
