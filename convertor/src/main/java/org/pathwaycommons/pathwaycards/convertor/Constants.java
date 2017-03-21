package org.pathwaycommons.pathwaycards.convertor;

import java.util.*;

/**
 * Created by babur on 1/20/2016.
 */
public class Constants
{
	public static final String[] EXTRACTED_INFORMATION = new String[]{"extracted_information"};
	public static final String[] PREDICATE = new String[]{"predicate"};
	public static final String[] PREDICATE_ID = new String[]{"id"};
	public static final String[] INTERACTION_TYPE = new String[]{"interaction_type", "type"};
	public static final String[] INTERACTION_SUB_TYPE = new String[]{"sub_type"};

	public static final String[] REGULATION = new String[]{"regulation"};
	public static final String[] POSITIVE_REGULATION = new String[]{"positive_regulation"};
	public static final String[] NEGATIVE_REGULATION = new String[]{"negative_regulation"};


	public static final String[] ADDS_MODIFICATION = new String[]{"adds_modification", "adds modification"};
	public static final String[] REMOVES_MODIFICATION = new String[]{"removes_modification", "inhibits modification", "inhibits_modification"};
	public static final String[] ACTIVATES = new String[]{"increases_activity", "activation", "positive-activation"};
	public static final String[] INACTIVATES = new String[]{"decreases_activity", "negative-activation"};
	public static final String[] TRANSLOCATES = new String[]{"translocates", "translocation"};
	public static final String[] BINDS = new String[]{"binds"};
	public static final String[] INCREASES = new String[]{"increases"};
	public static final String[] DECREASES = new String[]{"decreases"};

	public static final String[] PARTICIPANT_A = new String[]{"participant_a"};
	public static final String[] PARTICIPANT_B = new String[]{"participant_b"};
	public static final String[] IDENTIFIER = new String[]{"identifier"};
	public static final String[] ENTITY_TYPE = new String[]{"entity_type"};
	public static final String[] PROTEIN = new String[]{"protein"};
	public static final String[] CHEMICAL = new String[]{"chemical"};
	public static final String[] FAMILY = new String[]{"family"};
	public static final String[] ENTITY_TEXT = new String[]{"entity_text"};
	public static final String[] MODIFICATION_TYPE = new String[]{"modification_type"};
	public static final String[] MODIFICATIONS = new String[]{"modifications"};
	public static final String[] POSITION = new String[]{"position"};
	public static final String[] FEATURES = new String[]{"features", "modifications"};
	public static final String[] FEATURE_TYPE = new String[]{"feature_type"};
	public static final String[] TO_LOCATION = new String[]{"to_location_text", "to_location"};
	public static final String[] TO_LOCATION_ID = new String[]{"to_location_id"};
	public static final String[] FROM_LOCATION = new String[]{"from_location_text", "from_location"};
	public static final String[] FROM_LOCATION_ID = new String[]{"from_location_id"};
	public static final String[] NEGATIVE_INFORMATION = new String[]{"negative_information", "negative"};
	public static final String[] EVIDENCE = new String[]{"evidence", "sentence"};

	public static final String[] PUBLICATION_REF = new String[]{"pmc_id", "filename", "docId"};
	public static final String[] CLUSTER_SCORE = new String[]{"clusterscore"};
	public static final String[] EVENTS = new String[]{"events"};
	public static final String[] SIGN = new String[]{"sign"};

	public static final Map ACTIVE = new HashMap();
	public static final Map INACTIVE = new HashMap();

	public static final State UNMODIFIED = new State();

	static
	{
		ACTIVE.put("modification_type", "active");
		INACTIVE.put("modification_type", "inactive");
	}

	public static boolean equal(String val, String... arr)
	{
		for (String s : arr)
		{
			if (s.equals(val)) return true;
		}
		return false;
	}

	public static Object get(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s)) return map.get(s);
		}
		return null;
	}

	public static String getString(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s)) return (String) map.get(s);
		}
		return null;
	}

	public static List<String> getStrings(Map map, String[] arr)
	{
		for (String s : arr)
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

	public static Map getMap(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s)) return (Map) map.get(s);
		}
		return null;
	}

	public static List getList(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s))
			{
				if (!(map.get(s) instanceof List)) return Collections.singletonList(map.get(s));
				else return (List) map.get(s);
			}
		}
		return null;
	}

	public static Integer getInt(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s)) return (Integer) map.get(s);
		}
		return null;
	}

	public static Double getDouble(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s))
			{
				Object o = map.get(s);
				if (o instanceof Double) return (Double) o;
				else return new Double((Integer) o);
			}
		}
		return null;
	}

	public static boolean has(Map map, String[] arr)
	{
		for (String s : arr)
		{
			if (map.containsKey(s)) return true;
		}
		return false;
	}

	public static boolean isList(Map map, String[] arr)
	{
		for (String s : arr)
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
