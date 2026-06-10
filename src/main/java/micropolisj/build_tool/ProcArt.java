// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.build_tool;

import java.awt.image.BufferedImage;

/**
 * Shared helpers for the procedural art generators: deterministic
 * periodic value noise, material-mask smoothing, the common ground and
 * water textures, color math and image utilities.
 *
 * All textures are periodic over a tile (u,v in [0,1)) so that adjacent
 * map tiles join seamlessly.
 */
final class ProcArt
{
	static final int SRC = 16;   // source tile size
	static final int N = 64;     // render resolution per tile

	private ProcArt()
	{
	}

	//
	// deterministic hashing / noise
	//

	static float hash1(int i, int seed)
	{
		int h = i*0x9E3779B1 ^ seed*0x85EBCA77;
		h ^= h >>> 16;
		h *= 0x7FEB352D;
		h ^= h >>> 15;
		h *= 0x846CA68B;
		h ^= h >>> 16;
		return (h & 0x7fffffff) / (float)0x7fffffff;
	}

	static float hash2(int x, int y, int seed)
	{
		return hash1(x*73856093 ^ y*19349663, seed);
	}

	/** Value noise with lattice frequency {@code freq}, wrapping at u,v in [0,1). */
	static float noise(float u, float v, int freq, int seed)
	{
		float x = u*freq, y = v*freq;
		int x0 = (int)Math.floor(x), y0 = (int)Math.floor(y);
		float fx = x - x0, fy = y - y0;
		fx = fx*fx*fx*(fx*(fx*6-15)+10);
		fy = fy*fy*fy*(fy*(fy*6-15)+10);
		float v00 = hash2(Math.floorMod(x0, freq), Math.floorMod(y0, freq), seed);
		float v10 = hash2(Math.floorMod(x0+1, freq), Math.floorMod(y0, freq), seed);
		float v01 = hash2(Math.floorMod(x0, freq), Math.floorMod(y0+1, freq), seed);
		float v11 = hash2(Math.floorMod(x0+1, freq), Math.floorMod(y0+1, freq), seed);
		return (v00*(1-fx)+v10*fx)*(1-fy) + (v01*(1-fx)+v11*fx)*fy;
	}

	/** Three-octave fractal noise, periodic over the tile. */
	static float fbm(float u, float v, int seed)
	{
		return clamp(
			0.5f*noise(u, v, 4, seed)
			+ 0.3f*noise(u, v, 8, seed+1)
			+ 0.2f*noise(u, v, 16, seed+2),
			0f, 1f);
	}

	//
	// common textures (shared so different sheets join seamlessly)
	//

	static float [] landTexture(float u, float v)
	{
		float n = fbm(u, v, 1);
		float grain = noise(u, v, 32, 4);
		float patches = noise(u, v, 3, 7);
		float [] col = mix(rgb(190, 114, 90), rgb(216, 141, 112), n);
		col = mix(col, rgb(170, 116, 86), 0.55f*smoothstep(0.55f, 0.85f, patches));
		return scale(col, 0.94f + 0.12f*grain);
	}

	static float [] grassTexture(float u, float v)
	{
		float n = fbm(u, v, 11);
		float grain = noise(u, v, 32, 14);
		float [] col = mix(rgb(84, 134, 62), rgb(110, 158, 76), n);
		return scale(col, 0.95f + 0.10f*grain);
	}

	static float [] waterTexture(float u, float v, float depth)
	{
		float wave = fbm(u + 0.15f*fbm(v, u, 21), v, 22);
		float [] col = mix(rgb(125, 150, 238), rgb(52, 64, 196), smoothstep(0.5f, 1.0f, depth));
		return mix(col, rgb(160, 190, 245), 0.25f*wave);
	}

	//
	// mask helpers
	//

	/** Upsamples a SRCxSRC mask to NxN (bilinear) and smooths it. */
	static float [] smoothMask(float [] mask)
	{
		return smoothMask(mask, 3);
	}

	static float [] smoothMask(float [] mask, int blurPasses)
	{
		float [] m = upsample(mask);
		for (int i = 0; i < blurPasses; i++) {
			m = boxBlur(m);
		}
		return m;
	}

