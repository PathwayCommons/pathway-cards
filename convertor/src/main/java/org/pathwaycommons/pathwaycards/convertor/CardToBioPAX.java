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
 * Created by babur on 1/19/2016.
 */
public class CardToBioPAX
{
	ChemicalRepository chemRep;
	ProteinRepository protRep;
	FamilyRepository famRep;
	Model model;
	BioPAXFactory factory;
	ConversionRegistry cnvReg;
	ControlRegistry ctrReg;
	TemplateReactionRegistry trReg;

	TermCounter tc = new TermCounter();

	private static final Map<String, String[]> AA_HELPER = new HashMap<>();
	private static final Map<String, String> AA_MAP = new HashMap<>();

	static
	{
		AA_HELPER.put("S", new String[]{"s", "ser", "ser-", "serine", "serine s", "serine residue", "serine residues"});
		AA_HELPER.put("T", new String[]{"t", "thr", "thr-", "threonine", "threonine t", "threonine residue", "threonine residues"});
		AA_HELPER.put("Y", new String[]{"y", "tyr", "tyr-", "tyrosine", "tyrosine y", "tyrosine residue", "tyrosine residues"});

		for (String s : AA_HELPER.keySet())
		{
			for (String ss : AA_HELPER.get(s))
			{
				AA_MAP.put(ss, s);
			}
		}
	}

	public CardToBioPAX()
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
	}

	public void addToModel(Map map) throws IOException
	{
		if (map.isEmpty())
		{
			tc.addTerm("Card is empty");
			return;
		}

		Map ext = getMap(map, EXTRACTED_INFORMATION);
		if (ext == null) ext = map;

		if (get(ext, NEGATIVE_INFORMATION) == Boolean.TRUE)
		{
			tc.addTerm("Negative information");
			return;
		}

		String intType = getString(ext, INTERACTION_TYPE);

		// fix for table reading cards
		if ((intType == null || intType.isEmpty()) && ext.containsKey("fold"))
		{
			List list = (List) ext.get("fold");
			for (Object o : list)
			{
				Map m = (Map) o;
				Object s = m.get("interaction_type");
				if (s != null)
				{
					intType = s.toString();
					break;
				}
			}
		}

		if (intType == null || equal(intType, BINDS))
		{
			tc.addTerm("Interaction type is " + intType);
			return;
		}

		Set<PhysicalEntity> pAs = getParticipants(get(ext, PARTICIPANT_A));

		if (pAs == null || pAs.isEmpty())
		{
			tc.addTerm("Participant A is null or empty");
			return;
		}

		Map<String, String> fromLoc = getNameIDMap(getString(ext, FROM_LOCATION), getString(ext, FROM_LOCATION_ID));
		PhysicalEntity pB = getParticipant(getMap(ext, PARTICIPANT_B), null, fromLoc);

		if (pB == null)
		{
			tc.addTerm("Participant B is null");
			return;
		}

		PhysicalEntity pBm = null;

		Map<String, String> toLoc = getNameIDMap(getString(ext, TO_LOCATION), getString(ext, TO_LOCATION_ID));

		if (equal(intType, ADDS_MODIFICATION) || equal(intType, REMOVES_MODIFICATION))
		{
			pBm = getParticipant(getMap(ext, PARTICIPANT_B), get(ext, MODIFICATIONS), toLoc);
		}
		else if (equal(intType, ACTIVATES))
		{
			pBm = getParticipant(getMap(ext, PARTICIPANT_B), ACTIVE, toLoc);
		}
		else if (equal(intType, INACTIVATES))
		{
			pBm = getParticipant(getMap(ext, PARTICIPANT_B), INACTIVE, toLoc);
		}
		else if (equal(intType, TRANSLOCATES))
		{
			pBm = getParticipant(getMap(ext, PARTICIPANT_B), null, toLoc);
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

		Interaction inter;

		if (equal(intType, INCREASES) || equal(intType, DECREASES))
		{
			inter = trReg.getTempReac(pBm);
		}
		else
		{
			Class<? extends Conversion> cnvClazz;
			if (equal(intType, TRANSLOCATES)) cnvClazz = Transport.class;
			else cnvClazz = BiochemicalReaction.class;

			inter = cnvReg.getConversion(pB, pBm, cnvClazz);
		}

		Control ctr = ctrReg.getControl(pAs, inter);

		if (equal(intType, DECREASES)) ctr.setControlType(ControlType.INHIBITION);

		// Fix for REACH cards
		if (map.containsKey("pmc_id") && !map.get("pmc_id").toString().startsWith("PMC"))
		{
			ext.put("pmc_id", "PMC" + map.get("pmc_id").toString());
		}

		// Add source article ID
		String source = getString(ext, PUBLICATION_REF);
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

		// Cluster score
		Double score = getDouble(ext, CLUSTER_SCORE);
		if (score != null)
		{
			ctr.addComment("Cluster score: " + score);
		}

		tc.addTerm("Converted successfully");
	}

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

	private PhysicalEntity getParticipant(Map map, Object modifs, Map<String, String> location) throws IOException
	{
		String type = getString(map, ENTITY_TYPE);
		String id = getString(map, IDENTIFIER);

		// fix for Leidos table reading cards
		if (type == null && id != null && id.trim().startsWith("uniprot:")) type = "protein";

		if (type == null || type.equals("unknown")) return null;
		if (id == null) return null;

		String idType = "";
		if (id.contains(":"))
		{
			idType = id.substring(0, id.indexOf(":")).trim();
			id = id.substring(id.indexOf(":") + 1);
			if (id.startsWith("uniprot"))
			{
				System.out.println();
			}
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
			String fType = getString(m, FEATURE_TYPE);

			if (fType == null) continue;

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
		if (o instanceof Map) addToModel((Map) o);
		else
		{
			printClusterScoreDistribution((List) o);

			Progress p = watchProgress ? new Progress(((List) o).size(), "Processing cards") : null;
			for (Object oo : (List) o)
			{
				addToModel((Map) oo);
				if (watchProgress) p.tick();
			}
		}
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
		CardToBioPAX c = new CardToBioPAX();
		c.covertFolders(true, "/home/babur/Documents/DARPA/BigMech/cards-clustering");
		Interpro.write();
		c.writeModel("/home/babur/Documents/DARPA/BigMech/L.owl");

		System.out.println("ProteinRepository.mappedUniprot.size() = " + ProteinRepository.mappedUniprot.size());
		System.out.println("ProteinRepository.unmappedUniprot.size() = " + ProteinRepository.unmappedUniprot.size());
		System.out.println(new ArrayList<>(ProteinRepository.unmappedUniprot));
		c.tc.print();
	}
}
