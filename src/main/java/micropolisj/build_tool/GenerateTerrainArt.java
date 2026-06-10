// This file is part of MicropolisJ.
// Copyright (C) 2013 Jason Long
// Portions Copyright (C) 1989-2007 Electronic Arts Inc.
//
// MicropolisJ is free software; you can redistribute it and/or modify
// it under the terms of the GNU GPLv3, with additional terms.
// See the README file, included in this distribution, for details.

package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import static micropolisj.build_tool.ProcArt.*;

/**
 * Generates modern, full-color terrain art (dirt, water, forest, parks,
 * rubble, flood, radiation, fire) to replace the original
 * heavily-dithered 16px tiles.
 *
 * The original 16px tiles are used as "material masks" where possible:
 * each pixel is classified as water / tree / land by its palette color,
 * the masks are upsampled and smoothed, and full-color procedural
 * textures are rendered through them. This way every shoreline and
 * forest-edge orientation of the original tile set is reproduced
 * automatically. Parks, rubble, flood, radiation and fire are drawn
 * from scratch.
 *
 * Outputs (consumed by MakeTiles via its name_NxN.png convention):
 *   terrain_{8x8,16x16,32x32,64x64}.png
 *   misc_animation_{8x8,16x16,32x32,64x64}.png   (fountain frames redrawn)
 *
 * Tiles 38-39 (unused) keep the upscaled original art.
 *
 * Usage: java micropolisj.build_tool.GenerateTerrainArt <graphics-dir>
 */
public class GenerateTerrainArt
{
	// original palette -> material classification
	static final int [] WATER_COLORS = { 0x6666e6, 0x0000e6 };
	static final int [] TREE_COLORS = { 0x00e600, 0x007f00 };

	// terrain rows that get procedural art
	static final int FIRST_TILE = 0;
	static final int LAST_TILE = 63;
	static final int [] SKIP_TILES = { 38, 39 };
	static final int FIRST_PARK_TILE = 40;
	static final int LAST_PARK_TILE = 43;
	static final int FIRST_RUBBLE_TILE = 44;
	static final int FIRST_FLOOD_TILE = 48;
	static final int RADIATION_TILE = 52;
	static final int FIRST_SCORCH_TILE = 53;
	static final int FIRST_FIRE_TILE = 56;

	// fountain animation frames in misc_animation.png (16px basis)
	static final int FOUNTAIN_Y = 208;
	static final int FOUNTAIN_FRAMES = 4;

	final File graphicsDir;
	BufferedImage terrainSrc;

	GenerateTerrainArt(File graphicsDir)
	{
		this.graphicsDir = graphicsDir;
	}

	public static void main(String [] args)
		throws Exception
	{
		File dir = new File(args.length > 0 ? args[0] : ".");
		new GenerateTerrainArt(dir).run();
	}

	void run()
		throws Exception
	{
		terrainSrc = UpscaleArt.toArgb(ImageIO.read(new File(graphicsDir, "terrain.png")));
		int ntiles = terrainSrc.getHeight() / SRC;

		// render the redrawn tiles at full resolution
		BufferedImage [] hi = new BufferedImage[ntiles];
		for (int t = FIRST_TILE; t <= LAST_TILE; t++) {
			if (isSkipped(t)) continue;
			hi[t] = renderTerrainTile(t);
		}

		// terrain sheets: upscaled baseline with redrawn tiles pasted over
		BufferedImage base64 = UpscaleArt.scale2x(UpscaleArt.scale2x(terrainSrc));
		BufferedImage base32 = UpscaleArt.scale2x(terrainSrc);
		BufferedImage base16 = copy(terrainSrc);
		BufferedImage base8 = nearestHalf(terrainSrc);
		for (int t = 0; t < ntiles; t++) {
			if (hi[t] == null) continue;
			paste(base64, hi[t], 0, t*N);
			BufferedImage t32 = halve(hi[t]);
			paste(base32, t32, 0, t*32);
			BufferedImage t16 = halve(t32);
			paste(base16, t16, 0, t*16);
			paste(base8, halve(t16), 0, t*8);
		}
		write(base64, "terrain_64x64.png");
		write(base32, "terrain_32x32.png");
		write(base16, "terrain_16x16.png");
		write(base8, "terrain_8x8.png");

		// fountain frames in misc_animation; base on the de-dithered
		// version when the pipeline already produced one
		File misc16File = new File(graphicsDir, "misc_animation_16x16.png");
		BufferedImage misc = UpscaleArt.toArgb(ImageIO.read(
			misc16File.exists() ? misc16File : new File(graphicsDir, "misc_animation.png")));
		BufferedImage misc64 = UpscaleArt.scale2x(UpscaleArt.scale2x(misc));
		BufferedImage misc32 = UpscaleArt.scale2x(misc);
		BufferedImage misc16 = copy(misc);
		for (int f = 0; f < FOUNTAIN_FRAMES; f++) {
			BufferedImage f64 = renderFountainFrame(f);
			int y = FOUNTAIN_Y + f*SRC;
			paste(misc64, f64, 0, y*4);
			BufferedImage f32 = halve(f64);
			paste(misc32, f32, 0, y*2);
			paste(misc16, halve(f32), 0, y);
		}
		write(misc64, "misc_animation_64x64.png");
		write(misc32, "misc_animation_32x32.png");
		write(misc16, "misc_animation_16x16.png");
		write(halve(misc16), "misc_animation_8x8.png");

		writePreview(hi);
	}

