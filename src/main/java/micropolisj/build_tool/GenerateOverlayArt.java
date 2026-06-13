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
 * Generates modern art for the road-like overlay sheets: roads.png,
 * rails.png, wires.png, the roadwire.png transparent overlay and the
 * traffic_frames.png car animations.
 *
 * Roads, rails and the rail/road/wire crossings are drawn geometrically
 * from per-row path layouts (straights, quarter arcs, T- and X-junctions),
 * so curves stay perfectly smooth. Wires use the original 16px tiles as
 * material masks (classified by palette color, smoothed, textured), which
 * reproduces every orientation automatically. Pixels of unrecognized
 * colors (e.g. traffic lights) are preserved as crisp blocks. The traffic
 * car animation is synthesized along geometric lanes with stable per-car
 * colors (see generateTraffic).
 *
 * Outputs: {roads,rails,wires,roadwire,traffic_frames}_{16x16,32x32,64x64}.png
 *
 * Usage: java micropolisj.build_tool.GenerateOverlayArt <graphics-dir>
 */
public class GenerateOverlayArt
{
	static final int WATER1 = 0x6666e6, WATER2 = 0x0000e6;
	static final int DIRT = 0xcc7f66, DIRT2 = 0x997f4c;
	static final int DARK = 0x3f3f3f, BLACK = 0x000000;
	static final int GRAY = 0x7f7f7f, LGRAY = 0xbfbfbf, WHITE = 0xffffff;
	static final int YELLOW = 0xffff00;

	final File graphicsDir;

	GenerateOverlayArt(File graphicsDir)
	{
		this.graphicsDir = graphicsDir;
	}

	public static void main(String [] args)
		throws Exception
	{
		File dir = new File(args.length > 0 ? args[0] : ".");
		GenerateOverlayArt self = new GenerateOverlayArt(dir);
		self.generateSheet("roads", self::renderRoadTile);
		self.generateSheet("rails", self::renderRailTile);
		self.generateSheet("wires", self::renderWireTile);
		self.generateSheet("roadwire", self::renderRoadWireTile);
		self.generateTraffic();
	}

	interface TileRenderer {
		BufferedImage render(BufferedImage src, int row);
	}

	void generateSheet(String name, TileRenderer renderer)
		throws Exception
	{
		BufferedImage src = UpscaleArt.toArgb(ImageIO.read(new File(graphicsDir, name+".png")));
		int rows = src.getHeight() / SRC;

		BufferedImage out64 = new BufferedImage(N, rows*N, BufferedImage.TYPE_INT_ARGB);
		for (int r = 0; r < rows; r++) {
			paste(out64, renderer.render(src, r), 0, r*N);
		}
		BufferedImage out32 = halve(out64);
		BufferedImage out16 = halve(out32);
		write(out64, name+"_64x64.png");
		write(out32, name+"_32x32.png");
		write(out16, name+"_16x16.png");
		write(halve(out16), name+"_8x8.png");
	}

	void write(BufferedImage img, String fileName)
		throws Exception
	{
		File f = new File(graphicsDir, fileName);
		System.out.println("Generating art: "+f);
		ImageIO.write(img, "png", f);
	}

	//
	// mask helpers
	//

