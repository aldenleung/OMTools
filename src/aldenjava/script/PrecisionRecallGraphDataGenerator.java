/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.2 -- January 1, 2017
**  
**  Copyright (C) 2017 by Alden Leung, Ting-Fung Chan, All rights reserved.
**  Contact:  alden.leung@gmail.com, tf.chan@cuhk.edu.hk
**  Organization:  School of Life Sciences, The Chinese University of Hong Kong,
**                 Shatin, NT, Hong Kong SAR
**  
**  This file is part of OMTools.
**  
**  OMTools is free software; you can redistribute it and/or 
**  modify it under the terms of the GNU General Public License 
**  as published by the Free Software Foundation; either version 
**  3 of the License, or (at your option) any later version.
**  
**  OMTools is distributed in the hope that it will be useful,
**  but WITHOUT ANY WARRANTY; without even the implied warranty of
**  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**  GNU General Public License for more details.
**  
**  You should have received a copy of the GNU General Public 
**  License along with OMTools; if not, see 
**  <http://www.gnu.org/licenses/>.
**************************************************************************/


package aldenjava.script;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.lang.StringUtils;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.mapper.clustermodule.ClusteredResult;
import aldenjava.opticalmapping.mapper.clustermodule.ResultClusterModule;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

public class PrecisionRecallGraphDataGenerator {
	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(PrecisionRecallGraphDataGenerator.class.getSimpleName(), "Generates a data table for precision recall graphs. This module assumes one alignment (it can contain multiple partial alignments) per one query. You need to use the same alignment joining module parameters if the alignment file is generated by OMTools mapper. If you are using other alignment tools, set alignmentjoinmode as 0. ");
		
		ReferenceReader.assignOptions(parser, 1);
		OptMapDataReader.assignOptions(parser, 1);
		OptMapResultReader.assignOptions(parser, 1);
		ResultClusterModule.assignOptions(parser, 1);
		parser.addHeader("Precision Recall Graph Options", 1);
		OptionSpec<String> orocout = parser.accepts("prgout", "Precision recall graph table output").withRequiredArg().ofType(String.class).required();
		OptionSpec<Boolean> ocheckStrand = parser.accepts("checkstrand", "Checking strand for correctness").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		OptionSpec<String> osortStrategy = parser.accepts("sortstrat", "Sort by \"score\" or \"confidence\"").withRequiredArg().ofType(String.class).defaultsTo("score");
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		
		LinkedHashMap<String, DataNode> optrefmap = ReferenceReader.readAllData(options);
		
		List<String> optmapins = null;
		if (options.has("optmapin")) {
			optmapins = (List<String>) options.valuesOf("optmapin");
		}
		List<String> optresins = (List<String>) options.valuesOf("optresin");
		if (optmapins != null && optresins.size() != optmapins.size()) {
			System.err.println("Wrong size maching between molecule and result files");
			return;
		}
		BufferedWriter rocw = null;
		if (options.has(orocout))
			rocw = new BufferedWriter(new FileWriter(options.valueOf(orocout)));
		
