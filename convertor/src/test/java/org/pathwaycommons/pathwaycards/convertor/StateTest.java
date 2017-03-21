package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class StateTest
{
	@Test
	public void testEquals() throws Exception
	{
		State state1 = new State();
		String m1 = "phosphorylated";
		state1.addModification(m1, "serine", 13);

		State state2 = new State();
		String m2 = "phosphorylated";
		state2.addModification(m2, "serine", 13);

		Assert.assertEquals(true, state1.equals(state2));

		String cmpID = "GO:28374";
		state1.setCompartmentID(cmpID);
		Assert.assertEquals(false, state1.equals(state2));

		state2.setCompartmentID(cmpID);
		Assert.assertEquals(true, state1.equals(state2));

		// Compartment text should not affect equality
		String compName = "Cytoplasm";
		state1.setCompartmentText(compName);
		Assert.assertEquals(true, state1.equals(state2));
	}
}