package org.pathwaycommons.pathwaycards.convertor;

import com.github.jsonldjava.utils.JsonUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.cbio.causality.util.Progress;
import org.cbio.causality.util.TermCounter;

import java.io.*;
import java.util.*;

import static org.pathwaycommons.pathwaycards.convertor.Constants.*;

/**
 * Converts the extended FRIES format (Frext) into BioPAX.
 */
public class FrextToBioPAX
{
	/**
	 * Repository for small molecules.
	 */
	ChemicalRepository chemRep;

	/**
	 * Repository for proteins.
	 */
	ProteinRepository protRep;

	/**
	 * Repository for protein families.
	 */
	FamilyRepository famRep;

	/**
	 * The one and only BioPAX model.
	 */
	Model model;

	/**
	 * The factory that can generate BioPAX objects.
	 */
	BioPAXFactory factory;

	/**
	 * Repository for conversions.
	 */
	ConversionRegistry cnvReg;

	/**
	 * Repository for controls.
	 */
	ControlRegistry ctrReg;

	/**
	 * Repository for template reactions.
	 */
	TemplateReactionRegistry trReg;

	/**
	 * Repository for molecular interactions.
	 */
	MolecularInteractionRegistry miReg;

	/**
	 * A counter for keeping the statistics of the cases that is encountered during the conversion.
	 */
	TermCounter tc = new TermCounter();

	/**
	 * Helper map for recognizing amino acids.
	 */
	private static final Map<String, String> AA_MAP = new HashMap<>();

	/**
	 * Prepare the helper map for amino acid recognition.
	 */
	static
	{
		Map<String, String[]> map = new HashMap<>();
		map.put("S", new String[]{"s", "ser", "ser-", "serine", "serine s", "serine residue", "serine residues"});
		map.put("T", new String[]{"t", "thr", "thr-", "threonine", "threonine t", "threonine residue", "threonine residues"});
		map.put("Y", new String[]{"y", "tyr", "tyr-", "tyrosine", "tyrosine y", "tyrosine residue", "tyrosine residues"});

		for (String s : map.keySet())
		{
			for (String ss : map.get(s))
			{
				AA_MAP.put(ss, s);
			}
		}
	}

	/**
	 * Constructor initializes all repositories.
	 */
	public FrextToBioPAX()
	{
		chemRep = new ChemicalRepository();
		protRep = new ProteinRepository();
		famRep = new FamilyRepository(protRep);
		LocationRepository locRep = new LocationRepository();
		SeqModRepository modRep = new SeqModRepository();
		chemRep.setLocationRepository(locRep);
		protRep.setLocationRepository(locRep);
		protRep.setModificationRepository(modRep);
		famRep.setLocationRepository(locRep);
		famRep.setModificationRepository(modRep);
		factory = BioPAXLevel.L3.getDefaultFactory();
		model = factory.createModel();
		protRep.setModel(model);
		chemRep.setModel(model);
		famRep.setModel(model);
		modRep.setModel(model);
		locRep.setModel(model);
		cnvReg = new ConversionRegistry();
		cnvReg.setModel(model);
		ctrReg = new ControlRegistry();
		ctrReg.setModel(model);
		trReg = new TemplateReactionRegistry();
		trReg.setModel(model);
		miReg = new MolecularInteractionRegistry();
		miReg.setModel(model);
	}