	static boolean isSkipped(int tile)
	{
		for (int s : SKIP_TILES) {
			if (s == tile) return true;
		}
		return false;
	}

	void write(BufferedImage img, String fileName)
		throws Exception
	{
		File f = new File(graphicsDir, fileName);
		System.out.println("Generating art: "+f);
		ImageIO.write(img, "png", f);
	}

	//
	// terrain tile rendering
	//

	BufferedImage renderTerrainTile(int tile)
	{
		if (tile >= FIRST_PARK_TILE && tile <= LAST_PARK_TILE) {
			return renderParkTile(tile);
		}
		if (tile >= FIRST_RUBBLE_TILE && tile < FIRST_FLOOD_TILE) {
			return renderRubbleTile(tile - FIRST_RUBBLE_TILE);
		}
		if (tile >= FIRST_FLOOD_TILE && tile < RADIATION_TILE) {
			return renderFloodTile(tile - FIRST_FLOOD_TILE);
		}
		if (tile == RADIATION_TILE) {
			return renderRadiationTile();
		}
		if (tile >= FIRST_SCORCH_TILE && tile < FIRST_FIRE_TILE) {
			return renderScorchTile(tile - FIRST_SCORCH_TILE);
		}
		if (tile >= FIRST_FIRE_TILE && tile <= LAST_TILE) {
			return renderFireTile(tile - FIRST_FIRE_TILE);
		}

		float [] water = smoothMask(materialMask(tile, WATER_COLORS));
		float [] tree = smoothMask(materialMask(tile, TREE_COLORS));

		// canopy "height" field: tree mask modulated by individual crowns
		float [] canopyHeight = new float[N*N];
		int [] crownId = new int[N*N];
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float crown = crownField(x / (float)N, y / (float)N, 31, crownId, y*N+x);
				canopyHeight[y*N+x] = tree[y*N+x] * (0.25f + 0.75f*crown);
			}
		}
		canopyHeight = boxBlur(canopyHeight);

		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float w = water[y*N+x];
				float t = tree[y*N+x];

				// wobble the coastline so it doesn't look ruler-straight
				float ws = clamp(w + (fbm(u, v, 51) - 0.5f) * 0.14f * shoreband(w), 0f, 1f);

				float [] col = landTexture(u, v);

				// wet darker band on the land side of a shore
				float wet = smoothstep(0.28f, 0.47f, ws);
				col = mix(col, rgb(150, 112, 74), 0.65f*wet);

				// water with depth (mask value doubles as depth proxy)
				float a = smoothstep(0.44f, 0.56f, ws);
				if (a > 0f) {
					col = mix(col, waterTexture(u, v, w), a);
				}

				// soft ground shadow around tree clusters
				float shadow = smoothstep(0.12f, 0.42f, t) * (1f - smoothstep(0.42f, 0.58f, t));
				col = scale(col, 1f - 0.22f*shadow);

				// tree canopy, lit from the top-left
				float cover = smoothstep(0.40f, 0.52f, t);
				if (cover > 0f) {
					col = mix(col, canopyColor(x, y, canopyHeight, crownId, false), cover);
				}

				out.setRGB(x, y, pack(col));
			}
		}
		return out;
	}

	/** 1 in the transition zone of a shoreline, 0 in open water / inland. */
	static float shoreband(float w)
	{
		return smoothstep(0.05f, 0.25f, w) * (1f - smoothstep(0.75f, 0.95f, w));
	}

	/**
	 * Field of individual tree crowns: rounded bumps on a jittered grid,
	 * periodic over the tile. Also reports the id of the nearest crown so
	 * each tree can get its own tint.
	 */
	static float crownField(float u, float v, int seed, int [] idOut, int idIndex)
	{
		final int G = 4;   // crowns per tile edge
		float best = 0f;
		int bestId = 0;
		int i0 = (int)Math.floor(u*G);
		int j0 = (int)Math.floor(v*G);
		for (int dj = -1; dj <= 1; dj++) {
			for (int di = -1; di <= 1; di++) {
				int i = i0 + di;
				int j = j0 + dj;
				int ci = Math.floorMod(i, G);
				int cj = Math.floorMod(j, G);
				float cx = (i + 0.2f + 0.6f*hash2(ci, cj, seed)) / G;
				float cy = (j + 0.2f + 0.6f*hash2(ci, cj, seed+1)) / G;
				float r = (0.55f + 0.25f*hash2(ci, cj, seed+2)) / G;
				float dx = (u - cx) / r;
				float dy = (v - cy) / r;
				float f = 1f - (dx*dx + dy*dy);
				if (f > best) {
					best = f;
					bestId = ci*31 + cj*113;
				}
			}
		}
		if (idOut != null) {
			idOut[idIndex] = bestId;
		}
		return clamp(best, 0f, 1f);
	}

	float [] canopyColor(int x, int y, float [] height, int [] crownId, boolean park)
	{
		float h = height[y*N+x];

		// lighting from the canopy height gradient, sun in the top-left
		float hx = height[y*N + Math.min(x+1, N-1)] - height[y*N + Math.max(x-1, 0)];
		float hy = height[Math.min(y+1, N-1)*N + x] - height[Math.max(y-1, 0)*N + x];
		float lum = clamp(1f + 3.0f*(-hx - hy), 0.58f, 1.36f);

		// dark understory in the gaps between crowns
		float gap = 1f - smoothstep(0.28f, 0.45f, h);

		// per-crown tint variation
		float tint = hash1(crownId[y*N+x], 17);

		float [] hi = park
			? mix(rgb(34, 92, 40), rgb(50, 110, 48), tint)
			: mix(rgb(58, 128, 52), rgb(82, 148, 62), tint);
		float [] lo = park ? rgb(14, 56, 25) : rgb(28, 80, 35);

		float [] col = mix(lo, hi, smoothstep(0.30f, 0.85f, h));
		col = scale(col, lum);
		return mix(col, park ? rgb(10, 42, 18) : rgb(16, 52, 24), gap);
	}

	/**
	 * Park tiles are drawn from scratch (the original art is just noise):
	 * a darker, maintained grass lawn with a handful of large dark trees.
	 * Four variants, one per woods animation frame.
	 */
	BufferedImage renderParkTile(int tile)
	{
		int seed = 200 + (tile - FIRST_PARK_TILE)*10;
		final int ntrees = 6;
		float [] tx = new float[ntrees];
		float [] ty = new float[ntrees];
		float [] tr = new float[ntrees];
		for (int k = 0; k < ntrees; k++) {
			tx[k] = 0.16f + 0.68f*hash1(k, seed);
			ty[k] = 0.16f + 0.68f*hash1(k, seed+1);
			tr[k] = 0.13f + 0.05f*hash1(k, seed+2);
		}

		float [] height = new float[N*N];
		int [] crownId = new int[N*N];
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float best = 0f;
				int bestId = 0;
				for (int k = 0; k < ntrees; k++) {
					float dx = (u - tx[k]) / tr[k];
					float dy = (v - ty[k]) / tr[k];
					float f = 1f - (dx*dx + dy*dy);
					if (f > best) {
						best = f;
						bestId = seed + k;
					}
				}
				height[y*N+x] = clamp(best, 0f, 1f);
				crownId[y*N+x] = bestId;
			}
		}
		height = boxBlur(height);

		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float h = height[y*N+x];

				float [] col = grassTexture(u, v);

				float shadow = smoothstep(0.04f, 0.20f, h) * (1f - smoothstep(0.20f, 0.34f, h));
				col = scale(col, 1f - 0.25f*shadow);

				float cover = smoothstep(0.16f, 0.30f, h);
				if (cover > 0f) {
					col = mix(col, canopyColor(x, y, height, crownId, true), cover);
				}

				out.setRGB(x, y, pack(col));
			}
		}
		return out;
	}

	/** Rubble: ash-tinted ground strewn with angular debris chunks. */
	BufferedImage renderRubbleTile(int variant)
	{
		int seed = 300 + variant*10;
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float [] col = mix(landTexture(u, v), rgb(122, 106, 96), 0.35f);
				float ash = noise(u, v, 8, seed+5);
				col = mix(col, rgb(86, 78, 72), 0.35f*smoothstep(0.55f, 0.85f, ash));
				out.setRGB(x, y, pack(col));
			}
		}

		Graphics2D gr = out.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int k = 0; k < 26; k++) {
			float cx = hash1(k, seed)*N;
			float cy = hash1(k, seed+1)*N;
			float w = 3f + 6f*hash1(k, seed+2);
			float h = 2f + 4f*hash1(k, seed+3);
			float rot = hash1(k, seed+4)*(float)Math.PI;
			int shade = 88 + (int)(70*hash1(k, seed+5));
			AffineTransform saved = gr.getTransform();
			gr.translate(cx, cy);
			gr.rotate(rot);
			gr.setColor(new Color(0, 0, 0, 70));
			gr.fill(new Rectangle2D.Float(-w/2+1.2f, -h/2+1.2f, w, h));
			gr.setColor(new Color(shade, shade-6, shade-10));
			gr.fill(new Rectangle2D.Float(-w/2, -h/2, w, h));
			gr.setColor(new Color(Math.min(255, shade+38), Math.min(255, shade+32), shade+25));
			gr.fill(new Rectangle2D.Float(-w/2, -h/2, w, h/3));
			gr.setTransform(saved);
		}
		gr.dispose();
		return out;
	}

	/** Flood: shallow churning water with foam and floating debris. */
	BufferedImage renderFloodTile(int frame)
	{
		int seed = 340 + frame;
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float [] col = waterTexture(u, v, 0.35f);
				col = mix(col, rgb(118, 104, 76), 0.22f);   // muddy flood water

				float foam = fbm(u, v, seed);
				col = mix(col, rgb(225, 232, 240), 0.8f*smoothstep(0.62f, 0.80f, foam));

				out.setRGB(x, y, pack(col));
			}
		}

		Graphics2D gr = out.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int k = 0; k < 4; k++) {
			float cx = (hash1(k, 350) + 0.1f*frame) % 1f * N;
			float cy = hash1(k, 351)*N;
			float w = 6f + 6f*hash1(k, 352);
			float rot = hash1(k, 353)*(float)Math.PI;
			AffineTransform saved = gr.getTransform();
			gr.translate(cx, cy);
			gr.rotate(rot);
			gr.setColor(new Color(112, 80, 48));
			gr.fill(new Rectangle2D.Float(-w/2, -1.5f, w, 3f));
			gr.setColor(new Color(150, 112, 70));
			gr.fill(new Rectangle2D.Float(-w/2, -1.5f, w, 1.4f));
			gr.setTransform(saved);
		}
		gr.dispose();
		return out;
	}

	/** Radiation marker: scorched ground with a weathered trefoil sign. */
	BufferedImage renderRadiationTile()
	{
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		float cx = N/2f, cy = N/2f;
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float r = (float)Math.hypot(x+0.5f-cx, y+0.5f-cy);
				float [] col = landTexture(u, v);
				float burn = (1f - smoothstep(16f, 30f, r)) * (0.5f + 0.3f*fbm(u, v, 91));
				col = mix(col, rgb(58, 48, 44), burn);
				out.setRGB(x, y, pack(col));
			}
		}

		Graphics2D gr = out.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gr.setColor(new Color(228, 184, 32));
		gr.fill(new Ellipse2D.Float(cx-17, cy-17, 34, 34));
		gr.setColor(new Color(180, 140, 20));
		gr.draw(new Ellipse2D.Float(cx-17, cy-17, 34, 34));
		gr.setColor(new Color(28, 24, 20));
		for (int k = 0; k < 3; k++) {
			gr.fill(new Arc2D.Float(cx-14, cy-14, 28, 28, 90 + k*120 - 30, 60, Arc2D.PIE));
		}
		gr.setColor(new Color(228, 184, 32));
		gr.fill(new Ellipse2D.Float(cx-5.5f, cy-5.5f, 11, 11));
		gr.setColor(new Color(28, 24, 20));
		gr.fill(new Ellipse2D.Float(cx-3.2f, cy-3.2f, 6.4f, 6.4f));
		gr.dispose();
		return out;
	}

	/** Contaminated ground variants: scorch patches with a sickly tint. */
	BufferedImage renderScorchTile(int variant)
	{
		int seed = 370 + variant*7;
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float [] col = landTexture(u, v);
				float scorch = fbm(u, v, seed);
				col = mix(col, rgb(52, 44, 40), 0.75f*smoothstep(0.52f, 0.78f, scorch));
				float glow = noise(u, v, 6, seed+3);
				col = mix(col, rgb(118, 138, 58), 0.4f*smoothstep(0.68f, 0.88f, glow));
				out.setRGB(x, y, pack(col));
			}
		}
		return out;
	}

	/** Fire: eight flickering frames of procedural flames. */
	BufferedImage renderFireTile(int frame)
	{
		int seed = 400 + frame*3;
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;

				// vertically stretched, domain-warped turbulence makes
				// licking flame tongues; hotter toward the bottom
				float warp = noise(u, v*0.5f, 6, seed+9) - 0.5f;
				float uu = u + 0.18f*warp;
				float turb = 0.55f*noise(uu, v*0.45f, 6, seed)
					+ 0.30f*noise(uu, v*0.45f, 12, seed+1)
					+ 0.15f*noise(uu, v*0.45f, 24, seed+2);
				float envelope = (0.42f + 0.95f*v)
					* (0.88f + 0.24f*(1f - Math.abs(u - 0.5f)*2f));
				float heat = turb * envelope * 1.25f;

				float [] col;
				if (heat < 0.30f) {
					// scorched, smoke-darkened ground
					float [] ground = mix(landTexture(u, v), rgb(46, 36, 32), 0.78f);
					col = mix(ground, rgb(120, 36, 14), smoothstep(0.20f, 0.30f, heat));
				}
				else {
					float [] red = rgb(206, 44, 14);
					float [] orange = rgb(255, 126, 18);
					float [] yellow = rgb(255, 212, 48);
					float [] white = rgb(255, 248, 200);
					col = mix(red, orange, smoothstep(0.30f, 0.52f, heat));
					col = mix(col, yellow, smoothstep(0.52f, 0.72f, heat));
					col = mix(col, white, smoothstep(0.72f, 0.88f, heat));
				}
				out.setRGB(x, y, pack(col));
			}
		}

		// rising sparks
		Graphics2D gr = out.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int k = 0; k < 7; k++) {
			float ph = (hash1(k, 430) + frame/8f) % 1f;
			float sx = (hash1(k, 431) + 0.08f*(float)Math.sin(2*Math.PI*(ph + hash1(k, 432)))) % 1f * N;
			float sy = (1f - ph) * N * 0.85f;
			int alpha = (int)(200*(1f - ph));
			gr.setColor(new Color(255, 230, 120, Math.max(0, alpha)));
			gr.fill(new Ellipse2D.Float(sx-1.2f, sy-1.2f, 2.4f, 2.4f));
		}
		gr.dispose();
		return out;
	}

	//
	// fountain
	//

	BufferedImage renderFountainFrame(int frame)
	{
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		float cx = N/2f, cy = N/2f;
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float dx = x + 0.5f - cx;
				float dy = y + 0.5f - cy;
				float r = (float)Math.sqrt(dx*dx + dy*dy);

				float [] col = grassTexture(u, v);

				// stone plaza
				float stone = fbm(u, v, 41);
				float [] plaza = mix(rgb(168, 168, 172), rgb(196, 196, 198), stone);
				col = mix(col, plaza, 1f - smoothstep(26f, 28f, r));

				// basin rim
				float rim = smoothstep(15f, 17f, r) * (1f - smoothstep(20f, 22f, r));
				col = mix(col, rgb(110, 110, 118), rim);

				// basin water with rippling rings
				float inWater = 1f - smoothstep(14f, 16f, r);
				if (inWater > 0f) {
					float phase = frame / (float)FOUNTAIN_FRAMES;
					float ripple = 0.5f + 0.5f*(float)Math.sin(r*0.9 - phase*2*Math.PI);
					float [] wcol = mix(rgb(70, 110, 220), rgb(130, 170, 240), ripple);
					col = mix(col, wcol, inWater);
				}

				out.setRGB(x, y, pack(col));
			}
		}

		// spray droplets arcing out of the center, animated by frame
		Graphics2D gr = out.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		for (int jet = 0; jet < 10; jet++) {
			double angle = 2*Math.PI * (jet + 0.6*hash1(jet, 5)) / 10;
			for (int d = 0; d < 4; d++) {
				double ph = (d/4.0 + frame/(double)FOUNTAIN_FRAMES + hash1(jet, 9)) % 1.0;
				double dist = 2 + ph*11;
				double lift = 10*(1 - (2*ph-1)*(2*ph-1));   // parabolic arc
				int px = (int)Math.round(cx + Math.cos(angle)*dist);
				int py = (int)Math.round(cy + Math.sin(angle)*dist*0.55 - lift*0.45);
				int alpha = (int)(230*(1-ph) + 60);
				gr.setColor(new Color(235, 245, 255, Math.min(255, alpha)));
				int sz = ph < 0.4 ? 3 : 2;
				gr.fillOval(px-sz/2, py-sz/2, sz, sz);
			}
		}
		// central spout
		gr.setColor(new Color(225, 240, 255, 235));
		gr.fillOval((int)cx-2, (int)cy-8, 4, 9);
		gr.dispose();

		return out;
	}

	//
	// material masks
	//

	float [] materialMask(int tile, int [] colors)
	{
		float [] m = new float[SRC*SRC];
		for (int y = 0; y < SRC; y++) {
			for (int x = 0; x < SRC; x++) {
				int rgb = terrainSrc.getRGB(x, tile*SRC + y) & 0xffffff;
				for (int c : colors) {
					if (rgb == c) {
						m[y*SRC+x] = 1f;
						break;
					}
				}
			}
		}
		return m;
	}

	//
	// preview montage for visual iteration (written to the temp dir)
	//

	void writePreview(BufferedImage [] hi)
		throws Exception
	{
		int cols = 8;
		int rows = (LAST_TILE+1 + cols-1) / cols;
		// grid of all redrawn tiles, plus seam-test patches (3x3 of dirt,
		// dense forest, open water; 2x2 of park; fountain frames)
		BufferedImage img = new BufferedImage(cols*N + 8 + 6*N, Math.max(rows, 9)*N,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = img.createGraphics();
		for (int t = 0; t <= LAST_TILE; t++) {
			if (hi[t] == null) continue;
			gr.drawImage(hi[t], (t%cols)*N, (t/cols)*N, null);
		}
		int bx = cols*N + 8;
		int [] patches = { 0, 37, 2 };
		gr.setColor(Color.MAGENTA);
		gr.fillRect(bx, 0, 6*N, img.getHeight());
		for (int p = 0; p < patches.length; p++) {
			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++) {
					gr.drawImage(hi[patches[p]], bx + i*N, (p*3 + j)*N, null);
				}
			}
		}
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 2; i++) {
				gr.drawImage(hi[FIRST_PARK_TILE + j*2 + i], bx + 3*N + 8 + i*N, j*N, null);
			}
		}
		for (int f = 0; f < FOUNTAIN_FRAMES; f++) {
			gr.drawImage(renderFountainFrame(f), bx + 3*N + 8 + (f%2)*N, 2*N + 8 + (f/2)*N, null);
		}
		gr.dispose();

		File f = new File(System.getProperty("java.io.tmpdir"), "terrain_preview.png");
		System.out.println("Preview: "+f);
		ImageIO.write(img, "png", f);
	}
}
