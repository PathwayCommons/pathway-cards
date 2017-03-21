package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A repository for chemicals.
 */
public class ChemicalRepository
{
	Map<String, SmallMoleculeReference> idToSMR;
	Map<SmallMoleculeReference, Map<State, SmallMolecule>> refToSM;
	BioPAXFactory factory;
	LocationRepository locRep;
	Model model;

	public ChemicalRepository()
	{
		idToSMR = new HashMap<>();
		refToSM = new HashMap<>();
		factory = BioPAXLevel.L3.getDefaultFactory();
	}

	public void setLocationRepository(LocationRepository rep)
	{
		this.locRep = rep;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public SmallMolecule getChemical(String id, IDType idType, String name, State st)
	{
		SmallMoleculeReference smr = getSMR(id, idType, name);
		return getSM(smr, st);
	}

	private SmallMoleculeReference getSMR(String id, IDType idType, String name)
	{
		if (idToSMR.containsKey(id)) return idToSMR.get(id);
		SmallMoleculeReference smr = generateSMR(id, idType, name);
		idToSMR.put(id, smr);
		return smr;
	}

	private SmallMoleculeReference generateSMR(String id, IDType idType, String name)
	{
		if (!idType.isChemical)
		{
			throw new IllegalArgumentException("Wrong ID type for a small molecule: " + idType);
		}

		SmallMoleculeReference smr = factory.create(
			SmallMoleculeReference.class, "SmallMoleculeReference/" + NextNumber.get());
		model.add(smr);
		idType.addUnifXref(smr, id, model, factory);

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
		SmallMolecule sm = factory.create(SmallMolecule.class, "SmallMolecule/" + NextNumber.get());

		if (st.compartmentID != null)
		{
			CellularLocationVocabulary voc = locRep.getVoc(st.compartmentID, st.compartmentText);
			sm.setCellularLocation(voc);
		}

		sm.setEntityReference(smr);
		model.add(sm);
		return sm;
	}
}