	/**
	 * Converts and adds frex output from a single paper in JSON to the current BioPAX model.
	 */
	public void addToModel(Map map) throws IOException
	{
		// If there is nothing to add, just return.
		if (map.isEmpty())
		{
			tc.addTerm("Card is empty");
			return;
		}

		// The doc ID contains the PMC ID of the the current paper
		String docID = getString(map, PUBLICATION_REF);

		// Iterate events and add to the model
		for (Object o : getList(map, EVENTS))
		{
			Map event = (Map) o;

			Map interaction = getMap(event, INTERACTION_TYPE);

			// If the event is a "not happened" event, just skip it
			if (equal(getString(interaction, SIGN), NEGATIVE_INFORMATION))
			{
				tc.addTerm("Not happened");
				return;
			}

			String intType = getString(interaction, INTERACTION_TYPE);
			String intSubType = getString(interaction, INTERACTION_SUB_TYPE);

//--- DEBUG

//			lookAtKeys(event, "");

			tc.addTerm(intType + " ---- " + intSubType);

//			if (event.containsKey("participant_a"))
//			{
//				String term = ((Map) event.get("participant_a")).get("entity_type").toString();
//				tc.addTerm(term);
//			}

			if (true) return;
//--- DEBUG

			// Skip if interaction type is not set
			if (intType == null)
			{
				tc.addTerm("Interaction type is " + intType);
				return;
			}

			// Read participant As
			Set<PhysicalEntity> pAs = getParticipants(get(event, PARTICIPANT_A));

			// Skip if no participant A found
			if (pAs == null || pAs.isEmpty())
			{
				tc.addTerm("Participant A is null or empty");
				return;
			}

			// Read the "from" location of participant B
			Map<String, String> fromLoc = readLocation(getMap(event, FROM_LOCATION));

			// Generate participant B
			PhysicalEntity pB = getParticipant(getMap(event, PARTICIPANT_B), null, fromLoc);

			// Skip if no participant B is found
			if (pB == null)
			{
				tc.addTerm("Participant B is null");
				return;
			}

			PhysicalEntity pBm = null;

			// Read the "to" location of participant B
			Map<String, String> toLoc = readLocation(getMap(event, TO_LOCATION));

			List modifs = null;
			if (equal(intType, REGULATION))
			{
				modifs = getModifications(event, intSubType);
			}

			if (equal(intType, REGULATION) || equal(intType, BINDS))
			{
//				List modifs = getList(event, MODIFICATIONS);
				pBm = getParticipant(getMap(event, PARTICIPANT_B), modifs, toLoc);
			}
			else if (equal(intType, ACTIVATES))
			{
				if (equal(intSubType, ACTIVATES))
				{
					pBm = getParticipant(getMap(event, PARTICIPANT_B), ACTIVE, toLoc);
				}
				else if (equal(intSubType, INACTIVATES))
				{
					pBm = getParticipant(getMap(event, PARTICIPANT_B), INACTIVE, toLoc);
				}
			}
			else if (equal(intType, TRANSLOCATES))
			{
				pBm = getParticipant(getMap(event, PARTICIPANT_B), null, toLoc);
			}
			else if (equal(intType, INCREASES) || equal(intType, DECREASES))
			{
				pBm = pB;
			}
			else
			{
				System.err.println("Unhandled interaction type = " + intType);
			}

			if (equal(intType, REMOVES_MODIFICATION))
			{
				PhysicalEntity temp = pB;
				pB = pBm;
				pBm = temp;
			}

			// Create the event

			Interaction inter;

			// If it is a binding event, create a MolecularInteraction with all participants
			if (equal(intType, BINDS))
			{
				Set<PhysicalEntity> set = new HashSet<>(pAs);
				set.add(pBm);
				inter = miReg.getMolecularInteraction(set.toArray(new PhysicalEntity[set.size()]));
			}
			// Otherwise it is a child of Conversion
			else
			{
				Class<? extends Conversion> cnvClazz;

				// If it is a tranlocation, create a Transport event
				if (equal(intType, TRANSLOCATES))
				{
					cnvClazz = Transport.class;
				}
				// Otherwise, it is a BiochemicalReaction
				else
				{
					cnvClazz = BiochemicalReaction.class;
				}

				// create the Conversion
				inter = cnvReg.getConversion(pB, pBm, cnvClazz);
			}

			Interaction ctr = equal(intType, BINDS) ? inter : ctrReg.getControl(pAs, inter);

			if (equal(intType, DECREASES)) ((Control) ctr).setControlType(ControlType.INHIBITION);

			// Fix for REACH cards
			if (map.containsKey("pmc_id") && !map.get("pmc_id").toString().startsWith("PMC"))
			{
				event.put("pmc_id", "PMC" + map.get("pmc_id").toString());
			}

			// Add source article ID
			String source = getString(event, PUBLICATION_REF);
			if (source != null)
			{
				for (String s : source.split("-|\\.|_"))
				{
					if (s.startsWith("PMC"))
					{
						if (s.length() > 10) s = s.substring(0, 10);
						PublicationXref xref = factory.create(PublicationXref.class, "PublicationXref/" + NextNumber.get());
						xref.setDb("PMC International");
						xref.setId(s);
						ctr.addXref(xref);
						model.add(xref);
					}
				}
			}

			List<String> evidences = getStrings(map, EVIDENCE);
			if (evidences != null)
			{
				for (String evidence : evidences)
				{
					ctr.addComment(evidence);
				}
			}
			// Cluster score
			Double score = getDouble(event, CLUSTER_SCORE);
			if (score != null)
			{
				ctr.addComment("Cluster score: " + score);
			}

			tc.addTerm("Converted successfully");
		}

	}

