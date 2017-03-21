package org.pathwaycommons.pathwaycards.convertor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ozgun Babur
 */
public class Bioentities
{
	/**
	 * Family member map.
	 */
	static Map<String, Set<Entry>> famMems;

	/**
	 * Complex member map.
	 */
	static Map<String, Set<Entry>> comMems;

	public static Set<Entry> getComplexMembers(String id)
	{
		return comMems.get(id);
	}

	public static Set<Entry> getFamilyMembers(String id)
	{
		return famMems.get(id);
	}

	public static boolean isComplex(String id)
	{
		return comMems.containsKey(id);
	}

	public static boolean isFamily(String id)
	{
		return famMems.containsKey(id);
	}

	static
	{
		famMems = new HashMap<>();
		comMems = new HashMap<>();

		try
		{

			Files.lines(Paths.get(Bioentities.class.getResource("BE_relations.csv").toURI())).map(l -> l.split(","))
				.forEach(t ->
			{
				Relation rel = Relation.valueOf(t[2]);
				Entry e1 = new Entry(t[0], t[1]);
				String e2 = t[4];

				Map<String, Set<Entry>> map = rel == Relation.isa ? famMems : comMems;

				if (!map.containsKey(e2)) map.put(e2, new HashSet<>());
				map.get(e2).add(e1);
			});
		}
		catch (IOException | URISyntaxException e)
		{
			e.printStackTrace();
		}
	}

	public static class Entry
	{
		DB db;
		String id;

		public Entry(String db, String id)
		{
			this.db = DB.get(db);
			this.id = id;
		}

		@Override
		public String toString()
		{
			return db + ":" + id;
		}

		@Override
		public int hashCode()
		{
			return db.hashCode() + id.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof Entry && db.equals(((Entry) obj).db) && id.equals(((Entry) obj).id);
		}
	}

	public enum DB
	{
		Bioentities("BE"),
		UniProt("UP"),
		HGNC("HGNC");

		String text;

		DB(String text)
		{
			this.text = text;
		}

		static DB get(String text)
		{
			for (DB db : values())
			{
				if (db.text.equals(text)) return db;
			}
			return null;
		}
	}

	private static enum Relation
	{
		isa,
		partof
	}

	public static void main(String[] args)
	{
		boolean present = famMems.values().stream().filter(comMems.keySet()::contains).findAny().isPresent();
		System.out.println("present = " + present);
	}
}
