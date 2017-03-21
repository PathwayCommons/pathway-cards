package org.pathwaycommons.pathwaycards.extractor;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.pattern.miner.SIFEnum;
import org.biopax.paxtools.pattern.miner.SIFSearcher;
import org.biopax.paxtools.pattern.util.Blacklist;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * @author Ozgun Babur
 */
public class SIFGenerator
{
	public boolean generate(Model model, String blacklistURL, String outFile) throws IOException
	{
		Blacklist blacklist = null;

		if (blacklistURL != null)
		{
			if (blacklistURL.startsWith("http"))
			{
				blacklist = new Blacklist(new URL(blacklistURL).openConnection().getInputStream());
			}
			else
			{
				blacklist = new Blacklist(blacklistURL);
			}
		}

		SIFSearcher searcher = new SIFSearcher(SIFEnum.CONTROLS_STATE_CHANGE_OF,
			SIFEnum.CONTROLS_TRANSPORT_OF, SIFEnum.CONTROLS_EXPRESSION_OF, SIFEnum.IN_COMPLEX_WITH);

		searcher.setBlacklist(blacklist);

		searcher.searchSIF(model, new FileOutputStream(outFile));

		return true;
	}
}
