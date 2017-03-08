package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class NextNumberTest
{
	@Test
	public void testGet() throws Exception
	{
		long num1 = NextNumber.get();
		long num2 = NextNumber.get();
		Assert.assertEquals(true, num1 + 1 == num2);
	}
}