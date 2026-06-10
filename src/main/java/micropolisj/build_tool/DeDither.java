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
 * Removes the dense two-color dithering that the original 1980s art uses
 * to fake intermediate shades, replacing it with the blended color.
 *
 * A pixel is an "alternation" candidate when at least 3 of its 4
 * neighbors have a different color and all of those agree on one color.
 * Candidates are only blended where many other candidates surround them,
 * so isolated single-pixel details (windows, lights) are preserved.
 *
 * Usage: java micropolisj.build_tool.DeDither <input.png> <output.png>
 */
public class DeDither
{
	public static void main(String [] args)
		throws Exception
	{
		if (args.length != 2) {
			throw new Exception("Usage: DeDither <input.png> <output.png>");
		}
		BufferedImage img = UpscaleArt.toArgb(ImageIO.read(new File(args[0])));
		BufferedImage out = dedither(img);
		new File(args[1]).getAbsoluteFile().getParentFile().mkdirs();
		ImageIO.write(out, "png", new File(args[1]));
	}

	public static BufferedImage dedither(BufferedImage src)
	{
		final int w = src.getWidth();
		final int h = src.getHeight();
		int [] in = src.getRGB(0, 0, w, h, null, 0, w);

		// phase 1: find alternation candidates and their partner color
		int [] partner = new int[w*h];
		boolean [] alt = new boolean[w*h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = in[y*w+x];
				if ((p >>> 24) < 128) continue;

				int differing = 0, agreeing = 0, b = 0;
				boolean consistent = true;
				for (int d = 0; d < 4; d++) {
					int nx = x + (d==0?1:d==1?-1:0);
					int ny = y + (d==2?1:d==3?-1:0);
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int q = in[ny*w+nx];
					if ((q >>> 24) < 128) continue;
					if (q == p) continue;
					differing++;
					if (agreeing == 0) {
						b = q;
						agreeing = 1;
					}
					else if (q != b) {
						consistent = false;
					}
				}
				if (differing >= 3 && consistent) {
					alt[y*w+x] = true;
					partner[y*w+x] = b;
				}
			}
		}

		// phase 2: inside a dense alternation field (a true checkerboard,
		// not a sparse window grid), replace every pixel with its local
		// average. Sparse grids score low density and stay crisp.
		int [] out = in.clone();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = in[y*w+x];
				if ((p >>> 24) < 128) continue;

				int density = 0;
				for (int j = -2; j <= 2; j++) {
					for (int i = -2; i <= 2; i++) {
						int nx = x+i, ny = y+j;
						if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
						if (alt[ny*w+nx]) density++;
					}
				}
				if (density < 12) continue;

				int r=0, g=0, b=0, cnt=0;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						int nx = x+i, ny = y+j;
						if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
						int q = in[ny*w+nx];
						if ((q >>> 24) < 128) continue;
						r += (q>>16)&0xff; g += (q>>8)&0xff; b += q&0xff;
						cnt++;
					}
				}
				out[y*w+x] = (p & 0xff000000) | ((r/cnt)<<16) | ((g/cnt)<<8) | (b/cnt);
			}
		}

		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		dst.setRGB(0, 0, w, h, out, 0, w);
		return dst;
	}
}
