package org.pathwaycommons.pathwaycards.convertor;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.pattern.miner.*;
import org.cbio.causality.util.FileUtil;
import org.cbio.causality.util.Histogram;
import org.cbio.causality.util.Kronometre;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;

/**
 * For converting the BioPAX that is produced from index cards into SIF
 *
 * @author Ozgun Babur
 */
public class BioPAXToSIF
{
	public static void main(String[] args) throws IOException
	{
		Kronometre kron = new Kronometre();

		String base = "/media/babur/6TB1/REACH-cards/";
		SimpleIOHandler io = new SimpleIOHandler();
		Model model = io.convertFromOWL(new FileInputStream(base + "temp.owl"));
		String sifFilename = base + "temp.sif";
		convert(model, sifFilename);

//		printScoreDistribution(sifFilename);
//		extractAboveScore(sifFilename, base + "L0.1.sif", 0.1);

		kron.stop();
		kron.print();
	}

	public static void convert(Model model, String outFilename) throws FileNotFoundException
	{
		SIFSearcher searcher = new SIFSearcher(new Fetcher(), SIFEnum.CONTROLS_STATE_CHANGE_OF,
			SIFEnum.CONTROLS_EXPRESSION_OF, SIFEnum.IN_COMPLEX_WITH, SIFEnum.INTERACTS_WITH);

		SIFToText stt = new CustomFormat(OutputColumn.Type.PMC.toString(), OutputColumn.Type.COMMENTS.toString());

		searcher.searchSIF(model, new FileOutputStream(outFilename), stt);
	}

	static class Fetcher implements IDFetcher
	{

		@Override
		public Set<String> fetchID(BioPAXElement ele)
		{
			if (ele instanceof XReferrable)
			{
				for (Xref xref : ((XReferrable) ele).getXref())
				{
					if (xref.getDb().equals("HGNC Symbol")) return Collections.singleton(xref.getId());
				}
			}
			return Collections.emptySet();
		}
	}

	private static void printScoreDistribution(String sifFilename) throws IOException
	{
		Histogram h = new Histogram(0.01);
		h.setBorderAtZero(true);
		Scanner sc = new Scanner(new File(sifFilename));
		while (sc.hasNextLine())
		{
			String[] token = sc.nextLine().split("\t");
			if (token.length < 5) continue;

			double score = 0;
			for (String s : token[4].split(";"))
			{
				s = s.substring(s.lastIndexOf(" ") + 1);
				double x = Double.parseDouble(s);
				if (x > score) score = x;
			}
			h.count(score);
		}

		h.print();
	}

	private static void extractAboveScore(String sifFilename, String outFile, double scoreThr) throws IOException
	{
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
		Scanner sc = new Scanner(new File(sifFilename));
		while (sc.hasNextLine())
		{
			String line = sc.nextLine();
			String[] token = line.split("\t");

			double max = 0;
			if (token.length > 4)
			{
				String[] ss = token[4].split(";");
				for (String s : ss)
				{
					double x = Double.parseDouble(s.substring(s.lastIndexOf(" ") + 1));
					if (x > max) max = x;
				}
			}
			if (max >= scoreThr) writer.write(line + "\n");
		}
		writer.close();
	}

	private static void extractLinesWithMultipleReferences(String sifFilename, String outFile, int thr) throws IOException
	{
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile));
		Scanner sc = new Scanner(new File(sifFilename));
		while (sc.hasNextLine())
		{
			String line = sc.nextLine();
			String[] token = line.split("\t");
			if (token[3].split(";").length >= thr) writer.write(line + "\n");
		}
		writer.close();
	}


}
