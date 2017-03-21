package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.biopax.paxtools.impl.MockFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class ChemicalRepositoryTest
{
	ChemicalRepository chemRep;

	@Before
	public void setUp() throws Exception
	{
		chemRep = new ChemicalRepository();
		Model model = Mocker.getMockModel();
		chemRep.setModel(model);
		LocationRepository locRep = new LocationRepository();
		locRep.setModel(model);
		chemRep.setLocationRepository(locRep);
	}

	@Test
	public void testGetChemical() throws Exception
	{
		IDType idType = IDType.PubChem;
		String id = "";
		String name = "";
		State state1 = Mocker.getMockState(null, null, null);

		SmallMolecule sm1 = chemRep.getChemical(id, idType, name, state1);

		SmallMolecule sm2 = chemRep.getChemical(id, idType, name, state1);

		Assert.assertEquals(true, sm1 == sm2);

		State state2 = Mocker.getMockState(null, null, null);
		state2.setCompartmentText("cytoplasm");
		state2.setCompartmentID("GO:0203045");

		SmallMolecule sm3 = chemRep.getChemical(id, idType, name, state2);

		Assert.assertEquals(false, sm1.equals(sm3));
		Assert.assertEquals(true, sm1.getEntityReference() == sm3.getEntityReference());
	}
}