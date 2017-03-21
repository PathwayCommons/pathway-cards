package org.pathwaycommons.pathwaycards.convertor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ozgun Babur
 */
public enum FrexTag
{
	ENTITY_TYPE("entity_type"),
	ENTITY_TEXT("entity_text"),
	IDENTIFIER("identifier"),
	PARTICIPANT_A("participant_a"),
	PARTICIPANT_B("participant_b"),
	PUBLICATION_REF("docId"),
	EVENTS("events"),
	SENTENCE("sentence"),
	PREDICATE("predicate"),
	SIGN("sign"),
	NEGATIVE("negative"),
	PREDICATE_ID("id"),
	PREDICATE_TYPE("type"),
	PREDICATE_SUB_TYPE("sub_type"),
	FROM_LOCATION("from_location"),
	TO_LOCATION("to_location"),
	REGULATION("regulation"),
	PROTEIN_MODIFICATION("protein-modification"),
	TRANSLOCATION("translocation"),
	BINDS("binds"),
	ACTIVATION("activation"),
	MODIFICATIONS("modifications"),
	MODIFICATION_TYPE("modification_type"),
	SITES("sites"),
	SITE("site"),
	AA_CODE("amino_acid"),
	POSITION("position"),
	PROTEIN("protein"),
	CHEMICAL("simple-chemical"),
	FAMILY("family"),
	BIOPROCESS("bioprocess"),
	;

	String[] text;

	FrexTag(String... text)
	{
		this.text = text;
	}

	public String getTag()
	{
		return text[0];
	}

	public boolean equal(String val)
	{
		for (String s : text)
		{
			if (s.equals(val)) return true;
		}
		return false;
	}

	public boolean equal(Map map, FrexTag tag)
	{
		String value = getString(map);
		if (value != null)
		{
			return tag.equal(value);
		}
		return false;
	}

	public Object get(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s)) return map.get(s);
		}
		return null;
	}

	public Object remove(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s))
			{
				return map.remove(s);
			}
		}
		return null;
	}

	public String getString(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s)) return (String) map.get(s);
		}
		return null;
	}

	public List<String> getStrings(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s))
			{
				List<String> list = new ArrayList<>();
				for (Object o : (List) map.get(s))
				{
					list.add(o.toString());
				}
				return list;
			}
		}
		return null;
	}

	public Map getMap(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s)) return (Map) map.get(s);
		}
		return null;
	}

	public List getList(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s))
			{
				if (!(map.get(s) instanceof List)) return Collections.singletonList(map.get(s));
				else return (List) map.get(s);
			}
		}
		return null;
	}

	public Map getMapOrFirstInList(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s))
			{
				Object o = map.get(s);
				if (o instanceof Map) return (Map) o;
				else if (o instanceof List)
				{
					List list = (List) o;
					if (!list.isEmpty()) return (Map) list.iterator().next();
				}
			}
		}
		return null;
	}

	public Integer getInt(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s)) return Integer.valueOf(map.get(s).toString());
		}
		return null;
	}

	public boolean has(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s)) return true;
		}
		return false;
	}

	public boolean isList(Map map)
	{
		for (String s : text)
		{
			if (map.containsKey(s))
			{
				Object o = map.get(s);
				return o instanceof List;
			}
		}
		return false;
	}
}
