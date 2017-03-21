package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by BaburO on 1/18/2016.
 */
public class ProteinRepository
{
	Map<String, ProteinReference> idToPR;
	Map<ProteinReference, Map<State, Protein>> refToProt;
	BioPAXFactory factory;
	LocationRepository locRep;
	SeqModRepository modRep;
	Model model;

	public static Set<String> mappedUniprot = new HashSet<>();
	public static Set<String> unmappedUniprot = new HashSet<>();

	public ProteinRepository()
	{
		idToPR = new HashMap<>();
		refToProt = new HashMap<>();
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

	public Protein getProtein(String id, IDType idType, String name, State st)
	{
		if (!idType.isProtein)
			throw new IllegalArgumentException("Wrong type for protein: " + idType);

		if (name == null) name = HGNC.getSymbolOfUP(id);
		ProteinReference pr = getPR(id, idType);
		return getProtein(pr, name, st);
	}

	public ProteinReference getPR(String id, IDType idType)
	{
		if (id.contains(";")) throw new RuntimeException("ID contains \";\" : " + id);
//		{
//			for (String token : id.split(";"))
//			{
//				if (HGNC.getSymbolOfUP(token) != null)
//				{
//					id = token;
//					break;
//				}
//			}
//		}

		if (idToPR.containsKey(id)) return idToPR.get(id);

		ProteinReference pr = generatePR(id, idType);
		idToPR.put(id, pr);
		return pr;
	}

	public ProteinReference generatePR(String id, IDType idType)
	{
		if (!idType.isProtein && !idType.isFamily)
		{
			throw new IllegalArgumentException("Wrong ID type for protein reference: " + idType);
		}

		ProteinReference pr = factory.create(ProteinReference.class, "ProteinReference/" + NextNumber.get());
		model.add(pr);

		if (idType == IDType.HGNC)
		{
			String up = HGNC.getUniProtOfID(id);
			if (up != null)
			{
				id = up;
				idType = IDType.UniProt;
			}
		}

		idType.addUnifXref(pr, id, model, factory);

		String sym = HGNC.getSymbolOfUP(id);
		if (sym == null) sym = HGNC.getSymbolOfID(id);

		if (sym == null)
			unmappedUniprot.add(id);
		else
			mappedUniprot.add(id);

		if (sym != null)
		{
			Xref xref = factory.create(RelationshipXref.class, "http://identifiers.org/hgnc.symbol/" + sym);

			xref.setDb("HGNC Symbol");
			xref.setId(sym);
			pr.addXref(xref);
			model.add(xref);
		}

		return pr;
	}

	protected Protein getProtein(ProteinReference pr, String name, State st)
	{
		if (refToProt.containsKey(pr) && refToProt.get(pr).containsKey(st))
			return refToProt.get(pr).get(st);

		Protein p = generateProtein(pr, name, st);

		if (!refToProt.containsKey(pr)) refToProt.put(pr, new HashMap<>());
		refToProt.get(pr).put(st, p);
		return p;
	}

	protected Protein generateProtein(ProteinReference pr, String name, State st)
	{
		Protein protein = factory.create(Protein.class, "Protein/" + NextNumber.get());
		model.add(protein);
		protein.setDisplayName(name);
		addStateDataToPE(protein, st);

		protein.setEntityReference(pr);
		return protein;
	}

	public void addStateDataToPE(PhysicalEntity pe, State st)
	{
		for (Modification mod : st.modifications)
		{
			String type = mod.type;

			if ((type.equals("phosphorylated") || type.equals("phosphorylation")) && mod.aminoAcid != null)
			{
				switch (mod.aminoAcid)
				{
					case "S": type = "O-Phospho-L-serine"; break;
					case "T": type = "O-Phospho-L-threonine"; break;
					case "Y": type = "O-Phospho-L-tyrosine"; break;
					default: type += " " + mod.aminoAcid;
				}
			}

			ModificationFeature mf = factory.create(
				ModificationFeature.class, "ModificationFeature" + NextNumber.get());
			model.add(mf);

			SequenceModificationVocabulary voc = modRep.getVoc(type);
			mf.setModificationType(voc);
			if (mod.position != null)
			{
				SequenceSite site = factory.create(SequenceSite.class, "SequenceSite/" + NextNumber.get());
				site.setSequencePosition(mod.position);
				mf.setFeatureLocation(site);
				model.add(site);
			}
			pe.addFeature(mf);
		}

		if (st.compartmentID != null)
		{
			CellularLocationVocabulary voc = locRep.getVoc(st.compartmentID, st.compartmentText);
			pe.setCellularLocation(voc);
		}
	}
}
