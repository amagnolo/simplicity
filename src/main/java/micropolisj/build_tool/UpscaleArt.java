// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.build_tool;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Upscales pixel art by an integer factor (2 or 4) using the
 * Scale2x/EPX algorithm, which smooths diagonal edges while keeping
 * the image crisp. Factor 4 applies Scale2x twice.
 *
 * Usage: java micropolisj.build_tool.UpscaleArt input.png output.png factor
 */
public class UpscaleArt
{
	public static void main(String [] args)
		throws Exception
	{
		if (args.length != 3) {
			throw new Exception("Usage: UpscaleArt <input.png> <output.png> <factor: 2|4>");
		}

		File inFile = new File(args[0]);
		File outFile = new File(args[1]);
		int factor = Integer.parseInt(args[2]);
		if (factor != 2 && factor != 4) {
			throw new Exception("factor must be 2 or 4");
		}

		BufferedImage img = toArgb(ImageIO.read(inFile));
		img = scale2x(img);
		if (factor == 4) {
			img = scale2x(img);
		}

		outFile.getAbsoluteFile().getParentFile().mkdirs();
		ImageIO.write(img, "png", outFile);
	}

	static BufferedImage toArgb(BufferedImage src)
	{
		if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
			return src;
		}
		BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(),
			BufferedImage.TYPE_INT_ARGB);
		dst.createGraphics().drawImage(src, 0, 0, null);
		return dst;
	}

	public static BufferedImage scale2x(BufferedImage src)
	{
		final int w = src.getWidth();
		final int h = src.getHeight();
		int [] in = src.getRGB(0, 0, w, h, null, 0, w);
		int [] out = new int[w*2 * h*2];

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = in[y*w + x];
				int a = in[Math.max(y-1, 0)*w + x];     // above
				int b = in[y*w + Math.min(x+1, w-1)];   // right
				int c = in[y*w + Math.max(x-1, 0)];     // left
				int d = in[Math.min(y+1, h-1)*w + x];   // below

				int e0 = p, e1 = p, e2 = p, e3 = p;
				if (c == a && c != d && a != b) e0 = a;
				if (a == b && a != c && b != d) e1 = b;
				if (d == c && d != a && c != b) e2 = c;
				if (b == d && b != a && d != c) e3 = d;

				int ox = x*2;
				int oy = y*2;
				out[oy*w*2 + ox] = e0;
				out[oy*w*2 + ox+1] = e1;
				out[(oy+1)*w*2 + ox] = e2;
				out[(oy+1)*w*2 + ox+1] = e3;
			}
		}

		BufferedImage dst = new BufferedImage(w*2, h*2, BufferedImage.TYPE_INT_ARGB);
		dst.setRGB(0, 0, w*2, h*2, out, 0, w*2);
		return dst;
	}
}
