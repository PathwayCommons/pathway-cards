package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.*;

/**
 * Created by BaburO on 1/18/2016.
 */
public class FamilyRepository
{
	ProteinRepository protRep;
	ComplexRepository comRep;
	Map<String, ProteinReference> idToPR;
	Map<String, Map<State, Complex>> idToComplex;
	BioPAXFactory factory;
	LocationRepository locRep;
	SeqModRepository modRep;
	Model model;

	public FamilyRepository(ProteinRepository protRep, ComplexRepository comRep)
	{
		this.protRep = protRep;
		this.comRep = comRep;
		idToPR = new HashMap<>();
		idToComplex = new HashMap<>();
		factory = BioPAXLevel.L3.getDefaultFactory();
	}

	public void setLocationRepository(LocationRepository rep)
	{
		this.locRep = rep;
	}

	public void setModificationRepository(SeqModRepository rep)
	{
		this.modRep = rep;
	}

	public void setModel(Model model)
	{
		this.model = model;
	}

	public PhysicalEntity getFamily(String famID, IDType idType, String name, State st)
	{
		if (!idType.isFamily) throw new IllegalArgumentException("Wrong type for a family: " + idType);

		if (idType == IDType.Bioentities && Bioentities.isComplex(famID))
		{
			if (idToComplex.containsKey(famID) && idToComplex.get(famID).containsKey(st))
			{
				return idToComplex.get(famID).get(st);
			}

			Complex complex = getBioentitiesComplex(famID, st);

			if (!idToComplex.containsKey(famID)) idToComplex.put(famID, new HashMap<>());
			idToComplex.get(famID).put(st, complex);
			return complex;
		}

		// else it is a real family

		ProteinReference pr = getFamRef(famID, idType);
		return protRep.getProtein(pr, name, st);
	}

	private ProteinReference getFamRef(String famID, IDType idType)
	{
		if (idToPR.containsKey(famID)) return idToPR.get(famID);
		ProteinReference pr = generateFamRef(famID, idType);
		idToPR.put(famID, pr);
		return pr;
	}

	private ProteinReference generateFamRef(String famID, IDType idType)
	{
		ProteinReference pr = protRep.getPR(famID, idType);

		// Populate members for Pfam and InterPro

		Set<String> members = idType == IDType.Pfam ? Pfam.getMembers(famID) :
			idType == IDType.InterPro ? Interpro.getMembers(famID) :
			Collections.EMPTY_SET;

		for (String up : members)
		{
			if (HGNC.getSymbolOfUP(up) != null)
			{
				ProteinReference memPr = protRep.getPR(up, IDType.UniProt);
				pr.addMemberEntityReference(memPr);
			}
		}

		// Populate members for Bioentities

		if (idType == IDType.Bioentities)
		{
			for (Bioentities.Entry mem : Bioentities.getFamilyMembers(famID))
			{
				switch (mem.db)
				{
					case UniProt:
					{
						ProteinReference memPr = protRep.getPR(mem.id, IDType.UniProt);
						pr.addMemberEntityReference(memPr);
						break;
					}
					case HGNC:
					{
						String up = HGNC.getUniProtOfSymbol(mem.id);
						if (up != null)
						{
							ProteinReference memPr = protRep.getPR(up, IDType.UniProt);
							pr.addMemberEntityReference(memPr);
						}
						break;
					}
					case Bioentities:
					{
						if (Bioentities.isComplex(mem.id)) throw new RuntimeException("Violation of assumption. A " +
							"complex is a member of a family in Bioentities. Fam: " + famID + ", Mem: " + mem.id);

						if (!Bioentities.isFamily(mem.id)) throw new RuntimeException("Bioentities family member is " +
							"not recognized. Fam: " + famID + ", Mem: " + mem.id);

						ProteinReference memPr = getFamRef(mem.id, IDType.Bioentities);
						pr.addMemberEntityReference(memPr);
					}
				}
			}
		}

		return pr;
	}

	private Complex getBioentitiesComplex(String id, State st)
	{

		Set<Bioentities.Entry> members = Bioentities.getComplexMembers(id);

		Set<PhysicalEntity> pes = new HashSet<>();

		for (Bioentities.Entry member : members)
		{
			switch (member.db)
			{
				case UniProt:
				{
					pes.add(protRep.getProtein(member.id, IDType.UniProt, null, Constants.UNMODIFIED));
					break;
				}
				case HGNC:
				{
					String upID = HGNC.getUniProtOfSymbol(member.id);
					pes.add(protRep.getProtein(upID, IDType.UniProt, member.id, Constants.UNMODIFIED));
					break;
				}
				case Bioentities:
				{
					if (Bioentities.isComplex(member.id)) pes.add(getBioentitiesComplex(member.id, Constants.UNMODIFIED));
					else if (Bioentities.isFamily(member.id)) pes.add(getFamily(member.id, IDType.Bioentities, member.id, Constants.UNMODIFIED));
					break;
				}
			}
		}

		Complex complex = comRep.getComplex(pes, id);
		protRep.addStateDataToPE(complex, st);

		IDType.Bioentities.addUnifXref(complex, id, model, factory);

		return complex;
	}
}
