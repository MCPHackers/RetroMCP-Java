package org.mcphackers.mcp.tools;

@FunctionalInterface
public interface TriFunction<E, T, U, R> {
	
	R apply(E e, T t, U u);
}
