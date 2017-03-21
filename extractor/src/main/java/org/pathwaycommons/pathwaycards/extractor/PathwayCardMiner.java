package org.pathwaycommons.pathwaycards.extractor;

import com.github.jsonldjava.utils.JsonUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.pattern.Match;
import org.biopax.paxtools.pattern.Pattern;
import org.biopax.paxtools.pattern.Searcher;
import org.biopax.paxtools.pattern.miner.AbstractSIFMiner;
import org.biopax.paxtools.pattern.miner.SIFEnum;
import org.biopax.paxtools.pattern.util.Blacklist;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author Ozgun Babur
 */
public class PathwayCardMiner
{
	/**
	 * JSON cards
	 */
	List cards = new ArrayList();

	/**
	 * Miners used for generating cards.
	 */
	private AbstractMiner[] miners = new AbstractMiner[]{
		new CSCO(), new CSCO_ButPart(), new CSCO_ThrContSmMol(),
		new BindingMiner(),
		new ExpressionMiner()};

	public static void main(String[] args) throws IOException
	{
		// This is for avoiding ubiquitous small molecules like ATP
		Blacklist black = new Blacklist(new URL(
			"http://www.pathwaycommons.org/pc2/downloads/blacklist.txt").openStream());

		PathwayCardMiner mcm = new PathwayCardMiner();
		mcm.setBlacklist(black);

		SimpleIOHandler io = new SimpleIOHandler();

		Model model = io.convertFromOWL(new GZIPInputStream(new URL(
			"http://www.pathwaycommons.org/pc2/downloads/Pathway%20Commons.7.All.BIOPAX.owl.gz").
			openStream()));
//		Model model = io.convertFromOWL(new FileInputStream("/home/ozgun/Downloads/EGFR-downregulation.owl"));
//		Model model = io.convertFromOWL(new FileInputStream("/home/demir/Documents/Ras-2-neighborhood.owl"));

		mcm.mineAndCollect(model);
		mcm.addIDs();
		mcm.printGroundingStats();
		mcm.writeResults("/home/ozgun/Documents/Darpa/BigMech/evaluation/PC.json");
//		mcm.writeResults("/home/demir/Documents/PCall.json");
	}

	abstract class AbstractMiner extends AbstractSIFMiner
	{
		public AbstractMiner()
		{
			super(SIFEnum.CONTROLS_STATE_CHANGE_OF);
		}

		@Override
		public abstract Pattern constructPattern();

		@Override
		public void writeResult(Map<BioPAXElement, List<Match>> matches, OutputStream out) throws IOException
		{
			for (List<Match> matchList : matches.values())
			{
				for (Match m : matchList)
				{
					PhysicalEntity source = (PhysicalEntity) m.get(getSourceLabel(), getPattern());
					EntityReference target = (EntityReference) m.get(getTargetLabel(), getPattern());

					// collect gained and lost modifications and cellular locations of the target

					Set<String>[] modif = getDeltaModifications(m,
						getInputSimplePELabel(), getInputComplexPELabel(),
						getOutputSimplePELabel(), getOutputComplexPELabel());

					Set<String>[] comps = getDeltaCompartments(m,
						getInputSimplePELabel(), getInputComplexPELabel(),
						getOutputSimplePELabel(), getOutputComplexPELabel());

					// correct for inactive-labelled controllers and negative sign controls
					int sign = sign(m, getControlLabels());

					Set<String> modif0 = modif[sign == -1 ? 1 : 0];
					Set<String> modif1 = modif[sign == -1 ? 0 : 1];
					Set<String> comps0 = comps[sign == -1 ? 1 : 0];
					Set<String> comps1 = comps[sign == -1 ? 0 : 1];

					List<BioPAXElement> meds = m.get(getMediatorLabels(), getPattern());

					int actChSign = getActivityChangeSign(modif0, modif1);
					if (actChSign != 0)
					{
						prepareActivityChangeCard(source, target,
							(actChSign == 1 ? "inc" : "dec") + "reases_activity", meds);
					}

					if (!modif0.isEmpty())
					{
						cards.add(prepareModificationCard(source, target, "adds_modification", modif0, meds));
					}
					if (!modif1.isEmpty())
					{
						cards.add(prepareModificationCard(source, target, "removes_modification", modif1, meds));
					}

					if (!comps0.isEmpty() || !comps1.isEmpty())
					{
						cards.add(prepareTranslocationCard(source, target, "translocation", comps1, comps0, meds));
					}
				}
			}
		}

