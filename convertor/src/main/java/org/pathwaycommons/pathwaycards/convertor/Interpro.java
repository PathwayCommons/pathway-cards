package org.pathwaycommons.pathwaycards.convertor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by babur on 1/22/2016.
 */
public class Interpro
{
	private static Map<String, Set<String>> map = new HashMap<>();
	private static final String FILE = "interpro.txt";

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

		Set<String> set = spiderMembers(id);

		map.put(id, set);
		return set;
	}

	private static Set<String> spiderMembers(String id)
	{
		Set<String> set = new HashSet<>();
		try
		{
			Scanner sc = new Scanner(new URL("https://www.ebi.ac.uk/interpro/entry/" + id +
				"/proteins-matched?species=9606&export=fasta").openStream());
			while (sc.hasNextLine())
			{
				String line = sc.nextLine();
				if (line.startsWith(">")) set.add(line.substring(1));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return set;
	}

	private static void load() throws FileNotFoundException
	{
		if (!new File(FILE).exists()) return;
		Scanner sc = new Scanner(new File(FILE));
		while (sc.hasNextLine())
		{
			String[] token = sc.nextLine().split("\t");

			if (token.length > 0)
			{
				String id = token[0];
				Set<String> ups = token.length > 1 ?
					new HashSet<>(Arrays.asList(token).subList(1, token.length)) :
					Collections.<String>emptySet();
				map.put(id, ups);
			}
		}
	}

	public static void write() throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(FILE));
		for (String id : map.keySet())
		{
			writer.write("\n" + id);

			for (String up : map.get(id))
			{
				writer.write("\t" + up);
			}
		}
		writer.close();
	}
}