	/** Bilinear upsample of a SRCxSRC mask to NxN, edge-clamped. */
	static float [] upsample(float [] mask)
	{
		float [] big = new float[N*N];
		float ratio = SRC / (float)N;
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float sx = (x + 0.5f)*ratio - 0.5f;
				float sy = (y + 0.5f)*ratio - 0.5f;
				int x0 = (int)Math.floor(sx);
				int y0 = (int)Math.floor(sy);
				float fx = sx - x0;
				float fy = sy - y0;
				float v00 = at(mask, x0, y0);
				float v10 = at(mask, x0+1, y0);
				float v01 = at(mask, x0, y0+1);
				float v11 = at(mask, x0+1, y0+1);
				big[y*N+x] = (v00*(1-fx)+v10*fx)*(1-fy) + (v01*(1-fx)+v11*fx)*fy;
			}
		}
		return big;
	}

	static float at(float [] m, int x, int y)
	{
		x = Math.max(0, Math.min(SRC-1, x));
		y = Math.max(0, Math.min(SRC-1, y));
		return m[y*SRC+x];
	}

	static float [] boxBlur(float [] src)
	{
		final int R = 3;
		float [] tmp = new float[N*N];
		float [] dst = new float[N*N];
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float sum = 0;
				for (int k = -R; k <= R; k++) {
					sum += src[y*N + Math.max(0, Math.min(N-1, x+k))];
				}
				tmp[y*N+x] = sum / (2*R+1);
			}
		}
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float sum = 0;
				for (int k = -R; k <= R; k++) {
					sum += tmp[Math.max(0, Math.min(N-1, y+k))*N + x];
				}
				dst[y*N+x] = sum / (2*R+1);
			}
		}
		return dst;
	}

	//
	// image utilities
	//

	static BufferedImage copy(BufferedImage src)
	{
		BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(),
			BufferedImage.TYPE_INT_ARGB);
		dst.createGraphics().drawImage(src, 0, 0, null);
		return dst;
	}

	static void paste(BufferedImage sheet, BufferedImage tile, int offsetX, int offsetY)
	{
		java.awt.Graphics2D gr = sheet.createGraphics();
		gr.setComposite(java.awt.AlphaComposite.Src);
		gr.drawImage(tile, offsetX, offsetY, null);
		gr.dispose();
	}

	/** Box-filter downscale by 2 (averages each 2x2 block, alpha-weighted). */
	static BufferedImage halve(BufferedImage src)
	{
		int w = src.getWidth()/2;
		int h = src.getHeight()/2;
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int a=0, r=0, g=0, b=0;
				for (int j = 0; j < 2; j++) {
					for (int i = 0; i < 2; i++) {
						int p = src.getRGB(x*2+i, y*2+j);
						int pa = p>>>24;
						a += pa;
						r += ((p>>16)&0xff)*pa;
						g += ((p>>8)&0xff)*pa;
						b += (p&0xff)*pa;
					}
				}
				if (a == 0) {
					dst.setRGB(x, y, 0);
				}
				else {
					dst.setRGB(x, y, ((a/4)<<24)|((r/a)<<16)|((g/a)<<8)|(b/a));
				}
			}
		}
		return dst;
	}

	/** Nearest-neighbor downscale by 2 (matches what MakeTiles would do). */
	static BufferedImage nearestHalf(BufferedImage src)
	{
		int w = src.getWidth()/2;
		int h = src.getHeight()/2;
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				dst.setRGB(x, y, src.getRGB(x*2, y*2));
			}
		}
		return dst;
	}

	//
	// color helpers (rgb components as float[3] in 0..255)
	//

	static float [] rgb(int r, int g, int b)
	{
		return new float[] { r, g, b };
	}

	static float [] mix(float [] a, float [] b, float t)
	{
		return new float[] {
			a[0] + (b[0]-a[0])*t,
			a[1] + (b[1]-a[1])*t,
			a[2] + (b[2]-a[2])*t };
	}

	static float [] scale(float [] c, float f)
	{
		return new float[] { c[0]*f, c[1]*f, c[2]*f };
	}

	static int pack(float [] c)
	{
		int r = Math.max(0, Math.min(255, Math.round(c[0])));
		int g = Math.max(0, Math.min(255, Math.round(c[1])));
		int b = Math.max(0, Math.min(255, Math.round(c[2])));
		return 0xff000000 | (r<<16) | (g<<8) | b;
	}

	static int packA(float [] c, float alpha)
	{
		int a = Math.max(0, Math.min(255, Math.round(alpha*255)));
		return (pack(c) & 0xffffff) | (a<<24);
	}

	static float clamp(float v, float lo, float hi)
	{
		return v < lo ? lo : v > hi ? hi : v;
	}

	static float smoothstep(float lo, float hi, float v)
	{
		float t = clamp((v - lo) / (hi - lo), 0f, 1f);
		return t*t*(3 - 2*t);
	}
}
