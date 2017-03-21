package org.pathwaycommons.pathwaycards.extractor;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Ozgun Babur
 */
public class ModificationRecorder
{
	public static void record(Model model, String filename) throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write("HGNC Symbol\tUniProt Name\tModification\tSite\tID");
		Set<String> memory = new HashSet<>();

		for (Protein protein : model.getObjects(Protein.class))
		{
			EntityReference er = protein.getEntityReference();
			if (er == null) continue;

			String symbol  = IDMapper.getHGNCSymbol (er);
			String uniprot = IDMapper.getUniProtName(er);

			// iterate over features

			for (EntityFeature ef : protein.getFeature())
			{
				if (ef instanceof ModificationFeature)
				{
					ModificationFeature mf = (ModificationFeature) ef;

					SequenceModificationVocabulary typeVoc = mf.getModificationType();
					String term = null;
					Set<String> terms = typeVoc.getTerm();
					Iterator<String> iter = terms.iterator();
					while ((term == null || term.isEmpty()) && iter.hasNext())
					{
						term =  iter.next();
					}

					if (term == null || term.isEmpty()) continue;

					int site = 0;
					SequenceLocation seqLoc = mf.getFeatureLocation();
					if (seqLoc instanceof SequenceSite)
					{
						site = ((SequenceSite) seqLoc).getSequencePosition();
					}
					if (site < 0) site = 0;

					String hash = symbol + uniprot + term + site;
					if (memory.contains(hash)) continue;
					else memory.add(hash);
					writer.write("\n" + symbol + "\t" + uniprot + "\t" + term + "\t" + site + "\t" +
						protein.getUri());
				}
			}
		}

		writer.close();
	}
}
