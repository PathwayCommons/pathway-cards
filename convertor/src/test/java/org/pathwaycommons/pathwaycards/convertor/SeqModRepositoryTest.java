package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class SeqModRepositoryTest
{
	SeqModRepository modRep;

	@Before
	public void setUp() throws Exception
	{
		modRep = new SeqModRepository();
		modRep.setModel(Mocker.getMockModel());
	}

	@Test
	public void testGetVoc() throws Exception
	{
		String mod1 = "phosphorylated";

		SequenceModificationVocabulary voc1 = modRep.getVoc(mod1);
		SequenceModificationVocabulary voc2 = modRep.getVoc(mod1);

		Assert.assertEquals(true, voc1 == voc2);

		String mod3 = "methylated";
		SequenceModificationVocabulary voc3 = modRep.getVoc(mod3);
		Assert.assertEquals(false, voc1.equals(voc3));
	}
}