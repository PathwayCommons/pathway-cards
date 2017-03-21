package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class ProteinRepositoryTest
{
	ProteinRepository proteinRep;

	@org.junit.Before
	public void setUp() throws Exception
	{
		proteinRep = new ProteinRepository();
		Model model = Mocker.getMockModel();
		proteinRep.setModel(model);
		LocationRepository locRep = new LocationRepository();
		locRep.setModel(model);
		SeqModRepository modRep = new SeqModRepository();
		modRep.setModel(model);
		proteinRep.setLocationRepository(locRep);
		proteinRep.setModificationRepository(modRep);
	}

	@org.junit.Test
	public void testGetProtein() throws Exception
	{
		String uniprotID = "P05019";
		String text = "IGF-1";
		String state1 = "phosphorylated";
		IDType idType = IDType.UniProt;
		Protein p1 = proteinRep.getProtein(uniprotID, idType, text, Mocker.getMockState(state1, "S", 10));

		Protein p2 = proteinRep.getProtein(uniprotID, idType, "alternative text", Mocker.getMockState(state1, "S", 10));

		Assert.assertEquals(true, p1 == p2);

		String state2 = "acetylated";
		Protein p3 = proteinRep.getProtein(uniprotID, idType, text, Mocker.getMockState(state2, "T", 20));

		Assert.assertEquals(false, p1.equals(p3));
		Assert.assertEquals(true, p1.getEntityReference() == p3.getEntityReference());
	}
}