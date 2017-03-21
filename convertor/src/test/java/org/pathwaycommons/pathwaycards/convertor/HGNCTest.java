package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Ozgun Babur
 */
public class HGNCTest
{
	@Test
	public void testGetSymbol() throws Exception
	{
		String symbol = HGNC.getSymbolOfUP("P05019");
		Assert.assertEquals(true, symbol.equals("IGF1"));
	}
}