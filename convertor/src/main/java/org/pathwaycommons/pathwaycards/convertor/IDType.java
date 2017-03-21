package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ozgun Babur
 */
public enum IDType
{
	UniProt("uniprot", "UniProt Knowledgebase", false, true, false, false, false),
	HGNC("hgnc", "HGNC", false, true, false, false, false),
	ChEBI("chebi", "ChEBI", true, false, false, false, false),
	PubChem("pubchem", "PubChem-compound", true, false, false, false, false),
	InterPro("interpro", "InterPro", false, false, false, true, false),
	Pfam("pfam", "Pfam", false, false, false, true, false),
	Bioentities("be", "Bioentities", false, false, true, true, false),
	UAZ("uaz", "UAZ", true, true, true, true, false),
	GO("go", "Gene Ontology", false, false, false, false, true),
	MeSH("mesh", "MeSH", false, false, false, false, true),
	HMDB("hmdb", "HMDB", true, false, false, false, false),
	ChemIDplus("chemidplus", "ChemIDplus", true, false, false, false, false),
	LINCS_MOL("lincs.molecule", "LINCS Molecule", true, false, false, false, false),
	;

	String text;
	String dbName;
	boolean isChemical;
	boolean isProtein;
	boolean isComplex;
	boolean isFamily;
	boolean isProcess;

	IDType(String text, String dbName, boolean isChemical, boolean isProtein, boolean isComplex, boolean isFamily,
		boolean isProcess)
	{
		this.text = text;
		this.dbName = dbName;
		this.isChemical = isChemical;
		this.isProtein = isProtein;
		this.isComplex = isComplex;
		this.isFamily = isFamily;
		this.isProcess = isProcess;
	}

	static Map<String, IDType> nameToEnum = new HashMap<>();

	static void populateReverseMap()
	{
		for (IDType type : IDType.values())
		{
			nameToEnum.put(type.text, type);
		}
	}

	public static IDType get(String text)
	{
		if (nameToEnum.isEmpty()) populateReverseMap();
		if (!nameToEnum.containsKey(text))
		{
			System.err.println("ID Type not recognized: " + text);
		}
		return nameToEnum.get(text);
	}

	private static Map<String, UnificationXref> xrefRepo = new HashMap<>();

	public void addUnifXref(XReferrable entity, String id, Model model, BioPAXFactory factory)
	{
		String s = text + "/" + id;
		UnificationXref xref;
		if (xrefRepo.containsKey(s))
		{
//			throw new RuntimeException("UnificationXrefs should not be asked more than once.");
			xref = xrefRepo.get(s);
//			System.out.println("xref = " + xref + " used for different objects.");
		}
		else
		{
			xref = factory.create(UnificationXref.class, "http://identifiers.org/" + s);
			try{model.add(xref);}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			xref.setDb(dbName);
			xref.setId(id);

			xrefRepo.put(s, xref);
		}

		entity.addXref(xref);
	}
}
