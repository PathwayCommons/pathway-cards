package org.pathwaycommons.pathwaycards.convertor;

/**
 * Created by babur on 1/22/2016.
 */
public class NextNumber
{
	static long number = System.currentTimeMillis();

	public static long get()
	{
		return ++number;
	}
}
