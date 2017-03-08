package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.CellularLocationVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;

import java.util.HashMap;
import java.util.Map;

/**
 * A repository for not duplicating CellularLocationVocabulary objects in BioPAX.
 */
public class LocationRepository
{
	Map<String, CellularLocationVocabulary> idToVoc;
	BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
	Model model;

	public LocationRepository()
	{
		idToVoc = new HashMap<>();
	}

	public CellularLocationVocabulary getVoc(String loc, String id)
	{
		if (id == null) id = loc;
		if (idToVoc.containsKey(id)) return idToVoc.get(id);

		CellularLocationVocabulary voc = factory.create(
			CellularLocationVocabulary.class, "CellularLocationVocabulary/" + NextNumber.get());
		voc.addTerm(loc);

		if (id.toLowerCase().startsWith("go:"))
		{
			String xid = id.substring(id.indexOf(":") + 1);
			UnificationXref xref = factory.create(UnificationXref.class, "http://identifiers.org/go/" + id);
			xref.setDb("Gene Ontology");
			xref.setId(xid);
			voc.addXref(xref);
			model.add(xref);
		}

		idToVoc.put(id, voc);
		model.add(voc);
		return voc;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}
}