		boolean checkStrand = options.valueOf(ocheckStrand);
		String sortStrategy = options.valueOf(osortStrategy);
		if (!(sortStrategy.equalsIgnoreCase("score") || sortStrategy.equalsIgnoreCase("confidence")))
			throw new IllegalArgumentException("You can only sort by \"score\" or \"confidence\".");
		rocw.write("Score\tConfidence\tCorrect\n");
		for (int i = 0; i < optresins.size(); i++) {
			LinkedHashMap<String, DataNode> fragmentInfo = null;
			if (optmapins != null)
				fragmentInfo = OptMapDataReader.readAllData(optmapins.get(i));
			OptMapResultReader omrr = new OptMapResultReader(optresins.get(i));
			List<SimpleResult> srList = new ArrayList<SimpleResult>();
			List<OptMapResultNode> resultlist;
			while ((resultlist = omrr.readNextList()) != null)
				if (resultlist.get(0).isUsed()) {
					if (resultlist.size() > 0) {
						double score;
						double confidence;
						if (resultlist.size() > 1) {
							ResultClusterModule rcm = new ResultClusterModule(optrefmap);
							rcm.setMode(options);
							rcm.setParameters(options);
							List<ClusteredResult> crList = rcm.standardcluster(resultlist);
							if (crList.size() > 1) {
								VerbosePrinter.println("Warning: multiple alignments are found in query " + resultlist.get(0).parentFrag.name + ". If you are using mapper in OMTools, make sure you set the same alignment joining parameters as in alignment.");
								continue;
							}
							if (crList.size() == 0) {
								VerbosePrinter.println("Warning: no alignment is found in query " + resultlist.get(0).parentFrag.name + ". If you are using mapper in OMTools, make sure you set the same alignment joining parameters as in alignment.");
								continue;
							}
							score = crList.get(0).score;
							confidence = resultlist.get(0).confidence;	
						} 
						else {
							score = resultlist.get(0).mappedscore;
							confidence = resultlist.get(0).confidence;
						}
						
						boolean correct = false;
						for (OptMapResultNode result : resultlist)
							if (fragmentInfo.get(result.parentFrag.name).hasSimulationInfo()) {
								if (result.correctlyMapped(fragmentInfo.get(result.parentFrag.name).simuInfo))
									correct = true;
								else
									if (!checkStrand) {
										result.mappedstrand *= -1;
										if (result.correctlyMapped(fragmentInfo.get(result.parentFrag.name).simuInfo))
											correct = true;
									}
							}
									
						srList.add(new SimpleResult(score, confidence, correct));
					}
				}
			omrr.close();
			{
				if (sortStrategy.equalsIgnoreCase("score"))
					Collections.sort(srList, SimpleResult.scoreComparator);
				else
					Collections.sort(srList, SimpleResult.confidenceComparator);
				Collections.reverse(srList);
				int totalItems = fragmentInfo.size();
				int recall = 0;
				int fp = 0;
				for (SimpleResult sr : srList) {
					if (sr.correct)
						recall++;
					else
						fp++;
					if (rocw != null)
						rocw.write(String.format("%f" + StringUtils.repeat("\t", 1) + "%f" + "\t" + "%b" + "\n", sr.score, sr.confidence, sr.correct));
				}
			}
		}
		if (rocw != null)
			rocw.close();
	}
}
class SimpleResult {
//	String name
	double score;
	double confidence;
	boolean correct;
	public SimpleResult(double score, double confidence, boolean correct) {
		super();
		this.score = score;
		this.confidence = confidence;
		this.correct = correct;
	}
	public static Comparator<SimpleResult> scoreComparator = new Comparator<SimpleResult>() {
		@Override
		public int compare(SimpleResult s1, SimpleResult s2) {
			return Double.compare(s1.score, s2.score);
		}
		
	};
	
