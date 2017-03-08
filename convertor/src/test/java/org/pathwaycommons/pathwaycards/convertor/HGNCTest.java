package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class HGNCTest
{
	@Test
	public void testGetSymbol() throws Exception
	{
		String symbol = HGNC.getSymbol("P05019");
		Assert.assertEquals(true, symbol.equals("IGF1"));
	}
}