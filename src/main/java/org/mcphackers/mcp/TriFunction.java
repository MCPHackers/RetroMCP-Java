package org.mcphackers.mcp;

@FunctionalInterface
public interface TriFunction<E, T, U, R> {
	
    R apply(E e, T t, U u);
}
