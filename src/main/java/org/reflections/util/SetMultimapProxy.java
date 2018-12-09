package org.reflections.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;

public class SetMultimapProxy<K, V> 
		extends AbstractMultimap<K, V> 
		implements SetMultimap<K, V> {

	public SetMultimapProxy(
			HashMap<K, ? extends Collection<V>> map, 
			Supplier<? extends Collection<V>> factory) {
		super(map, factory);
	}

}