		private int getActivityChangeSign(Set<String> mods0, Set<String> mods1)
		{
			boolean act0 = mods0.remove("residue modification, active");
			boolean act1 = mods1.remove("residue modification, active");
			boolean inh0 = mods0.remove("residue modification, inactive");
			boolean inh1 = mods1.remove("residue modification, inactive");

			if (act0 && !act1) return 1;
			if (act1 && !act0) return -1;
			if (inh0 && !inh1) return -1;
			if (inh1 && !inh0) return 1;
			return 0;
		}

		private Map prepareModificationCard(PhysicalEntity source, EntityReference target, String type,
			Set<String> mods, List<BioPAXElement> mediators)
		{
			Map map = new LinkedHashMap();
			Map extractedInfo = fillBasic(source, target, type, mediators, map);
			List modList = new ArrayList();
			extractedInfo.put("modifications", modList);
			for (String mod : mods)
			{
				String site = null;
				if (mod.contains("@"))
				{
					site = mod.substring(mod.indexOf("@") + 2 );
					mod = mod.substring(0, mod.indexOf("@"));
				}
				mod = FieldReaderUtil.mapModificationTerm(mod);
				Map modMap = new LinkedHashMap();
				modList.add(modMap);
				modMap.put("modification_type", mod);
				if (site != null) modMap.put("position", site);
			}

			addEvidence(mediators, map);

			return map;
		}

		private Map prepareTranslocationCard(PhysicalEntity source, EntityReference target, String type,
			Set<String> from, Set<String> to, List<BioPAXElement> mediators)
		{
			Map map = new LinkedHashMap();
			fillBasic(source, target, type, mediators, map);

			if (!from.isEmpty())
			{
				if (from.size() == 1) map.put("from_location", from.iterator().next());
				else map.put("from_location", new ArrayList(from));
			}
			if (!to.isEmpty())
			{
				if (to.size() == 1) map.put("to_location", to.iterator().next());
				else map.put("to_location", new ArrayList(to));
			}

			addEvidence(mediators, map);
			return map;
		}

		private Map prepareActivityChangeCard(PhysicalEntity source, EntityReference target, String type,
			List<BioPAXElement> mediators)
		{
			Map map = new LinkedHashMap();
			fillBasic(source, target, type, mediators, map);
			addEvidence(mediators, map);
			return map;
		}

		protected Map prepareBindingCard(PhysicalEntity pe1, PhysicalEntity pe2,
			List<BioPAXElement> mediators)
		{
			Map map = new LinkedHashMap();
			fillBasic(pe1, pe2, "binds", mediators, map);
			addEvidence(mediators, map);
			return map;
		}

		protected Map prepareExpressionCard(PhysicalEntity source, EntityReference target,
			String type, List<BioPAXElement> mediators)
		{
			Map map = new LinkedHashMap();
			fillBasic(source, target, type, mediators, map);
			addEvidence(mediators, map);
			return map;
		}

		private void addEvidence(List<BioPAXElement> mediators, Map map)
		{
			ArrayList evList = new ArrayList();
			map.put("evidence", evList);
			for (BioPAXElement mediator : mediators)
			{
				if (mediator instanceof Interaction)
				{
					for (String comment : ((Interaction) mediator).getComment())
					{
						if (comment.startsWith("REPLACED")) continue;
						if (comment.contains("@Layout@")) continue;
						evList.add(comment);
					}
				}
			}
		}

		private Map fillBasic(PhysicalEntity source, BioPAXElement target, String type, List<BioPAXElement> mediators, Map map)
		{
			List eles = new ArrayList();
			map.put("model_elements", eles);
			for (BioPAXElement mediator : mediators)
			{
				eles.add(mediator.getUri());
			}
			Map extractedInfo = new LinkedHashMap();
			map.put("extracted_information", extractedInfo);
			extractedInfo.put("participant_a", FieldReaderUtil.convertToJASON(source));
			if (target instanceof PhysicalEntity)
				extractedInfo.put("participant_b", FieldReaderUtil.convertToJASON((PhysicalEntity) target));
			else
				extractedInfo.put("participant_b", FieldReaderUtil.convertToJASON((EntityReference) target));
			extractedInfo.put("interaction_type", type);
			return extractedInfo;
		}

		String getInputSimplePELabel()
		{
			return "input simple PE";
		}

		String getOutputSimplePELabel()
		{
			return "output simple PE";
		}

		String getInputComplexPELabel()
		{
			return "input PE";
		}

		String getOutputComplexPELabel()
		{
			return "output PE";
		}

		@Override
		public String getSourceLabel()
		{
			return "controller PE";
		}

		@Override
		public String getTargetLabel()
		{
			return "changed ER";
		}

		@Override
		public String[] getMediatorLabels()
		{
			return new String[]{"Control", "Conversion"};
		}

