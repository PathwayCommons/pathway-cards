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

	public Protein getProtein(String uniprot, String name, State st)
	{
		ProteinReference pr = getPR(uniprot);
		Protein p = getProtein(pr, name, st);
		return p;
	}

	public ProteinReference getPR(String uniprot)
	{
		if (uniprot.contains(";"))
		{
			for (String uni : uniprot.split(";"))
			{
				if (HGNC.getSymbol(uni) != null)
				{
					uniprot = uni;
					break;
				}
			}
		}
		else if (uniprot.contains("\n"))
		{
			uniprot = "Parse-error";
		}

		if (idToPR.containsKey(uniprot)) return idToPR.get(uniprot);

		ProteinReference pr = generatePR(uniprot);
		idToPR.put(uniprot, pr);
		return pr;
	}

	private ProteinReference generatePR(String uniprot)
	{
		ProteinReference pr = factory.create(ProteinReference.class, "ProteinReference/" + NextNumber.get());
		model.add(pr);

		Xref xref = factory.create(
			UnificationXref.class, "http://identifiers.org/uniprot/" + uniprot);
		xref.setDb("UniProt Knowledgebase");
		xref.setId(uniprot);
		pr.addXref(xref);
		model.add(xref);

		String sym = HGNC.getSymbol(uniprot);

		if (sym == null)
			unmappedUniprot.add(uniprot);
		else
			mappedUniprot.add(uniprot);

		if (sym != null)
		{
			xref = factory.create(RelationshipXref.class,
				"http://identifiers.org/hgnc.symbol/" + sym);

			xref.setDb("HGNC Symbol");
			xref.setId(sym);
			pr.addXref(xref);
			model.add(xref);
		}

		return pr;
	}

	private Protein getProtein(ProteinReference pr, String name, State st)
	{
		if (refToProt.containsKey(pr) && refToProt.get(pr).containsKey(st))
			return refToProt.get(pr).get(st);

		Protein p = generateProtein(pr, name, st);

		if (!refToProt.containsKey(pr)) refToProt.put(pr, new HashMap<State, Protein>());
		refToProt.get(pr).put(st, p);
		return p;
	}

	private Protein generateProtein(ProteinReference pr, String name, State st)
	{
		Protein protein = factory.create(Protein.class, "Protein/" + NextNumber.get());
		model.add(protein);
		protein.setDisplayName(name);
		for (String mod : st.modifications)
		{
			Integer loc = null;
			String aa = null;
			if (mod.contains("@"))
			{
				String s = mod.substring(mod.indexOf("@") + 1);
				if (s.startsWith("Y") || s.startsWith("S") || s.startsWith("T"))
				{
					aa = s.substring(0, 1);
					s = s.substring(1);
				}
				loc = Integer.parseInt(s);
				mod = mod.substring(0, mod.indexOf("@"));
				if ((mod.equals("phosphorylated") || mod.equals("phosphorylation")) && aa != null)
				{
					switch (aa)
					{
						case "S": mod = "O-Phospho-L-serine"; break;
						case "T": mod = "O-Phospho-L-threonine"; break;
						case "Y": mod = "O-Phospho-L-tyrosine"; break;
					}
				}
			}

			if (mod.equals("phosphorylated") || mod.equals("phosphorylation")) mod = "phosphorylated residue";

			ModificationFeature mf = factory.create(
				ModificationFeature.class, "ModificationFeature" + NextNumber.get());
			model.add(mf);

			SequenceModificationVocabulary voc = modRep.getVoc(mod);
			mf.setModificationType(voc);
			if (loc != null)
			{
				SequenceSite site = factory.create(SequenceSite.class, "SequenceSite/" + NextNumber.get());
				site.setSequencePosition(loc);
				mf.setFeatureLocation(site);
				model.add(site);
			}
			protein.addFeature(mf);
		}

		if (st.compartmentID != null)
		{
			CellularLocationVocabulary voc = locRep.getVoc(st.compartmentID, st.compartmentText);
			protein.setCellularLocation(voc);
		}

		protein.setEntityReference(pr);
		return protein;
	}
}
