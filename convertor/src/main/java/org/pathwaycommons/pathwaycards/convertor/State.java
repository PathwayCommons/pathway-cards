package org.pathwaycommons.pathwaycards.convertor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by BaburO on 1/18/2016.
 */
public class State
{
	Set<String> modifications;
	String compartment;

	public State()
	{
		modifications = new HashSet<>();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof State)
		{
			State st = (State) obj;

			if (this.compartment == null && st.compartment != null) return false;
			if (st.compartment == null && this.compartment != null) return false;
			if (this.compartment != null && !this.compartment.equals(st.compartment)) return false;

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
		if (compartment != null) h += compartment.hashCode();

		return h;
	}
}
