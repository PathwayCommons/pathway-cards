package org.pathwaycommons.pathwaycards.convertor;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Ozgun Babur
 */
public class PfamTest
{
	@Test
	public void testGetMembers() throws Exception
	{
		Set<String> members = Pfam.getMembers("PF07728");
		Assert.assertEquals(true, members.contains("Q9C0G6")); // that is DNAH6
		Assert.assertEquals(true, members.size() == 35); // that is DNAH6
	}
}