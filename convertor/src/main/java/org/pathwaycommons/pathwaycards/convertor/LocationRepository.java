package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.CellularLocationVocabulary;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by BaburO on 1/18/2016.
 */
public class LocationRepository
{
	Map<String, CellularLocationVocabulary> locToVoc;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	long idBase = System.currentTimeMillis();
	Model model;

	public LocationRepository()
	{
		locToVoc = new HashMap<>();
	}

	public CellularLocationVocabulary getVoc(String loc)
	{
		if (locToVoc.containsKey(loc)) return locToVoc.get(loc);

		CellularLocationVocabulary voc = factory.create(
			CellularLocationVocabulary.class, "CellularLocationVocabulary/" + idBase++);
		voc.addTerm(loc);
		locToVoc.put(loc, voc);
		model.add(voc);
		return voc;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}
}
