package me.decce.gnetum;

import net.minecraft.util.ResourceLocation;

// Caches toString() to avoid excessive string concat and hashcode calculation
public class FastResourceLocation extends ResourceLocation {
	protected final String string;

	protected FastResourceLocation(int unused, String... resourceName) {
		super(unused, resourceName);
		this.string = this.namespace + ':' + this.path;
	}

	public FastResourceLocation(String resourceName) {
		super(resourceName);
		this.string = this.namespace + ':' + this.path;
	}

	public FastResourceLocation(String namespaceIn, String pathIn) {
		super(namespaceIn, pathIn);
		this.string = this.namespace + ':' + this.path;
	}

	@Override
	public String toString()
	{
		return this.string;
	}
}