	static float [] classMask(BufferedImage src, int row, int... colors)
	{
		float [] m = new float[SRC*SRC];
		for (int y = 0; y < SRC; y++) {
			for (int x = 0; x < SRC; x++) {
				int p = src.getRGB(x, row*SRC + y);
				if ((p >>> 24) < 128) continue;
				int rgb = p & 0xffffff;
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

	static float [] alphaMask(BufferedImage src, int row)
	{
		float [] m = new float[SRC*SRC];
		for (int y = 0; y < SRC; y++) {
			for (int x = 0; x < SRC; x++) {
				m[y*SRC+x] = (src.getRGB(x, row*SRC + y) >>> 24) >= 128 ? 1f : 0f;
			}
		}
		return m;
	}

	static boolean isEmpty(float [] mask)
	{
		for (float v : mask) {
			if (v > 0f) return false;
		}
		return true;
	}

	/** Ground under an overlay tile: water where the original had water, dirt elsewhere. */
	static float [] background(float [] water, int x, int y)
	{
		float u = x / (float)N;
		float v = y / (float)N;
		float w = water[y*N+x];
		float [] col = landTexture(u, v);
		float wet = smoothstep(0.28f, 0.47f, w);
		col = mix(col, rgb(150, 112, 74), 0.65f*wet);
		float a = smoothstep(0.44f, 0.56f, w);
		if (a > 0f) {
			col = mix(col, waterTexture(u, v, w), a);
		}
		return col;
	}

	/** Paints pixels of colors not covered by any mask as crisp blocks (traffic lights etc). */
	static void paintSpecials(BufferedImage out, BufferedImage src, int row, int... knownColors)
	{
		Graphics2D gr = out.createGraphics();
		for (int y = 0; y < SRC; y++) {
			for (int x = 0; x < SRC; x++) {
				int p = src.getRGB(x, row*SRC + y);
				if ((p >>> 24) < 128) continue;
				int rgb = p & 0xffffff;
				boolean known = false;
				for (int c : knownColors) {
					if (rgb == c) { known = true; break; }
				}
				if (!known) {
					gr.setColor(new Color(rgb));
					gr.fillOval(x*4, y*4, 4, 4);
				}
			}
		}
		gr.dispose();
	}

	//
	// roads
	//

	BufferedImage renderRoadTile(BufferedImage src, int row)
	{
		// roads.png layout: 0,1 = bridges (H/V), 15 = the open draw
		// bridge's center gap — plain open water, as in the original
		// art (the raised leaves live in the misc_animation tiles)
		if (row == 0 || row == 1) {
			return renderBridge(row == 0);
		}
		if (row == 15) {
			return renderOpenWater();
		}

		float [] opaque = alphaMask(src, row);
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		if (isEmpty(opaque)) {
			return out;   // fully transparent spare tile
		}

		// rows 2-12 share the rail sheet's per-row layout (straights,
		// curves, T- and X-junctions); drawing the asphalt geometrically
		// from those paths keeps curve edges perfectly smooth
		float [] water = smoothMask(classMask(src, row, WATER1, WATER2));
		TrackPath [] paths = railPaths(row);

		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;

				float [] col = background(water, x, y);

				float d = Float.MAX_VALUE, along = 0f;
				for (TrackPath p : paths) {
					float dd = pathDistance(p, x + 0.5f, y + 0.5f);
					if (dd < d) {
						d = dd;
						along = pathAlong(p, x + 0.5f, y + 0.5f);
					}
				}
				col = paintRoadSurface(col, d, along, u, v, true);

				out.setRGB(x, y, pack(col));
			}
		}

		paintSpecials(out, src, row, WATER1, WATER2, DIRT, DIRT2, DARK, BLACK, GRAY, WHITE, LGRAY);
		return out;
	}

	/**
	 * Mixes the geometric road surface over {@code col} given the distance
	 * {@code d} from the road centerline: asphalt out to ROAD_HW, a dark
	 * outline band at the boundary, and an optional dashed center line.
	 */
	static final float ROAD_HW = 26f;   // total half-width incl. outline

	static float [] paintRoadSurface(float [] col, float d, float along,
		float u, float v, boolean markings)
	{
		float a = 1f - smoothstep(ROAD_HW - 2f, ROAD_HW, d);
		if (a <= 0f) {
			return col;
		}
		float grain = noise(u, v, 32, 61);
		float [] asphalt = mix(rgb(66, 66, 70), rgb(84, 84, 88), grain);
		float edge = smoothstep(21f, 23.5f, d);
		asphalt = mix(asphalt, rgb(34, 34, 38), 0.8f*edge);
		col = mix(col, asphalt, a);

		if (markings && ((int)(along / 8f)) % 2 == 0) {
			float mk = 1f - smoothstep(1.4f, 2.6f, d);
			col = mix(col, rgb(232, 232, 228), 0.9f*mk);
		}
		return col;
	}

	/**
	 * Road bridge over water: straight-edged deck with railings and a
	 * dashed center line, standing on concrete piers — the pier caps
	 * poke out on both sides of the deck and the deck throws a shadow
	 * band on the water to the southeast, so the bridge clearly reads
	 * as elevated, like the classic art's pillars.
	 */
	BufferedImage renderBridge(boolean horizontal)
	{
		final float deckLo = 5f, deckHi = 59f;
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;
				float along = (horizontal ? x : y) + 0.5f;
				float across = (horizontal ? y : x) + 0.5f;

				float [] col = waterTexture(u, v, 0.9f);

				// the deck's shadow on the water along the south/east
				// side (sun from the northwest)
				float shade = smoothstep(deckHi-1f, deckHi+1f, across)
					* (1f - smoothstep(deckHi+4f, deckHi+7f, across));
				col = mix(col, rgb(16, 20, 56), 0.45f*shade);

				// pier caps every half tile, slightly wider than the
				// deck so they show on both sides
				float dPier = Math.min(Math.abs(along - 16f), Math.abs(along - 48f));
				float pier = 1f - smoothstep(3.5f, 5.5f, dPier);
				if (pier > 0f) {
					float stub = Math.max(
						smoothstep(deckLo-4.5f, deckLo-2.5f, across)
							* (1f - smoothstep(deckLo, deckLo+1.5f, across)),
						smoothstep(deckHi-1.5f, deckHi, across)
							* (1f - smoothstep(deckHi+2.5f, deckHi+4.5f, across)));
					if (stub > 0f) {
						// concrete, darker on the shaded southeast side
						float [] concrete = mix(rgb(132, 134, 140), rgb(74, 76, 84),
							smoothstep(deckLo, deckHi+4f, across));
						col = mix(col, concrete, pier*stub);
					}
				}

				float deck = smoothstep(deckLo-1f, deckLo+1f, across)
					* (1f - smoothstep(deckHi-1f, deckHi+1f, across));
				if (deck > 0f) {
					float grain = noise(u, v, 32, 61);
					float [] asphalt = mix(rgb(66, 66, 70), rgb(84, 84, 88), grain);

					// railings along the deck edges
					float rail = smoothstep(deckLo+1f, deckLo+3f, across)
						* (1f - smoothstep(deckHi-3f, deckHi-1f, across));
					asphalt = mix(rgb(26, 26, 30), asphalt, rail);

					// dashed center line
					boolean dashOn = ((int)(along / 8f)) % 2 == 0;
					if (dashOn && Math.abs(across - 32f) < 1.8f) {
						asphalt = mix(asphalt, rgb(235, 235, 230), 0.9f);
					}

					col = mix(col, asphalt, deck);
				}

				out.setRGB(x, y, pack(col));
			}
		}
		return out;
	}

	/** Plain open water, for the open drawbridge's center gap. */
	BufferedImage renderOpenWater()
	{
		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				out.setRGB(x, y, pack(waterTexture(x/(float)N, y/(float)N, 0.9f)));
			}
		}
		return out;
	}

	//
	// track geometry (shared by rails and the rail+wire tiles)
	//

	static final int WOOD = 0x997f4c;

	/** A parametric track path: straight segment or quarter arc. */
	static class TrackPath
	{
		// straight: from (x0,y0) to (x1,y1); arc: center (x0,y0), radius x1, angles y1..extra (degrees)
		boolean isArc;
		float x0, y0, x1, y1, a0, a1;

		static TrackPath straight(float x0, float y0, float x1, float y1)
		{
			TrackPath p = new TrackPath();
			p.x0=x0; p.y0=y0; p.x1=x1; p.y1=y1;
			return p;
		}

		static TrackPath arc(float cx, float cy, float r, float a0deg, float a1deg)
		{
			TrackPath p = new TrackPath();
			p.isArc = true;
			p.x0=cx; p.y0=cy; p.x1=r; p.a0=(float)Math.toRadians(a0deg); p.a1=(float)Math.toRadians(a1deg);
			return p;
		}

		float length()
		{
			if (isArc) return Math.abs(a1-a0)*x1;
			return (float)Math.hypot(x1-x0, y1-y0);
		}

		float [] point(float t)
		{
			if (isArc) {
				float a = a0 + (a1-a0)*t;
				return new float[] { x0 + x1*(float)Math.cos(a), y0 + x1*(float)Math.sin(a) };
			}
			return new float[] { x0 + (x1-x0)*t, y0 + (y1-y0)*t };
		}

		float [] normal(float t)
		{
			if (isArc) {
				float a = a0 + (a1-a0)*t;
				return new float[] { (float)Math.cos(a), (float)Math.sin(a) };
			}
			float len = length();
			return new float[] { -(y1-y0)/len, (x1-x0)/len };
		}
	}

	/** Track layout per rails.png row (16px tile coords scaled x4 to 64). */
	static TrackPath [] railPaths(int row)
	{
		switch (row) {
		case 0: case 2: case 13: case 15:
			return new TrackPath[] { TrackPath.straight(0, 32, 64, 32) };
		case 1: case 3: case 14:
			return new TrackPath[] { TrackPath.straight(32, 0, 32, 64) };
		case 4: return new TrackPath[] { TrackPath.arc(64, 0, 32, 180, 90) };
		case 5: return new TrackPath[] { TrackPath.arc(64, 64, 32, 180, 270) };
		case 6: return new TrackPath[] { TrackPath.arc(0, 64, 32, 0, -90) };
		case 7: return new TrackPath[] { TrackPath.arc(0, 0, 32, 0, 90) };
		case 8: return new TrackPath[] { TrackPath.straight(0, 32, 64, 32), TrackPath.straight(32, 0, 32, 32) };
		case 9: return new TrackPath[] { TrackPath.straight(32, 0, 32, 64), TrackPath.straight(32, 32, 64, 32) };
		case 10: return new TrackPath[] { TrackPath.straight(0, 32, 64, 32), TrackPath.straight(32, 32, 32, 64) };
		case 11: return new TrackPath[] { TrackPath.straight(32, 0, 32, 64), TrackPath.straight(0, 32, 32, 32) };
		case 12: return new TrackPath[] { TrackPath.straight(0, 32, 64, 32), TrackPath.straight(32, 0, 32, 64) };
		default: return new TrackPath[0];
		}
	}

	/** Rasterizes ballast / ties / rails coverage masks for the given paths. */
	static void strokeTrack(TrackPath [] paths, BufferedImage ballast, BufferedImage ties, BufferedImage rails)
	{
		Graphics2D bg = ballast.createGraphics();
		Graphics2D tg = ties.createGraphics();
		Graphics2D rg = rails.createGraphics();
		for (Graphics2D g : new Graphics2D[] { bg, tg, rg }) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Color.WHITE);
		}

		for (TrackPath p : paths) {
			float len = p.length();

			int nb = (int)Math.ceil(len / 1.5f);
			for (int i = 0; i <= nb; i++) {
				float [] pt = p.point(i/(float)nb);
				bg.fill(new Ellipse2D.Float(pt[0]-10.5f, pt[1]-10.5f, 21, 21));
			}

			int nt = Math.round(len / 7f);
			for (int i = 0; i <= nt; i++) {
				float t = nt == 0 ? 0.5f : (i + 0.5f) / (nt + 1);
				float [] pt = p.point(t);
				float [] nm = p.normal(t);
				float tx = -nm[1], ty = nm[0];
				Path2D quad = new Path2D.Float();
				quad.moveTo(pt[0] - nm[0]*8.5f - tx*1.7f, pt[1] - nm[1]*8.5f - ty*1.7f);
				quad.lineTo(pt[0] + nm[0]*8.5f - tx*1.7f, pt[1] + nm[1]*8.5f - ty*1.7f);
				quad.lineTo(pt[0] + nm[0]*8.5f + tx*1.7f, pt[1] + nm[1]*8.5f + ty*1.7f);
				quad.lineTo(pt[0] - nm[0]*8.5f + tx*1.7f, pt[1] - nm[1]*8.5f + ty*1.7f);
				quad.closePath();
				tg.fill(quad);
			}

			for (int side = -1; side <= 1; side += 2) {
				Path2D line = new Path2D.Float();
				int np = (int)Math.ceil(len / 2f);
				for (int i = 0; i <= np; i++) {
					float t = i/(float)np;
					float [] pt = p.point(t);
					float [] nm = p.normal(t);
					float px = pt[0] + nm[0]*5f*side;
					float py = pt[1] + nm[1]*5f*side;
					if (i == 0) line.moveTo(px, py); else line.lineTo(px, py);
				}
				rg.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
				rg.draw(line);
			}
		}
		bg.dispose(); tg.dispose(); rg.dispose();
	}

	static float coverage(BufferedImage mask, int x, int y)
	{
		return (mask.getRGB(x, y) >>> 24) / 255f;
	}

	/** Distance from (px,py) to the path centerline. */
	static float pathDistance(TrackPath p, float px, float py)
	{
		if (p.isArc) {
			return Math.abs((float)Math.hypot(px - p.x0, py - p.y0) - p.x1);
		}
		float dx = p.x1 - p.x0, dy = p.y1 - p.y0;
		float t = clamp(((px - p.x0)*dx + (py - p.y0)*dy) / (dx*dx + dy*dy), 0f, 1f);
		return (float)Math.hypot(px - (p.x0 + dx*t), py - (p.y0 + dy*t));
	}

	/** Arc-length coordinate along the path, used for dash phasing. */
	static float pathAlong(TrackPath p, float px, float py)
	{
		if (p.isArc) {
			float a = (float)Math.atan2(py - p.y0, px - p.x0);
			float rel = a - p.a0;
			while (rel > Math.PI) rel -= (float)(2*Math.PI);
			while (rel < -Math.PI) rel += (float)(2*Math.PI);
			return Math.abs(rel) * p.x1;
		}
		return p.x0 == p.x1 ? py : px;
	}

	//
	// rails
	//

	BufferedImage renderRailTile(BufferedImage src, int row)
	{
		boolean underwater = row <= 1;
		boolean roadCross = row >= 13;
		boolean roadHorizontal = row == 14;   // crossing road runs against the track

		float [] water = smoothMask(classMask(src, row, WATER1, WATER2));
		float [] crossMark = roadCross ? smoothMask(classMask(src, row, YELLOW, WHITE), 1) : null;

		BufferedImage ballast = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		BufferedImage ties = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		BufferedImage railLines = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		strokeTrack(railPaths(row), ballast, ties, railLines);

		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float u = x / (float)N;
				float v = y / (float)N;

				float [] col = background(water, x, y);

				if (roadCross) {
					float d = Math.abs((roadHorizontal ? y : x) + 0.5f - 32f);
					col = paintRoadSurface(col, d, 0f, u, v, false);
				}

				if (underwater) {
					// submerged track: ghost of the rails only, dashed
					float dash = 0.5f + 0.5f*(float)Math.sin(((row == 0 ? x : y)) * (float)Math.PI / 5f);
					float steel = coverage(railLines, x, y) * smoothstep(0.35f, 0.7f, dash);
					col = mix(col, rgb(58, 64, 96), 0.8f*steel);
				}
				else {
					float bl = coverage(ballast, x, y);
					if (bl > 0f) {
						float grain = noise(u, v, 32, 71);
						float [] gravel = mix(rgb(124, 116, 108), rgb(150, 142, 132), grain);
						col = mix(col, gravel, (roadCross ? 0.45f : 0.92f)*bl);
					}
					float tie = coverage(ties, x, y);
					if (tie > 0f) {
						float [] wood = mix(rgb(94, 66, 40), rgb(122, 90, 56), noise(u, v, 16, 72));
						col = mix(col, wood, (roadCross ? 0.4f : 1f)*tie);
					}
					float steel = coverage(railLines, x, y);
					if (steel > 0f) {
						col = mix(col, rgb(152, 157, 167), steel);
					}
				}

				if (roadCross) {
					float mk = smoothstep(0.48f, 0.62f, crossMark[y*N+x]);
					if (mk > 0f) {
						col = mix(col, rgb(228, 198, 40), mk);
					}
				}

				out.setRGB(x, y, pack(col));
			}
		}
		return out;
	}

	//
	// power wires
	//

	BufferedImage renderWireTile(BufferedImage src, int row)
	{
		// wires.png layout: 208+row. 0,1 = underwater cable H/V;
		// 13,14 = rail+wire crossings (RAILHPOWERV / RAILVPOWERH)
		boolean underwater = row <= 1;
		boolean railCross = row == 13 || row == 14;

		float [] water = smoothMask(classMask(src, row, WATER1, WATER2));

		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);

		if (underwater) {
			// row 0 runs north-south, row 1 east-west (matching the
			// original 16px art); twin dotted cable shadows under the
			// water, at the same positions the straight wire tiles
			// put their twin cables
			boolean horizontal = row == 1;
			float [] cables = horizontal
				? new float[] { 21.5f, 33.5f }
				: new float[] { 25.5f, 37.5f };
			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					float [] col = background(water, x, y);
					// dot period divides the tile so runs join seamlessly
					float dash = 0.5f + 0.5f*(float)Math.sin((horizontal ? x : y) * (float)Math.PI / 4f);
					for (float c : cables) {
						float dist = Math.abs((horizontal ? y : x) + 0.5f - c);
						// pale corridor so the dark dots stand out
						float halo = 1f - smoothstep(3.5f, 6.5f, dist);
						col = mix(col, rgb(168, 192, 240), 0.45f*halo);
						float band = 1f - smoothstep(2.6f, 4.0f, dist);
						col = mix(col, rgb(14, 14, 28), 0.95f * band * smoothstep(0.35f, 0.65f, dash));
					}
					out.setRGB(x, y, pack(col));
				}
			}
			return out;
		}

		if (railCross) {
			// geometric rail along one axis, wire straight across the other
			boolean railHorizontal = row == 13;
			BufferedImage ballast = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
			BufferedImage ties = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
			BufferedImage railLines = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
			strokeTrack(new TrackPath[] {
				railHorizontal ? TrackPath.straight(0, 32, 64, 32) : TrackPath.straight(32, 0, 32, 64)
				}, ballast, ties, railLines);

			for (int y = 0; y < N; y++) {
				for (int x = 0; x < N; x++) {
					float u = x / (float)N;
					float v = y / (float)N;
					float [] col = background(water, x, y);
					float bl = coverage(ballast, x, y);
					if (bl > 0f) {
						float [] gravel = mix(rgb(124, 116, 108), rgb(150, 142, 132), noise(u, v, 32, 71));
						col = mix(col, gravel, 0.92f*bl);
					}
					float tie = coverage(ties, x, y);
					if (tie > 0f) {
						float [] wood = mix(rgb(94, 66, 40), rgb(122, 90, 56), noise(u, v, 16, 72));
						col = mix(col, wood, tie);
					}
					float steel = coverage(railLines, x, y);
					if (steel > 0f) {
						col = mix(col, rgb(152, 157, 167), steel);
					}
					// twin cables crossing above, at the same positions
					// the mask-rendered straight wire tiles produce
					float [] cables = railHorizontal
						? new float[] { 25.5f, 37.5f }    // vertical wire
						: new float[] { 21.5f, 33.5f };   // horizontal wire
					for (float c : cables) {
						float band = 1f - smoothstep(1.8f, 3.4f, Math.abs((railHorizontal ? x : y) + 0.5f - c));
						col = mix(col, rgb(40, 36, 34), band);
					}
					out.setRGB(x, y, pack(col));
				}
			}
			return out;
		}

		float [] wire = smoothMask(classMask(src, row, DARK, BLACK), 1);
		float [] pole = smoothMask(classMask(src, row, WOOD, GRAY), 1);
		float [] cap = smoothMask(classMask(src, row, WHITE, LGRAY), 1);

		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float [] col = background(water, x, y);

				// faint ground shadow under the line
				float shade = smoothstep(0.15f, 0.30f, wire[y*N+x]) * (1f - smoothstep(0.30f, 0.52f, wire[y*N+x]));
				col = scale(col, 1f - 0.12f*shade);

				// the 1px cable lines peak well below 1 after mask
				// smoothing, so the threshold sits low to keep both
				// cables of the twin line fully dark
				float w = smoothstep(0.30f, 0.52f, wire[y*N+x]);
				if (w > 0f) {
					col = mix(col, rgb(40, 36, 34), w);
				}

				float p = smoothstep(0.45f, 0.62f, pole[y*N+x]);
				if (p > 0f) {
					col = mix(col, rgb(110, 82, 50), p);
				}

				float c = smoothstep(0.48f, 0.64f, cap[y*N+x]);
				if (c > 0f) {
					col = mix(col, rgb(210, 212, 218), c);
				}

				out.setRGB(x, y, pack(col));
			}
		}

		paintSpecials(out, src, row, WATER1, WATER2, DIRT, DIRT2, DARK, BLACK, GRAY, WOOD, WHITE, LGRAY);
		return out;
	}

	//
	// road+wire transparent overlay
	//

	BufferedImage renderRoadWireTile(BufferedImage src, int row)
	{
		float [] opaque = alphaMask(src, row);
		float [] wire = smoothMask(opaque, 1);

		BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				float w = smoothstep(0.30f, 0.52f, wire[y*N+x]);
				if (w > 0f) {
					out.setRGB(x, y, packA(rgb(40, 36, 34), w));
				}
			}
		}
		return out;
	}

	//
	// traffic cars
	//

	/**
	 * The traffic animation is synthesized rather than recovered from the
	 * original frames: cars run along geometric lanes matching the redrawn
	 * roads, advancing a quarter lane-length per frame so the 4-frame loop
	 * is seamless. Car colors are keyed to the car's identity (row, lane,
	 * slot), never its position, so they stay stable across frames, and
	 * drawing is clipped to each 64px cell so cars near a tile edge can't
	 * bleed into the neighboring animation frame on the sheet.
	 *
	 * Sheet layout (per 16px-basis cell): columns 0-3 = light traffic
	 * frames, 4-7 = heavy; rows match the road tile rows referenced by
	 * tiles.rc (0=H, 1=V, 4-7=curves, 12=X junction).
	 */
	static final int [] TRAFFIC_ROWS = { 0, 1, 4, 5, 6, 7, 12 };

	void generateTraffic()
		throws Exception
	{
		BufferedImage src = UpscaleArt.toArgb(ImageIO.read(new File(graphicsDir, "traffic_frames.png")));

		BufferedImage out64 = new BufferedImage(src.getWidth()*4, src.getHeight()*4,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = out64.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (int col = 0; col < 8; col++) {
			boolean heavy = col >= 4;
			int frame = col % 4;
			for (int row : TRAFFIC_ROWS) {
				Graphics2D g = (Graphics2D)gr.create();
				g.clipRect(col*N, row*N, N, N);
				g.translate(col*N, row*N);
				drawTrafficTile(g, row, frame, heavy);
				g.dispose();
			}
		}
		gr.dispose();

		BufferedImage out32 = halve(out64);
		BufferedImage out16 = halve(out32);
		write(out64, "traffic_frames_64x64.png");
		write(out32, "traffic_frames_32x32.png");
		write(out16, "traffic_frames_16x16.png");
		write(halve(out16), "traffic_frames_8x8.png");
	}

	// the original cars are all dark gray, invisible on the new dark
	// asphalt; give each a body color instead
	static final Color [] BODY_COLORS = {
		new Color(178, 44, 38),    // red
		new Color(96, 148, 222),   // blue
		new Color(224, 224, 226),  // white
		new Color(160, 166, 174),  // silver
		new Color(196, 158, 44),   // taxi yellow
		new Color(98, 176, 106),   // green
	};

	static void drawTrafficTile(Graphics2D gr, int row, int frame, boolean heavy)
	{
		Lane [] lanes = trafficLanes(row);
		int carsPerLane = heavy ? 2 : 1;
		for (int li = 0; li < lanes.length; li++) {
			Lane lane = lanes[li];
			float len = lane.length();
			float phase = hash1(row*8 + li, 17) * len;
			for (int i = 0; i < carsPerLane; i++) {
				float s = (phase + i*len/carsPerLane + frame*len/4f) % len;
				Color body = BODY_COLORS[
					(int)(hash1(row*64 + li*8 + i, 99) * BODY_COLORS.length) % BODY_COLORS.length];
				// also draw the wrapped copies: a car straddling the lane's
				// wrap point shows its other half at the opposite tile edge
				for (int wrap = -1; wrap <= 1; wrap++) {
					float [] pose = lane.place(s + wrap*len);
					drawCar(gr, pose[0], pose[1], pose[2], body);
				}
			}
		}
	}

	/**
	 * A traffic lane: a straight line or quarter arc offset LANE_OFF from
	 * the road centerline, traversed at arc-length positions s in [0,len).
	 */
	static final float LANE_OFF = 11f;

	static class Lane
	{
		boolean isArc;
		float cx, cy, r, a0;          // arc: quarter starting at angle a0
		boolean horizontal; float c;  // straight: cross-axis position
		int dir;                      // straight: sign along the axis; arc: sign of angle sweep

		float length()
		{
			return isArc ? (float)(Math.PI/2)*r : N;
		}

		/** Center point and heading (radians) for position s. */
		float [] place(float s)
		{
			if (isArc) {
				float a = dir > 0 ? a0 + s/r : a0 + (float)(Math.PI/2) - s/r;
				return new float[] {
					cx + r*(float)Math.cos(a),
					cy + r*(float)Math.sin(a),
					a + dir*(float)(Math.PI/2) };
			}
			float coord = dir > 0 ? s : N - s;
			return horizontal
				? new float[] { coord, c, dir > 0 ? 0f : (float)Math.PI }
				: new float[] { c, coord, dir*(float)(Math.PI/2) };
		}
	}

	static Lane straightLane(boolean horizontal, float c, int dir)
	{
		Lane l = new Lane();
		l.horizontal = horizontal; l.c = c; l.dir = dir;
		return l;
	}

	static Lane arcLane(float cx, float cy, float r, float a0deg, int dir)
	{
		Lane l = new Lane();
		l.isArc = true;
		l.cx = cx; l.cy = cy; l.r = r;
		l.a0 = (float)Math.toRadians(a0deg);
		l.dir = dir;
		return l;
	}

	/**
	 * Curve lanes around a corner. The inner lane (radius 32-LANE_OFF)
	 * always sweeps with increasing angle and the outer one against it;
	 * this matches the straight-lane directions (right-hand traffic) on
	 * the adjoining tiles at every corner.
	 */
	static Lane [] cornerLanes(float cx, float cy, float a0deg)
	{
		return new Lane[] {
			arcLane(cx, cy, 32f - LANE_OFF, a0deg, +1),
			arcLane(cx, cy, 32f + LANE_OFF, a0deg, -1) };
	}

	static Lane [] trafficLanes(int row)
	{
		final float IN = 32f - LANE_OFF, OUT = 32f + LANE_OFF;
		switch (row) {
		case 0:
			return new Lane[] {
				straightLane(true, IN, -1), straightLane(true, OUT, +1) };
		case 1:
			return new Lane[] {
				straightLane(false, IN, +1), straightLane(false, OUT, -1) };
		case 4: return cornerLanes(64, 0, 90);
		case 5: return cornerLanes(64, 64, 180);
		case 6: return cornerLanes(0, 64, 270);
		case 7: return cornerLanes(0, 0, 0);
		case 12:
			return new Lane[] {
				straightLane(true, IN, -1), straightLane(true, OUT, +1),
				straightLane(false, IN, +1), straightLane(false, OUT, -1) };
		default:
			return new Lane[0];
		}
	}

	/**
	 * Draws a small shaded car centered at (px,py), pointing at
	 * {@code heading}. The length matches the original art's cars: with
	 * the fixed 16px-per-frame animation step, anything shorter than
	 * ~20px leaves a gap between successive positions and the motion
	 * reads as jumps instead of driving.
	 */
	static void drawCar(Graphics2D gr, float px, float py, float heading, Color color)
	{
		final float len = 20f, wid = 9f;
		Graphics2D g = (Graphics2D)gr.create();
		g.translate(px, py);
		g.rotate(heading);

		// shadow
		g.setColor(new Color(0, 0, 0, 90));
		g.fill(new RoundRectangle2D.Float(-len/2+1f, -wid/2+1f, len, wid, 3, 3));
		// body
		g.setColor(color);
		g.fill(new RoundRectangle2D.Float(-len/2, -wid/2, len, wid, 3, 3));
		// roof highlight
		g.setColor(color.brighter());
		g.fill(new RoundRectangle2D.Float(-len*0.21f, -wid*0.31f, len*0.42f, wid*0.62f, 2, 2));
		// windshield ahead of the roof
		g.setColor(new Color(40, 52, 72));
		g.fill(new Rectangle2D.Float(len*0.14f, -wid/2+1f, 2.2f, wid-2f));
		g.dispose();
	}
}
