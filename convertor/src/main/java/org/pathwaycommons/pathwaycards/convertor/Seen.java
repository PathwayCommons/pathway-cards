package org.pathwaycommons.pathwaycards.convertor;

import java.util.HashSet;
import java.util.Set;

/**
 * A debug class for checking existing types. Not thread-safe at all.
 *
 * Created by babur on 1/21/2016.
 */
public class Seen
{
	private static Set<String> memory = new HashSet<>();

	public static void look(String item)
	{
		if (!memory.contains(item))
		{
			System.out.println("item = " + item);
			memory.add(item);
		}
	}

	public static void clear()
	{
		memory.clear();
	}
}
