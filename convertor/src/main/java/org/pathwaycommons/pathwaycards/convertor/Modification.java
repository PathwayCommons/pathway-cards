package org.pathwaycommons.pathwaycards.convertor;

/**
 * @author Ozgun Babur
 */
public class Modification
{
	String type;
	String aminoAcid;
	Integer position;

	public Modification(String type, String aminoAcid, Integer position)
	{
		this.type = type;
		this.aminoAcid = aminoAcid;
		this.position = position;
	}

	@Override
	public int hashCode()
	{
		int h = 0;
		if (type != null) h += type.hashCode();
		if (aminoAcid != null) h += aminoAcid.hashCode();
		if (position != null) h += position.hashCode();
		return h;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Modification)
		{
			Modification m = (Modification) obj;
			return ((type == null && m.type == null) || (type != null && type.equals(m.type))) &&
				((aminoAcid == null && m.aminoAcid == null) || (aminoAcid != null && aminoAcid.equals(m.aminoAcid))) &&
				((position == null && m.position == null) || (position != null && position.equals(m.position)));
		}
		return false;
	}
}