	// todo
	private List getModifications(Map event, String intSubType)
	{
		return null;
	}

	/**
	 * Debug method.
	 */
	private void lookAtKeys(Map map, String root)
	{
		for (Object o : map.keySet())
		{
			String term = root + " -> " + o.toString();
			tc.addTerm(term);

			Object v = map.get(o);
			if (v instanceof Map)
			{
				lookAtKeys((Map) v, term);
			}
			else if (v instanceof List)
			{
				for (Object item : ((List) v))
				{
					if (item instanceof Map)
					{
						lookAtKeys((Map) item, term);
					}
				}
			}
		}
	}

	/**
	 * Gets the set of molecules from the given map or list in JSON.
	 */
	private Set<PhysicalEntity> getParticipants(Object o) throws IOException
	{
		if (o == null) return null;
		if (o instanceof List) return getParticipants((List) o);
		else
		{
			PhysicalEntity pe = getParticipant((Map) o, null, null);
			if (pe != null) return Collections.singleton(pe);
		}
		return null;
	}

	/**
	 * Gets the set of molecules from the given JSON list.
	 */
	private Set<PhysicalEntity> getParticipants(List list) throws IOException
	{
		Set<PhysicalEntity> pes = new HashSet<>();
		for (Object o : list)
		{
			if (o instanceof Map)
			{
				PhysicalEntity pe = getParticipant((Map) o, null, null);
				if (pe != null) pes.add(pe);
			}
			else pes.addAll(getParticipants((List) o));
		}
		return pes;
	}

	/**
	 * Generates the PhysicalEntity.
	 */
	private PhysicalEntity getParticipant(Map map, Object modifs, Map<String, String> location) throws IOException
	{
		if (map == null) return null;

		String type = getString(map, ENTITY_TYPE);
		String id = getString(map, IDENTIFIER);

		if (type == null || type.equals("unknown")) return null;
		if (id == null) return null;

		String idType = "";
		if (id.contains(":"))
		{
			idType = id.substring(0, id.indexOf(":")).trim();
			id = id.substring(id.indexOf(":") + 1);
		}

		if (!idType.equalsIgnoreCase("uniprot") &&
			!idType.equalsIgnoreCase("pubchem") &&
			!idType.equalsIgnoreCase("chebi") &&
			!idType.equalsIgnoreCase("interpro")
			)
		{
			return null;
		}

		String name = getString(map, ENTITY_TEXT);

		List feats = getList(map, FEATURES);
		State st = feats == null && modifs == null && location == null ? UNMODIFIED : new State();

		addFeaturesToState(st, feats);
		addModificationsToState(st, modifs);
		addLocationToState(st, location);

		if (id.startsWith("CHEBI:")) type = CHEMICAL[0];

		if (equal(type, PROTEIN))
		{
			return protRep.getProtein(id, name, st);
		}
		else if (equal(type, CHEMICAL))
		{
			return chemRep.getChemical(id, name, st);
		}
		else if (equal(type, FAMILY))
		{
			return famRep.getFamily(id, name, st);
		}
		return null;
	}

	/**
	 * A small conversion to the format of the locations in the map.
	 * todo: This is probably unnecessary and needs to be revisited.
	 */
	private Map<String, String> readLocation(Map map)
	{
		if (map == null) return null;
		String fromText = getString(map, ENTITY_TEXT);
		String fromID = getString(map, IDENTIFIER);
		return getNameIDMap(fromText, fromID);
	}

	private Map<String, String> getNameIDMap(String name, String id)
	{
		if (name == null && id == null) return null;
		Map<String, String> map = new HashMap<>();
		map.put("name", name);
		map.put("id", id);
		return map;
	}

	private void addFeaturesToState(State st, List feats)
	{
		if (feats == null) return;

		for (Object o : feats)
		{
			Map m = (Map) o;
			String mType = getString(m, MODIFICATION_TYPE);
			if (mType == null) continue;

			String[] pos = getPositions(m);
			if (pos != null && pos.length > 0 && pos[0] != null)
			{
				for (String p : pos)
				{
					if (p == null) continue;
					st.modifications.add(mType + "@" + p);
				}
			}
			else st.modifications.add(mType);
		}
	}

