/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.4 -- March 10, 2018
**  
**  Copyright (C) 2018 by Alden Leung, Ting-Fung Chan, All rights reserved.
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


package aldenjava.opticalmapping.multiplealignment;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;


public class CollinearBlockReader extends OMReader<CollinearBlock> {

	public CollinearBlockReader(String filename) throws IOException {
		super(filename);
	}
	public CollinearBlockReader(InputStream stream) throws IOException {
		super(stream);
	}

	public CollinearBlockReader(OptionSet options) throws IOException {
		this((String) options.valueOf("cblin"));
	}
	
	@Override
	public CollinearBlock read() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\\t", -1);
		String name = l[0];
		String[] ll = l[1].split(";");
		LinkedHashMap<String, BlockInfo> groups = new LinkedHashMap<>();
		for (int i = 0; i < ll.length; i++) {
			if (ll[i].isEmpty())
				continue;
			String[] entry = ll[i].split(":");
			String query = entry[0];
			boolean reverse;
			switch (entry[1].substring(entry[1].length() - 1)) {
				case "+": 
				case "F": 
				case "f":
					reverse = false;
					break;
				case "-": 
				case "R": 
				case "r":
					reverse = true;
					break;
				default:
					throw new IllegalArgumentException("Incorrect group info format: " + entry[1]);
			}
			String[] startstop = entry[1].substring(0, entry[1].length() - 1).split("-");
			int start = Integer.parseInt(startstop[0]);
			int stop = Integer.parseInt(startstop[1]);
			if ((stop > start && reverse) || (start > stop && !reverse))
				throw new IllegalArgumentException("The indicated orientation is opposite to the orientation suggested by the start and stop signals: " + entry[1]);
			groups.put(query, new BlockInfo(start, stop));
		}
		proceedNextLine();
		return new CollinearBlock(name, groups);
	}
	
	public static LinkedHashMap<String, CollinearBlock> readAllData(String filename) throws IOException {
		CollinearBlockReader cbr = new CollinearBlockReader(filename);
		LinkedHashMap<String, CollinearBlock> map = new LinkedHashMap<>();
		CollinearBlock block;
		while ((block = cbr.read()) != null) {
			map.put(block.name, block);
		}
		cbr.close();
		return map;
	}

	public static LinkedHashMap<String, CollinearBlock> readAllData(OptionSet options) throws IOException {
		CollinearBlockReader cbr = new CollinearBlockReader(options);
		LinkedHashMap<String, CollinearBlock> map = new LinkedHashMap<>();
		CollinearBlock block;
		while ((block = cbr.read()) != null) {
			map.put(block.name, block);
		}
		cbr.close();
		return map;		
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Collinear Block Reader Options", level);
		parser.accepts("cblin", "Multiple alignment collinear blocks input.").withRequiredArg().ofType(String.class).required();
	}


}

