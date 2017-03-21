package org.pathwaycommons.pathwaycards.convertor;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Created by babur on 1/22/2016.
 */
public class Pfam
{
	private static Map<String, Set<String>> map = new HashMap<>();
	private static final String FILE = "pfam.txt";

	static
	{
		try
		{
			load();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public static Set<String> getMembers(String id)
	{
		if (map.containsKey(id)) return map.get(id);
		return Collections.emptySet();
	}

	private static void load() throws FileNotFoundException
	{
		Scanner sc = new Scanner(Pfam.class.getResourceAsStream(FILE));

		// Skip header
		sc.nextLine();

		while (sc.hasNextLine())
		{
			String[] token = sc.nextLine().split("\t");

			if (token.length > 0)
			{
				String up = token[0];

				for (String pfam : token[1].split(";"))
				{
					if (!pfam.isEmpty())
					{
						if (!map.containsKey(pfam)) map.put(pfam, new HashSet<>());
						map.get(pfam).add(up);
					}
				}
			}
		}
	}
}