	private void addLocationToState(State st, Map<String, String> map)
	{
		if (map == null) return;
		String name = map.get("name");
		String id = map.get("id");
		if (name == null) name = id;
		if (id == null) id = name;
		st.compartmentText = name;
		st.compartmentID = id;
	}

	private void addModificationsToState(State st, Object o)
	{
		if (o == null) return;
		if (o instanceof List)
		{
			for (Object oo : (List) o)
			{
				addModificationtoState(st, (Map) oo);
			}
		}
		else addModificationtoState(st, (Map) o);
	}

	private void addModificationtoState(State st, Map map)
	{
		String type = getString(map, MODIFICATION_TYPE);
		String[] positions = getPositions(map);
		if (positions != null && positions.length > 0 && positions[0] != null)
		{
			for (String p : positions)
			{
				st.modifications.add(type + "@" + p);
			}
		}
		else st.modifications.add(type);
	}

	private String[] getPositions(Map m)
	{
		if (!has(m, POSITION)) return null;

		if (isList(m, POSITION))
		{
			List list = getList(m, POSITION);

			List<String> posList = new ArrayList<>();

			for (Object o : list)
			{
				if (o instanceof Integer)
				{
					posList.add(getPositionString(o));
				}
			}

			return posList.toArray(new String[posList.size()]);
		}
		else
		{
			Object o = get(m, POSITION);
			String str = getPositionString(o);
			if (str == null) return null;
			return new String[]{str};
		}
	}

	private String getPositionString(Object o)
	{
		if (o instanceof Integer)
		{
			return o.toString();
		}
		else
		{
			String s = (String) o;

			int i = getStartIndexOfEndNumber(s);
			if (i < 0) return null;

			int pos = Integer.parseInt(s.substring(i));

			s = s.substring(0, i).trim().toLowerCase();

			String aa = AA_MAP.get(s);

			return aa == null ? "" + pos : aa + pos;
		}
	}

	private int getStartIndexOfEndNumber(String s)
	{
		int x = -1;
		for (int i = s.length() - 1; i >= 0; i--)
		{
			if (!Character.isDigit(s.charAt(i)))
			{
				x = i + 1;
				break;
			}
		}

		if (x >= s.length()) return -1;
		else return x;
	}

	public void writeModel(String filename) throws FileNotFoundException
	{
		SimpleIOHandler io = new SimpleIOHandler(BioPAXLevel.L3);
		io.convertToOWL(model, new FileOutputStream(filename));
	}

	/**
	 * Make sure that directories are not nested. Otherwise duplications will happen.
	 * @param dirs
	 */
	public void covertFolders(boolean watchProgress, String... dirs) throws IOException
	{
		for (String dir : dirs)
		{
			File[] files = new File(dir).listFiles();

			boolean multiFile = files != null &&  files.length > 1;

			Progress p = watchProgress && multiFile ? new Progress(files.length, "Processing directory: " + dir) : null;

			for (File f : files)
			{
				if (watchProgress && multiFile) p.tick();
				if (f.isDirectory()) covertFolders(false, f.getPath());
				else if (f.getName().endsWith(".json"))
				{
					addToModel(watchProgress && !multiFile, f.getPath());
				}

//				if (Math.random() < 0.1) break;
			}
		}
	}



	public void addToModel(boolean watchProgress, String file) throws IOException
	{
		Object o = JsonUtils.fromInputStream(new FileInputStream(file));
		addToModel((Map) o);
	}

	void printClusterScoreDistribution(List list)
	{
		TermCounter tc = new TermCounter();
		for (Object o : list)
		{
			Map map = (Map) o;
			Object x = get(map, CLUSTER_SCORE);
			if (x != null) tc.addTerm(x.getClass().getName());
			else tc.addTerm("null");
		}
		tc.print();
	}

	public static void main(String[] args) throws IOException
	{
		FrextToBioPAX c = new FrextToBioPAX();
		c.covertFolders(true, "/media/babur/6TB1/REACH-cards/sample");
		Interpro.write();
		c.writeModel("/media/babur/6TB1/REACH-cards/temp.owl");

		System.out.println("ProteinRepository.mappedUniprot.size() = " + ProteinRepository.mappedUniprot.size());
		System.out.println("ProteinRepository.unmappedUniprot.size() = " + ProteinRepository.unmappedUniprot.size());
		System.out.println(new ArrayList<>(ProteinRepository.unmappedUniprot));
		c.tc.print();
	}
}
