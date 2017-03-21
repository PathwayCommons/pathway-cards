package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PhysicalEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ozgun Babur
 */
public class BioprocessRepository
{
	Map<String, Map<State, PhysicalEntity>> idToPE;
	BioPAXFactory factory;
	Model model;

	ProteinRepository protRep;

	public BioprocessRepository()
	{
		idToPE = new HashMap<>();
		factory = BioPAXLevel.L3.getDefaultFactory();
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public void setProtRep(ProteinRepository protRep)
	{
		this.protRep = protRep;
	}

	public PhysicalEntity getBioprocess(String id, IDType idType, String name, State st)
	{
		if (idToPE.containsKey(id) && idToPE.containsKey(st)) return idToPE.get(id).get(st);

		PhysicalEntity pe = generateProcess(id, idType, name, st);
		if (!idToPE.containsKey(id)) idToPE.put(id, new HashMap<>());
		idToPE.get(id).put(st, pe);
		return pe;
	}

	private PhysicalEntity generateProcess(String id, IDType idType, String name, State st)
	{
		PhysicalEntity pe = factory.create(PhysicalEntity.class, "PhysicalEntity/" + NextNumber.get());
		model.add(pe);

		pe.setDisplayName(name);
		protRep.addStateDataToPE(pe, st);

		idType.addUnifXref(pe, id, model, factory);

		return pe;
	}
}
