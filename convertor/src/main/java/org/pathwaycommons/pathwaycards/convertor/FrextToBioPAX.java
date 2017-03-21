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

import static org.pathwaycommons.pathwaycards.convertor.FrexTag.*;

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
	 * Repository for Bio-processes.
	 */
	BioprocessRepository biopRep;

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
	ComplexRepository complexRep;

	/**
	 * Registry for processed events.
	 */
	EventRegistry eventReg;

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
		complexRep = new ComplexRepository();
		famRep = new FamilyRepository(protRep, complexRep);
		biopRep = new BioprocessRepository();
		biopRep.setProtRep(protRep);
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
		complexRep.setModel(model);
		biopRep.setModel(model);
		modRep.setModel(model);
		locRep.setModel(model);
		cnvReg = new ConversionRegistry();
		cnvReg.setModel(model);
		ctrReg = new ControlRegistry();
		ctrReg.setModel(model);
		trReg = new TemplateReactionRegistry();
		trReg.setModel(model);
		eventReg = new EventRegistry();
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

		// Prepare the xref pointing to the resource paper
		String pmcID = PUBLICATION_REF.getString(map);
		PublicationXref xref = factory.create(PublicationXref.class, "PublicationXref/" + NextNumber.get());
		xref.setDb("PMC International");
		xref.setId(pmcID);
		model.add(xref);

		// Iterate events and add to the model
		for (Object o : EVENTS.getList(map))
		{
			Map event = (Map) o;

//			//fix for translocations
			if (PARTICIPANT_A.has(event) && !PARTICIPANT_B.has(event))
			{
				event.put(PARTICIPANT_B.getTag(), PARTICIPANT_A.get(event));
				PARTICIPANT_A.remove(event);

				System.out.println("Moved participant A to B. " + pmcID);
			}

			processEvent(event, xref);
		}
	}

	/**
	 * Recursive method that creates each event.
	 */
	private void processEvent(Map event, final Xref xref)
	{
		Map predicate = PREDICATE.getMap(event);
		String predID = PREDICATE_ID.getString(predicate);

		// Stop if this event is already processed
		if (eventReg.isRegistered(predID)) return;

		// If the event is a "not happened" event, just skip it
		if (SIGN.equal(predicate, NEGATIVE))
		{
			tc.addTerm("Not happened");
			return;
		}

		String predType = PREDICATE_TYPE.getString(predicate);
		String predSubType = PREDICATE_SUB_TYPE.getString(predicate);

//--- DEBUG

//		lookAtKeys(event, "");

			tc.addTerm(predType + " ---- " + predSubType);

//			if (event.containsKey("participant_a"))
//			{
//				String term = ((Map) event.get("participant_a")).get("entity_type").toString();
//				tc.addTerm(term);
//			}

//		if (event.containsKey("sites"))
//		{
//			((List) event.get("sites")).stream().map(o -> (Map) o).forEach(m -> tc.addTerm(((Map) m).get("entity_type").toString()));
//		}

//		if (true) return;
//--- DEBUG

		// Skip if interaction type is not set
		if (predType == null)
		{
			tc.addTerm("Interaction type is " + predType);
			return;
		}

		// Process participant As

		Set<PhysicalEntity> pAs = new HashSet<>();

		Object partAObj = PARTICIPANT_A.get(event);
		if (partAObj != null)
		{
			if (partAObj instanceof List)
			{
				for (Object o : (List) partAObj)
				{
					processPartA((Map) o, xref, pAs);
				}
			}
			else processPartA((Map) partAObj, xref, pAs);
		}

		// Read the "from" location of participant B
		Map fromLoc = FROM_LOCATION.getMapOrFirstInList(event);

		List<Interaction> interactions = new ArrayList<>();

		Object partBObj = PARTICIPANT_B.get(event);

		if (partBObj == null)
			return;
//			throw new RuntimeException("Participant B is not present! " + xref.getId());

		// Read the "to" location of participant B
		Map toLoc = TO_LOCATION.getMapOrFirstInList(event);

		List<Map> modifs = null;
		if (PROTEIN_MODIFICATION.equal(predType))
		{
			modifs = new ArrayList<>();
			Map<String, Object> map = new HashMap<>();
			map.put(MODIFICATION_TYPE.getTag(), predSubType);
			List sites = SITES.getList(event);
			if (sites != null) map.put(SITES.getTag(), sites);
			modifs.add(map);
		}

		Set<PhysicalEntity> pBs = new HashSet<>();

		if (partBObj instanceof List)
		{
			processPartBList((List) partBObj, event, xref, predID, predType, predSubType, pAs, fromLoc, toLoc, modifs,
				interactions, pBs);
		}
		else
		{
			processPartBMap((Map) partBObj, event, xref, predID, predType, predSubType, pAs, fromLoc, toLoc, modifs,
				interactions, pBs);
		}

		if (BINDS.equal(predType))
		{
			Set<PhysicalEntity> set = new HashSet<>(pAs);
			set.addAll(pBs);

			if (set.size() > 1)
			{
				Complex complex = complexRep.getComplex(set);
				Interaction inter = cnvReg.getConversion(set, Collections.singleton(complex), ComplexAssembly.class);
				eventReg.register(predID, inter, complex);
			}
		}

		if (!pAs.isEmpty() && !BINDS.equal(predType))
		{
			Interaction control = null;

			// generate a complex if there is more than one participant A
			Complex complex = null;
			if (pAs.size() > 1)
			{
				complex = complexRep.getComplex(pAs);
			}

			for (Interaction inter : interactions)
			{
				control = ctrReg.getControl(complex == null ? pAs : Collections.singleton(complex), inter,
					!(predSubType != null && predSubType.startsWith("negative")));

				control.addXref(xref);
				control.addComment(SENTENCE.getString(event));

				tc.addTerm("Converted successfully");
			}

			eventReg.register(predID, control, eventReg.getTheProduct(predID));
		}
	}

	private void processPartBList(List partBObj, Map event, Xref xref, String predID, String predType, String predSubType, Set<PhysicalEntity> pAs, Map fromLoc, Map toLoc, List<Map> modifs, List<Interaction> interactions, Set<PhysicalEntity> pBs)
	{
		for (Object o : partBObj)
		{
			processPartBMap((Map) o, event, xref, predID, predType, predSubType, pAs, fromLoc, toLoc, modifs, interactions, pBs);
		}
	}

	private void processPartBMap(Map partBObj, Map event, Xref xref, String predID, String predType, String predSubType, Set<PhysicalEntity> pAs, Map fromLoc, Map toLoc, List<Map> modifs, List<Interaction> interactions, Set<PhysicalEntity> pBs)
	{
		if (isEvent(partBObj))
		{
			processEvent(partBObj, xref);
			Interaction inter = eventReg.getTheInteraction(getPredicateIDOfEvent(partBObj));
			if (inter != null) interactions.add(inter);
		}
		else
		{
			processPartB(partBObj, event, xref, predID, predType, predSubType, pAs, fromLoc, toLoc, modifs,
				interactions, pBs);
		}
	}

	private void processPartB(Map partBMap, Map event, Xref xref, String predID, String predType, String predSubType, Set<PhysicalEntity> pAs, Map fromLoc, Map toLoc, List<Map> modifs, List<Interaction> interactions, Set<PhysicalEntity> pBs)
	{
		// Generate participant B
		PhysicalEntity pB = getParticipant(partBMap, null, fromLoc);

		// Skip if no participant B is found
		if (pB == null)
		{
			tc.addTerm("Participant B is null");
			return;
		}

		pBs.add(pB);
		PhysicalEntity pBm = null;


		if (PROTEIN_MODIFICATION.equal(predType) || TRANSLOCATION.equal(predType))
		{
			pBm = getParticipant(partBMap, modifs, toLoc);
		}

		else if (ACTIVATION.equal(predType))
		{
			pBm = getParticipant(partBMap, Collections.singletonList(Constants.ACTIVE), toLoc);
		}
		else if (BINDS.equal(predType) || REGULATION.equal(predType))
		{
			// no pBm needed
		}
		else
		{
			System.err.println("Unhandled interaction type = " + predType);
		}

		// Swap modified participant B if the event is a removal of modification
		if (isRemovalOfModification(predSubType))
		{
			PhysicalEntity temp = pB;
			pB = pBm;
			pBm = temp;
		}

		// Create the event

		Interaction inter = null;

		// If it is a binding event, skip it, it will be handles in upper methods.
		if (BINDS.equal(predType))
		{
			// do nothing
		}
		// Otherwise it is a child of Conversion
		else if (!REGULATION.equal(predType))
		{
			Class<? extends Conversion> cnvClazz;

			// If it is a translocation, create a Transport event
			if (TRANSLOCATION.equal(predType))
			{
				cnvClazz = Transport.class;
			}
			// Otherwise, it is a BiochemicalReaction
			else
			{
				cnvClazz = BiochemicalReaction.class;
			}

			// create the Conversion
			inter = cnvReg.getConversion(Collections.singleton(pB), Collections.singleton(pBm), cnvClazz);
			eventReg.register(predID, inter, pBm);
		}

		if (inter != null)
		{
			interactions.add(inter);
			inter.addXref(xref);
			inter.addComment(SENTENCE.getString(event));
		}
	}

	private boolean isRemovalOfModification(String predSubType)
	{
		return predSubType != null && predSubType.startsWith("de");
	}

	private String getNormalizedModType(String predSubType)
	{
		predSubType = predSubType.trim().toLowerCase();
		if (predSubType.startsWith("de")) predSubType = predSubType.substring(2);
		if (predSubType.startsWith("auto")) predSubType = predSubType.substring(4);
		if (predSubType.endsWith("tion")) predSubType = predSubType.substring(0, predSubType.length() - 3) + "ed";
		return predSubType;
	}

	private void processPartA(Map map, Xref xref, Set<PhysicalEntity> pAs)
	{
		if (isEvent(map))
		{
			processEvent(map, xref);
			String id = getPredicateIDOfEvent(map);
			PhysicalEntity product = eventReg.getTheProduct(id);
			if (product != null)
			{
				pAs.add(product);
			}
		}
		else
		{
			PhysicalEntity pe = getParticipant(map, null, null);
			if (pe != null) pAs.add(pe);
		}
	}

	private List getModifications(Map mapWithSites, String modifText)
	{
		String modification = getNormalizedModType(modifText);
		List list = new ArrayList<>();

		List sites = SITES.getList(mapWithSites);

		if (sites == null || sites.isEmpty())
		{
			Map<String, String> map = initModifMap(modification);
			list.add(map);
		}
		else
		{
			for (Object o : sites)
			{
				Map site = (Map) o;

				Map<String, String> map = initModifMap(modification);

				Integer pos = POSITION.getInt(site);
				if (pos != null) map.put(POSITION.getTag(), pos.toString());
				String aa = AA_CODE.getString(site);
				if (AA_MAP.containsKey(aa)) aa = AA_MAP.get(aa);
				if (aa != null) map.put(AA_CODE.getTag(), aa);
				list.add(map);
			}
		}
		return list;
	}

	private Map<String, String> initModifMap(String modification)
	{
		Map<String, String> map = new HashMap<>();
		map.put(MODIFICATION_TYPE.getTag(), modification);
		return map;
	}

	private boolean isEvent(Map map)
	{
		return PREDICATE.has(map);
	}

	private String getPredicateIDOfEvent(Map map)
	{
		Map predMap = PREDICATE.getMap(map);
		if (predMap != null)
		{
			return PREDICATE_ID.getString(predMap);
		}
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
	 * Generates the PhysicalEntity.
	 */
	private PhysicalEntity getParticipant(Map map, List modifs, Map location)
	{
		if (map == null) return null;

		String type = ENTITY_TYPE.getString(map);
		String id = IDENTIFIER.getString(map);

		if (type == null || type.equals("unknown")) return null;
		if (id == null) return null;

		IDType idType = null;
		if (id.contains(":"))
		{
			idType = IDType.get(id.substring(0, id.indexOf(":")).trim());
			id = id.substring(id.indexOf(":") + 1);
		}

		if (idType == null) return null;

		String name = ENTITY_TEXT.getString(map);

		List feats = MODIFICATIONS.getList(map);
		State st = feats == null && modifs == null && location == null ? Constants.UNMODIFIED : new State();

		addFeaturesToState(st, feats);
		addFeaturesToState(st, modifs);
		addLocationToState(st, location);

		// Decide the type of the entity by idType. If fails, then use type.

		if (idType != IDType.UAZ)
		{
			if (idType.isProtein)
			{
				return protRep.getProtein(id, idType, name, st);
			} else if (idType.isChemical)
			{
				return chemRep.getChemical(id, idType, name, st);
			} else if (idType.isFamily)
			{
				return famRep.getFamily(id, idType, name, st);
			} else if (idType.isProcess)
			{
				return biopRep.getBioprocess(id, idType, name, st);
			}
		}
		else if (PROTEIN.equal(type))
		{
			return protRep.getProtein(id, idType, name, st);
		}
		else if (CHEMICAL.equal(type))
		{
			return chemRep.getChemical(id, idType, name, st);
		}
		else if (FAMILY.equal(type))
		{
			return famRep.getFamily(id, idType, name, st);
		}
		else if (BIOPROCESS.equal(type))
		{
			return biopRep.getBioprocess(id, idType, name, st);
		}
		return null;
	}

	private void addFeaturesToState(State st, List feats)
	{
		if (feats == null) return;

		for (Object o : feats)
		{
			Map m = (Map) o;
			String mType = MODIFICATION_TYPE.getString(m);
			if (mType == null) continue;

			List mods = getModifications(m, mType);

			for (Object mod : mods)
			{
				String type = MODIFICATION_TYPE.getString((Map) mod);
				Integer pos = POSITION.getInt((Map) mod);
				String aa = AA_CODE.getString((Map) mod);

				st.addModification(type, aa, pos);
			}
		}
	}

	private void addLocationToState(State st, Map map)
	{
		if (map == null) return;
		String name = ENTITY_TEXT.getString(map);
		String id = IDENTIFIER.getString(map);
		if (name == null) name = id;
		if (id == null) id = name;
		st.compartmentText = name;
		st.compartmentID = id;
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

			boolean hasJSON = files != null &&
				Arrays.stream(files).filter(f -> f.getName().endsWith(".json")).findAny().isPresent();

			Progress p = watchProgress && hasJSON ? new Progress(files.length, "Processing directory: " + dir) : null;

			for (File f : files)
			{
				if (watchProgress && hasJSON) p.tick();
				if (f.isDirectory()) covertFolders(!hasJSON, f.getPath());
				else if (f.getName().endsWith(".json"))
				{
					Object o = JsonUtils.fromInputStream(new FileInputStream(f.getPath()));
					addToModel((Map) o);
				}

//				if (Math.random() < 0.1) break;
			}
		}
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
