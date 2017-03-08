package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.biopax.paxtools.model.level3.CellularLocationVocabulary;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class LocationRepositoryTest
{
	LocationRepository locRep;
	@Before
	public void setUp() throws Exception
	{
		locRep = new LocationRepository();
		locRep.setModel(Mocker.getMockModel());
	}

	@Test
	public void testGetVoc() throws Exception
	{
		String name1 = "cytoplasm";
		String id1 = "GO:00023";
		String name2 = "cytoplasm alternative name";
		String id2 = "GO:00023";
		String name3 = "nucleus";
		String id3 = "GO:85746";

		CellularLocationVocabulary voc1 = locRep.getVoc(name1, id1);
		CellularLocationVocabulary voc2 = locRep.getVoc(name2, id2);
		CellularLocationVocabulary voc3 = locRep.getVoc(name3, id3);

		Assert.assertEquals(true, voc1 == voc2);
		Assert.assertEquals(false, voc1.equals(voc3));
	}
}