package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by BaburO on 1/18/2016.
 */
public class FamilyRepository
{
	ProteinRepository protRep;
	Map<String, ProteinReference> idToPR;
	Map<ProteinReference, Map<State, Protein>> refToProt;
	BioPAXFactory factory;
	LocationRepository locRep;
	SeqModRepository modRep;
	Model model;

	public FamilyRepository(ProteinRepository protRep)
	{
		this.protRep = protRep;
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

	public Protein getFamily(String interpro, String name, State st) throws IOException
	{
		ProteinReference pr = getFamRef(interpro);
		Protein p = getProtein(pr, name, st);
		return p;
	}

	private ProteinReference getFamRef(String interpro) throws IOException
	{
		if (idToPR.containsKey(interpro)) return idToPR.get(interpro);
		ProteinReference pr = generateFamRef(interpro);
		idToPR.put(interpro, pr);
		return pr;
	}

	private ProteinReference generateFamRef(String interpro) throws IOException
	{
		ProteinReference pr = factory.create(ProteinReference.class, "ProteinReference/" + NextNumber.get());
		model.add(pr);

		Xref xref = factory.create(
			UnificationXref.class, "http://identifiers.org/interpro/" + interpro);
		xref.setDb("InterPro");
		xref.setId(interpro);
		pr.addXref(xref);
		model.add(xref);

		for (String up : Interpro.getMembers(interpro))
		{
			if (HGNC.getSymbol(up) != null)
			{
				ProteinReference memPr = protRep.getPR(up);
				pr.addMemberEntityReference(memPr);
			}
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
				if (mod.equals("phosphorylated") && aa != null)
				{
					switch (aa)
					{
						case "S": mod = "O-Phospho-L-serine"; break;
						case "T": mod = "O-Phospho-L-threonine"; break;
						case "Y": mod = "O-Phospho-L-tyrosine"; break;
					}
				}
			}

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
