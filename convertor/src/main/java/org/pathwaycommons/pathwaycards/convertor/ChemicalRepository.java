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

	public SmallMolecule getChemical(String id, String name, State st)
	{
		SmallMoleculeReference smr = getSMR(id, name);
		return getSM(smr, st);
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
			SmallMoleculeReference.class, "SmallMoleculeReference/" + NextNumber.get());
		model.add(smr);
		Xref xref = factory.create(
			UnificationXref.class, id.toUpperCase().startsWith("CHEBI") ? "http://identifiers.org/chebi/" + id : "http://identifiers.org/pubchem.compound/" + id);
		model.add(xref);
		xref.setDb(id.toUpperCase().startsWith("CHEBI") ? "ChEBI" : "PubChem-compound");
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
