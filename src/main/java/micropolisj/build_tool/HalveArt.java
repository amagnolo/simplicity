// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.build_tool;

import java.io.File;
import javax.imageio.ImageIO;

/**
 * Box-filter downscale by 2 (alpha-weighted). Used by the tile pipeline
 * to derive 8px source sheets from cleaned-up 16px art.
 *
 * Usage: java micropolisj.build_tool.HalveArt &lt;input.png&gt; &lt;output.png&gt;
 */
public class HalveArt
{
	public static void main(String [] args)
		throws Exception
	{
		if (args.length != 2) {
			throw new Exception("Usage: HalveArt <input.png> <output.png>");
		}
		var img = UpscaleArt.toArgb(ImageIO.read(new File(args[0])));
		new File(args[1]).getAbsoluteFile().getParentFile().mkdirs();
		ImageIO.write(ProcArt.halve(img), "png", new File(args[1]));
	}
}
