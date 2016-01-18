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
public class ProteinRepository
{
	Map<String, ProteinReference> idToPR;
	Map<ProteinReference, Map<State, Protein>> refToProt;
	BioPAXFactory factory;
	long idBase;
	LocationRepository locRep;
	SeqModRepository modRep;
	Model model;

	public ProteinRepository()
	{
		idToPR = new HashMap<>();
		refToProt = new HashMap<>();
		factory = BioPAXLevel.L3.getDefaultFactory();
		idBase = System.currentTimeMillis();
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

	private ProteinReference getPR(String uniprot)
	{
		if (idToPR.containsKey(uniprot)) return idToPR.get(uniprot);
		ProteinReference pr = generatePR(uniprot);
		idToPR.put(uniprot, pr);
		return pr;
	}

	private ProteinReference generatePR(String uniprot)
	{
		ProteinReference pr = factory.create(ProteinReference.class, "ProteinReference/" + idBase++);
		model.add(pr);

		Xref xref = factory.create(
			UnificationXref.class, "http://identifiers.org/uniprot/" + uniprot);
		xref.setDb("UniProt Knowledgebase");
		xref.setId(uniprot);
		pr.addXref(xref);
		model.add(xref);

		String sym = HGNC.getSymbol(uniprot);

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

	private Protein getProtein(ProteinReference pr, State st)
	{
		if (refToProt.containsKey(pr) && refToProt.get(pr).containsKey(st))
			return refToProt.get(pr).get(st);

		Protein p = generateProtein(pr, st);

		if (!refToProt.containsKey(pr)) refToProt.put(pr, new HashMap<State, Protein>());
		refToProt.get(pr).put(st, p);
		return p;
	}

	private Protein generateProtein(ProteinReference pr, State st)
	{
		Protein protein = factory.create(Protein.class, "Protein/" + idBase++);
		model.add(protein);
		for (String mod : st.modifications)
		{
			Integer loc = null;
			if (mod.contains("@"))
			{
				loc = Integer.parseInt(mod.substring(mod.indexOf("@") + 1));
				mod = mod.substring(0, mod.indexOf("@"));
			}

			ModificationFeature mf = factory.create(
				ModificationFeature.class, "ModificationFeature" + idBase++);
			model.add(mf);

			SequenceModificationVocabulary voc = modRep.getVoc(mod);
			mf.setModificationType(voc);
			if (loc != null)
			{
				SequenceSite site = factory.create(SequenceSite.class, "SequenceSite/" + idBase++);
				site.setSequencePosition(loc);
				mf.setFeatureLocation(site);
				model.add(site);
			}
			protein.addFeature(mf);
		}

		if (st.compartment != null)
		{
			CellularLocationVocabulary voc = locRep.getVoc(st.compartment);
			protein.setCellularLocation(voc);
		}

		protein.setEntityReference(pr);
		return protein;
	}
}
