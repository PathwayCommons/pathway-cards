package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by BaburO on 1/18/2016.
 */
public class ChemicalRepository
{
	Map<String, SmallMoleculeReference> idToSMR;
	Map<SmallMoleculeReference, Map<State, SmallMolecule>> refToSM;
	BioPAXFactory factory;
	long idBase;
	LocationRepository locRep;
	Model model;

	public ChemicalRepository()
	{
		idToSMR = new HashMap<>();
		refToSM = new HashMap<>();
		factory = BioPAXLevel.L3.getDefaultFactory();
		idBase = System.currentTimeMillis();
	}

	public void setLocationRepository(LocationRepository rep)
	{
		this.locRep = rep;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	private SmallMoleculeReference getSMR(String id, String name)
	{
		if (idToSMR.containsKey(id)) return idToSMR.get(id);
		SmallMoleculeReference smr = generateSMR(id, name);
		idToSMR.put(id, smr);
		return smr;
	}

	private SmallMoleculeReference generateSMR(String id, String name)
	{
		SmallMoleculeReference smr = factory.create(
			SmallMoleculeReference.class, "SmallMoleculeReference/" + idBase++);
		model.add(smr);
		Xref xref = factory.create(
			UnificationXref.class, "http://identifiers.org/pubchem.compound/" + id);
		model.add(xref);
		xref.setDb("PubChem-compound");
		xref.setId(id);
		smr.addXref(xref);

		smr.setDisplayName(name);
		return smr;
	}

	private SmallMolecule getSM(SmallMoleculeReference smr, State st)
	{
		if (refToSM.containsKey(smr) && refToSM.get(smr).containsKey(st))
			return refToSM.get(smr).get(st);

		SmallMolecule sm = generateSM(smr, st);

		if (!refToSM.containsKey(smr)) refToSM.put(smr, new HashMap<State, SmallMolecule>());
		refToSM.get(smr).put(st, sm);
		return sm;
	}

	private SmallMolecule generateSM(SmallMoleculeReference smr, State st)
	{
		SmallMolecule sm = factory.create(SmallMolecule.class, "SmallMolecule/" + idBase++);

		if (st.compartment != null)
		{
			CellularLocationVocabulary voc = factory.create(
				CellularLocationVocabulary.class, "CellularLocationVocabulary/" + idBase++);
			voc.addTerm(st.compartment);
			sm.setCellularLocation(voc);
		}

		sm.setEntityReference(smr);
		model.add(sm);
		return sm;
	}
}
