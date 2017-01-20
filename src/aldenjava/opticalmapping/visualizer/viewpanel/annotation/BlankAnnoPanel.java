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


package aldenjava.opticalmapping.visualizer.viewpanel.annotation;

import java.awt.FlowLayout;

import javax.swing.JLabel;

import aldenjava.opticalmapping.visualizer.OMView;

public class BlankAnnoPanel extends AnnotationPanel {

	public BlankAnnoPanel(OMView mainView) {
		super(mainView);
		this.setLayout(new FlowLayout());
		this.add(new JLabel("Blank Annotation"));
	}

	@Override
	public void autoSetSize() {
		if (region == null)
			setSize(OMView.blankPanelSize);
		else
			setSize((int) (region.length() / dnaRatio * ratio + objBorder.x * 2), mainView.getHeight());
		setPreferredSize(getSize());
	}

	@Override
	public void reorganize() {
		
	}

}