	public static Comparator<SimpleResult> confidenceComparator = new Comparator<SimpleResult>() {
		@Override
		public int compare(SimpleResult s1, SimpleResult s2) {
			return Double.compare(s1.confidence, s2.confidence);
		}
		
	};
	
}
/*	
	
	private double maxmappedfactor = 1.0;
	private LinkedHashMap<String, List<ROCNode>> rocresult;
	public ROCCurveGenerator()
	{
		reset();
	}
	public void reset()
	{
		rocresult = new LinkedHashMap<String, List<ROCNode>>();
	}
	
	public void addNext(String name, List<OptMapResultNode> resultlist)
	{
		List<ROCNode> roclist = new ArrayList<ROCNode>();
		for (int i = 0; i < 100; i += 2)
			roclist.add(parseResult(name, resultlist, i));
		rocresult.put(name, roclist);
	}
	private ROCNode parseResult(String name, List<OptMapResultNode> resultlist, double score)
	{
		int fp = 0;
		int tp = 0;
		int total = 0;
		int totaltp = 0;
		int totalfp = 0;
		for (OptMapResultNode result : resultlist)
		{
			total++;
			if (result.isUsed())
			{
				if (result.mappedscore >= score)
				{
					if (result.correctlyMapped(maxmappedfactor))
//					if (result.correctlyMappedWithoutReference(maxmappedfactor))
						tp++;
					else
						fp++;
				}
				if (result.correctlyMapped(maxmappedfactor))
					totaltp++;
				else
					totalfp++;
				
			}
		}	
		// AWARE!!!
//		total = 15482;
		return new ROCNode(name, tp / (double) total, fp / (double) total);
//		return new ROCNode(name, tp / (double) totaltp, fp / (double) totalfp);
	}
	
	public void outputResult(String filename) throws IOException
	{
		StringBuilder fpline = new StringBuilder();
		int previous = 0;
		List<StringBuilder> tpline = new ArrayList<StringBuilder>();
		for (String key : rocresult.keySet())
		{
			List<ROCNode> roclist = rocresult.get(key);
			StringBuilder line = new StringBuilder();
			line.append(key);
			line.append(StringUtils.repeat("\t", previous));
			for (ROCNode roc : roclist)
			{
				fpline.append("\t" + Double.toString(roc.fpr));
				line.append("\t" + Double.toString(roc.tpr));
				previous++;
			}
			tpline.add(line);	
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		bw.write(fpline.toString() + "\n");
		for (StringBuilder s : tpline)
			bw.write(s.toString() + "\n");
		bw.close();
	}
	
	public static void main (String[] args) throws IllegalArgumentException, IllegalAccessException, IOException
	{
		String folder = "C:\\Users\\Alden\\Desktop\\Projects\\OpticalMapping\\Program\\Mapper\\SelfMapperAnalysis\\";
		String resultlistfile = folder + "resultlist.txt";
//		String resultlistfile = folder + "resultlistRefAligner.txt";		
		String outputfile = folder  + "output.tsv";
		String outputfile2 = folder + "output2.tsv";
//		String fragmentfile = folder + "high\\high.txt";
//		LinkedHashMap<String, FragmentNode> fragmentmap = OptMapDataReader.readAllData(fragmentfile, 6);
//		List<Double> name = new ArrayList<Double>();
//		List<Double> usage = new ArrayList<Double>();
//		List<Double> accuracy = new ArrayList<Double>();
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile2));
		bw.write("Mapper\tAccuracy\tUsage\n");
		ROCCurveGenerator roc = new ROCCurveGenerator();
		List<String> resultlist = ListExtractor.extractList(resultlistfile);
		for (String result : resultlist)
		{
			if (result.contains("refaligner"))
			{
				LinkedHashMap<String, OptMapResultNode> map = (OptMapResultReader.readAllData(folder + result, 1));
				int total = 0;
				int use = 0;
				int mapped = 0;
				for (OptMapResultNode r : map.values())
				{
					total++;
					if (r.isUsed() && r.mappedscore >= 9)
					{
						use++;
						if (r.correctlyMapped(1))
							mapped++;
					}
				}
				if (result.contains("none"))
					total = 15482;
				if (result.contains("low"))
					total = 15490;
				if (result.contains("mid"))
					total = 15491;
				if (result.contains("high"))
					total = 15476;
//				accuracy.add((mapped / (double) use));
//				usage.add(use / (double) total);
				bw.write(String.format("%s\t%.4f\t%.4f\n", result, mapped / (double) use, use / (double) total));
//			List<OptMapResultNode> list = new ArrayList<OptMapResultNode>((OptMapResultReader.readAllData(folder + result, 3)).values());
//			for (OptMapResultNode r : list)
//				r.importFragment(fragmentmap.get(r.id));
			
//			for (FragmentNode f : fragmentmap.values())
//			{
//				if (map.containsKey(f.id))
//					map.get(f.id).importFragment(f);
//				else
//					map.put(f.id, new OptMapResultNode(FragmentMapNode.newDiscardedMapNode(f)));
//			}
				
				roc.addNext(result, new ArrayList<OptMapResultNode>(map.values()));
			}
		}
		bw.close();
		roc.outputResult(outputfile);
		
		
	}
}

class ROCNode
{
	final String name;
	final double tpr;
	final double fpr;

	public ROCNode(String name, double tpr, double fpr) {
		this.name = name;
		this.tpr = tpr;
		this.fpr = fpr;
	}	
}*/