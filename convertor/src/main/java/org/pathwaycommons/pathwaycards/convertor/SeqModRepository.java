package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by BaburO on 1/18/2016.
 */
public class SeqModRepository
{
	Map<String, SequenceModificationVocabulary> modToVoc;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	long idBase = System.currentTimeMillis();
	Model model;

	public SeqModRepository()
	{
		modToVoc = new HashMap<>();
	}

	public SequenceModificationVocabulary getVoc(String mod)
	{
		if (modToVoc.containsKey(mod)) return modToVoc.get(mod);

		SequenceModificationVocabulary voc = factory.create(
			SequenceModificationVocabulary.class, "SequenceModificationVocabulary/" + idBase++);
		voc.addTerm(mod);
		modToVoc.put(mod, voc);
		model.add(voc);
		return voc;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}
}
