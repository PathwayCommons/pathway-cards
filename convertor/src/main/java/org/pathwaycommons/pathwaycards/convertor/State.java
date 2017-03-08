package org.pathwaycommons.pathwaycards.convertor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by BaburO on 1/18/2016.
 */
public class State
{
	Set<String> modifications;
	String compartmentID;
	String compartmentText;

	public State()
	{
		modifications = new HashSet<>();
	}

	public void addModification(String modifText)
	{
		modifications.add(modifText);
	}

	public void setCompartmentID(String compartmentID)
	{
		this.compartmentID = compartmentID;
	}

	public void setCompartmentText(String compartmentText)
	{
		this.compartmentText = compartmentText;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof State)
		{
			State st = (State) obj;

			if (this.compartmentID == null && st.compartmentID != null) return false;
			if (st.compartmentID == null && this.compartmentID != null) return false;
			if (this.compartmentID != null && !this.compartmentID.equals(st.compartmentID)) return false;

			if (this.modifications.size() == st.modifications.size() &&
				this.modifications.containsAll(st.modifications)) return true;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int h = 0;
		for (String m : modifications)
		{
			h += m.hashCode();
		}
		if (compartmentID != null) h += compartmentID.hashCode();

		return h;
	}
}
