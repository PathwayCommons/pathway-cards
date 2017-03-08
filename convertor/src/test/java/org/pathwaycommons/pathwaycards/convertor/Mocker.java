package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

/**
 * @author Ozgun Babur
 */
public class Mocker
{
	public static Model getMockModel()
	{
		BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
		return factory.createModel();
	}

	public static State getMockState(String text)
	{
		State state = new State();
		if (text != null) state.addModification(text);
		return state;
	}
}