		public String[] getControlLabels()
		{
			return new String[]{"Control"};
		}
	}

	class CSCO extends AbstractMiner
	{
		@Override
		public Pattern constructPattern()
		{
			return PatternGenerator.controlsStateChange();
		}
	}

	class CSCO_ButPart extends AbstractMiner
	{
		@Override
		public Pattern constructPattern()
		{
			return PatternGenerator.controlsStateChangeButIsParticipant();
		}

		@Override
		public String[] getControlLabels()
		{
			return new String[]{};
		}

		@Override
		public String[] getMediatorLabels()
		{
			return new String[]{"Conversion"};
		}
	}

	class CSCO_ThrContSmMol extends AbstractMiner
	{
		@Override
		public Pattern constructPattern()
		{
			return PatternGenerator.controlsStateChangeThroughControllerSmallMolecule(blacklist);
		}

		@Override
		public String getSourceLabel()
		{
			return "upper controller PE";
		}

		@Override
		public String[] getMediatorLabels()
		{
			return new String[]{"upper Control", "upper Conversion", "Control", "Conversion"};
		}

		@Override
		public String[] getControlLabels()
		{
			return new String[]{"upper Control", "Control"};
		}
	}

	class BindingMiner extends AbstractMiner
	{
		@Override
		public Pattern constructPattern()
		{
			return PatternGenerator.bindingPattern();
		}

		@Override
		public void writeResult(Map<BioPAXElement, List<Match>> matches, OutputStream out) throws IOException
		{
			for (List<Match> matchList : matches.values())
			{
				for (Match match : matchList)
				{
					PhysicalEntity pe1 = (PhysicalEntity) match.get("PE1", getPattern());
					PhysicalEntity pe2 = (PhysicalEntity) match.get("PE2", getPattern());
					List<BioPAXElement> meds = match.get(getMediatorLabels(), getPattern());

					Map card = prepareBindingCard(pe1, pe2, meds);
					cards.add(card);
				}
			}
		}

		@Override
		public String[] getMediatorLabels()
		{
			return new String[]{"Conversion"};
		}
	}

	class ExpressionMiner extends AbstractMiner
	{
		@Override
		public Pattern constructPattern()
		{
			return PatternGenerator.expressionPattern();
		}

		@Override
		public void writeResult(Map<BioPAXElement, List<Match>> matches, OutputStream out) throws IOException
		{
			for (List<Match> matchList : matches.values())
			{
				for (Match match : matchList)
				{
					PhysicalEntity source = (PhysicalEntity) match.get("TF PE", getPattern());
					EntityReference target = (EntityReference) match.get("product ER", getPattern());
					List<BioPAXElement> meds = match.get(getMediatorLabels(), getPattern());
					int sign = sign(match, getControlLabels());
					Map card = prepareExpressionCard(source, target, (sign < 0 ? "de" : "in") + "creases", meds);
					cards.add(card);
				}
			}
		}

		@Override
		public String[] getMediatorLabels()
		{
			return new String[]{"TempReac"};
		}
	}

	public void setBlacklist(Blacklist blacklist)
	{
		for (AbstractMiner miner : miners)
		{
			miner.setBlacklist(blacklist);
		}
	}

	public void mineAndCollect(Model model)
	{
		for (AbstractMiner miner : miners)
		{
			Map<BioPAXElement, List<Match>> matches = Searcher.search(model, miner.getPattern());

			try { miner.writeResult(matches, null);
			} catch (IOException e){e.printStackTrace();}
		}
	}

	public void addIDs()
	{
		int i = 1;
		for (Object obj : cards)
		{
			Map card = (Map) obj;
			card.put("card_id", "" + i++);
		}
	}

	public void writeResults(String filename) throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		JsonUtils.writePrettyPrint(writer, cards);
		writer.close();
	}

	public void printGroundingStats()
	{
		int ungroundCnt = 0;
		for (Object card : cards)
		{
			Map map = (Map) ((Map) card).get("extracted_information");
			boolean gA = map.containsKey("participant_a") &&
				FieldReaderUtil.isGrounded(map.get("participant_a"));
			boolean gB = map.containsKey("participant_b") &&
				FieldReaderUtil.isGrounded(map.get("participant_b"));

			if (!gA || !gB)
				ungroundCnt++;
		}

		System.out.println("card size  = " + cards.size());
		System.out.println("ungrounded = " + ungroundCnt);
		System.out.println("Grounded physical entities = " + FieldReaderUtil.grounded.size());
		System.out.println("Ungrounded physical entities = " + FieldReaderUtil.ungrounded.size());
	}
}
