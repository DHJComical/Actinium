package net.coderbot.iris.fantastic;

import net.coderbot.iris.layer.RenderLayer;
import java.util.function.Function;

public interface WrappingMultiBufferSource {
	void pushWrappingFunction(Function<RenderLayer, RenderLayer> wrappingFunction);
	void popWrappingFunction();
	void assertWrapStackEmpty();
}
