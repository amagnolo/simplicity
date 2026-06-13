package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;

/**
 * Redraws the in-use misc_animation tiles for the deluxe skin and
 * patches them into the upscaled sheet: the blinking power-out
 * indicator, the eight raised-drawbridge tiles (matching the procedural
 * road-bridge palette from GenerateOverlayArt), the eight-frame zone
 * destruction explosion, the airport radar sweep (DeluxePortArt) and
 * the nuclear reactor swirl (DeluxePowerArt). Everything else in the
 * sheet — the fountain is restored separately, the rest is unused —
 * keeps the upscaled art.
 *
 * Patch rows are in 16px-basis units, matching tiles.rc and the .ani
 * frame coordinates.
 */
final class DeluxeMiscArt
{
	static final int ROW_BOLT = 0;
	static final int ROW_HBRDG = 16;      // 4 tiles
	static final int ROW_RADAR = 80;      // 8 frames
	static final int ROW_DESTRUCT = 528;  // 8 frames
	static final int ROW_VBRDG = 1936;    // 4 tiles
	static final int ROW_SWIRL = 2000;    // 4 frames

	/** The patched regions, {16px-basis row, tile count} each. */
	static final int [][] REGIONS = {
		{ ROW_BOLT, 1 }, { ROW_HBRDG, 4 }, { ROW_RADAR, 8 },
		{ ROW_DESTRUCT, 8 }, { ROW_VBRDG, 4 }, { ROW_SWIRL, 4 },
	};

	private DeluxeMiscArt()
	{
	}

	/** Preview harness: renders the redrawn tiles to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"));
		BufferedImage strip = new BufferedImage(17*T, T, BufferedImage.TYPE_INT_ARGB);
		paste(strip, renderBolt(), 0, 0);
		for (int k = 0; k < 4; k++) {
			paste(strip, renderBridgeTile(true, k), (1+k)*T, 0);
		}
		for (int k = 0; k < 4; k++) {
			paste(strip, renderBridgeTile(false, k), (5+k)*T, 0);
		}
		for (int f = 0; f < 8; f++) {
			paste(strip, renderDestructFrame(f), (9+f)*T, 0);
		}
		javax.imageio.ImageIO.write(strip, "png",
			new java.io.File(dir, "misc_tiles_preview.png"));
		System.out.println("Previews in "+dir);
	}

	/**
	 * Pastes the redrawn regions, halved twice, over the modern 16px
	 * sheet — the small deluxe sizes keep the de-dithered art except
	 * where tiles were redrawn.
	 */
	static BufferedImage patchSmall(BufferedImage base16, BufferedImage sheet64)
	{
		BufferedImage out = copy(base16);
		for (int [] region : REGIONS) {
			BufferedImage part = sheet64.getSubimage(0, region[0]*4,
				sheet64.getWidth(), region[1]*T);
			paste(out, halve(halve(part)), 0, region[0]);
		}
		return out;
	}

	/** Pastes all redrawn tiles into the 64px-basis misc_animation sheet. */
	static void patch(BufferedImage sheet64)
	{
		paste(sheet64, renderBolt(), 0, ROW_BOLT*4);
		for (int k = 0; k < 4; k++) {
			paste(sheet64, renderBridgeTile(true, k), 0, (ROW_HBRDG + k*16)*4);
			paste(sheet64, renderBridgeTile(false, k), 0, (ROW_VBRDG + k*16)*4);
		}
		paste(sheet64, DeluxePortArt.renderRadarFrames(), 0, ROW_RADAR*4);
		for (int f = 0; f < 8; f++) {
			paste(sheet64, renderDestructFrame(f), 0, (ROW_DESTRUCT + f*16)*4);
		}
		paste(sheet64, DeluxePowerArt.renderNuclearSwirl(), 0, ROW_SWIRL*4);
	}

	//
	// power-out indicator
	//

	/**
	 * The blinking no-power tile: concrete pad, red frame, the single
	 * red diagonal and the glossy yellow bolt — the classic warning,
	 * smooth.
	 */
	static BufferedImage renderBolt()
	{
		BufferedImage img = new BufferedImage(T, T, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < T; y++) {
			for (int x = 0; x < T; x++) {
				float lum = 1f + 0.08f*(hash2(x, y, 77) - 0.5f);
				img.setRGB(x, y, pack(scale(rgb(148, 148, 152), lum)));
			}
		}
		Graphics2D gr = gfx(img);
		// the single red diagonal, as in the classic art
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(7f));
		gr.setColor(new Color(200, 40, 32));
		gr.draw(new Line2D.Float(3, 3, T-3, T-3));
		gr.setStroke(saved);
		// red frame
		gr.setColor(new Color(214, 48, 38));
		gr.fill(new Rectangle2D.Float(0, 0, T, 5));
		gr.fill(new Rectangle2D.Float(0, T-5, T, 5));
		gr.fill(new Rectangle2D.Float(0, 0, 5, T));
		gr.fill(new Rectangle2D.Float(T-5, 0, 5, T));
		gr.setColor(new Color(140, 26, 22));
		gr.draw(new Rectangle2D.Float(4.5f, 4.5f, T-9, T-9));
		// the bolt
		DeluxePowerArt.boltGlyph(gr, T/2f, T/2f, 5.5f, new Color(246, 210, 56));
		gr.setColor(new Color(255, 245, 170));
		gr.fill(new Polygon(
			new int [] { T/2+6, T/2-8, T/2-4 },
			new int [] { T/2-24, T/2-7, T/2-7 }, 3));
		gr.dispose();
		return img;
	}

	//
	// raised drawbridge tiles
	//

	/**
	 * One raised-drawbridge tile, 64px. The engine arranges the open
	 * horizontal bridge as HBRDG0 = west deck end with its leaf HBRDG1
	 * in the tile above, two tiles of open water, then HBRDG2 = east
	 * deck end with HBRDG3 above it; the vertical bridge as VBRDG0 =
	 * north deck end with VBRDG1 in the tile to its east, the open
	 * water, then VBRDG2 = south deck end with VBRDG3 east of it.
	 *
	 * Each pair is rendered once on a two-tile canvas — the deck stub
	 * with its bascule piers in the deck-end tile, and the raised leaf
	 * sweeping up across both tiles from its hinge — then sliced, so
	 * the plate connects seamlessly to the machinery that lifts it.
	 * Deck geometry and palette match the procedural road bridges
	 * from GenerateOverlayArt.
	 */
	static BufferedImage renderBridgeTile(boolean horizontal, int which)
	{
		boolean far = which >= 2;
		boolean leaf = which % 2 == 1;
		BufferedImage pair = horizontal ? renderHBridgePair(far) : renderVBridgePair(far);
		int x0 = !horizontal && leaf ? T : 0;
		int y0 = horizontal && !leaf ? T : 0;
		return copy(pair.getSubimage(x0, y0, T, T));
	}

	/** Open water over the whole image, matching the bridge tiles. */
	static void fillWater(BufferedImage img)
	{
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				img.setRGB(x, y, pack(waterTexture(
					(x % T)/(float)T, (y % T)/(float)T, 0.9f)));
			}
		}
	}

	/**
	 * The horizontal pair, 64x128: deck-end tile in the lower half
	 * (the bridge continues off its west or east edge), the raised
	 * leaf reaching into the tile above. The west leaf shows its road
	 * face to the viewer, the east one its structural underside.
	 */
	static BufferedImage renderHBridgePair(boolean east)
	{
		BufferedImage img = new BufferedImage(T, 2*T, BufferedImage.TYPE_INT_ARGB);
		fillWater(img);
		Graphics2D gr = gfx(img);
		if (!east) {
			hDeckStub(gr, 0, 24, T);
			pierBlock(gr, 16, T+2, 14, 15);
			pierBlock(gr, 16, T+47, 14, 15);
			leafPlate(gr, 24, T+9, 24, T+55, 14, -72, true);
		}
		else {
			hDeckStub(gr, 40, T, T);
			pierBlock(gr, 34, T+2, 14, 15);
			pierBlock(gr, 34, T+47, 14, 15);
			leafPlate(gr, 40, T+9, 40, T+55, -14, -72, false);
		}
		gr.dispose();
		return img;
	}

	/**
	 * The vertical pair, 128x64: deck-end tile in the left half (the
	 * bridge continues off its north or south edge), the raised leaf
	 * reaching into the tile to the east. The south leaf shows its
	 * road face, the north one its underside.
	 */
	static BufferedImage renderVBridgePair(boolean south)
	{
		BufferedImage img = new BufferedImage(2*T, T, BufferedImage.TYPE_INT_ARGB);
		fillWater(img);
		Graphics2D gr = gfx(img);
		if (!south) {
			vDeckStub(gr, 0, 40);
			pierBlock(gr, 0, 33, 15, 14);
			pierBlock(gr, 49, 33, 15, 14);
			leafPlate(gr, 9, 40, 55, 40, 44, -32, false);
		}
		else {
			vDeckStub(gr, 40, T);
			pierBlock(gr, 0, 33, 15, 14);
			pierBlock(gr, 49, 33, 15, 14);
			leafPlate(gr, 9, 40, 55, 40, 44, -32, true);
		}
		gr.dispose();
		return img;
	}

	/** A horizontal deck stub from x0 to x1, in the tile at yOff. */
	static void hDeckStub(Graphics2D gr, float x0, float x1, int yOff)
	{
		float w = x1 - x0;
		// shadow on the water south of the deck
		gr.setColor(new Color(16, 20, 56, 115));
		gr.fill(new Rectangle2D.Float(x0+2, yOff+60, w, 5));
		gr.setColor(new Color(74, 74, 78));
		gr.fill(new Rectangle2D.Float(x0, yOff+5, w, 54));
		gr.setColor(new Color(26, 26, 30));
		gr.fill(new Rectangle2D.Float(x0, yOff+5, w, 2));
		gr.fill(new Rectangle2D.Float(x0, yOff+57, w, 2));
		// center dashes, in the same phase as the closed bridge
		gr.setColor(new Color(235, 235, 230));
		for (int x = 0; x < T; x += 16) {
			float a = Math.max(x, x0), b = Math.min(x+8, x1);
			if (b > a) {
				gr.fill(new Rectangle2D.Float(a, yOff+31, b-a, 2));
			}
		}
	}

	/** A vertical deck stub from y0 to y1, in the left tile. */
	static void vDeckStub(Graphics2D gr, float y0, float y1)
	{
		float h = y1 - y0;
		// shadow on the water east of the deck
		gr.setColor(new Color(16, 20, 56, 115));
		gr.fill(new Rectangle2D.Float(60, y0+2, 5, h));
		gr.setColor(new Color(74, 74, 78));
		gr.fill(new Rectangle2D.Float(5, y0, 54, h));
		gr.setColor(new Color(26, 26, 30));
		gr.fill(new Rectangle2D.Float(5, y0, 2, h));
		gr.fill(new Rectangle2D.Float(57, y0, 2, h));
		gr.setColor(new Color(235, 235, 230));
		for (int y = 0; y < T; y += 16) {
			float a = Math.max(y, y0), b = Math.min(y+8, y1);
			if (b > a) {
				gr.fill(new Rectangle2D.Float(31, a, 2, b-a));
			}
		}
	}

	/** A concrete bascule pier standing in the water, lit from NW. */
	static void pierBlock(Graphics2D gr, float x, float y, float w, float h)
	{
		gr.setColor(new Color(16, 20, 56, 110));
		gr.fill(new Rectangle2D.Float(x+2, y+2, w, h));
		gr.setColor(new Color(96, 98, 106));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		gr.setColor(new Color(146, 148, 156));
		gr.fill(new Rectangle2D.Float(x, y, w, 2));
		gr.fill(new Rectangle2D.Float(x, y, 2, h));
		gr.setColor(new Color(52, 54, 62));
		gr.fill(new Rectangle2D.Float(x, y+h-2, w, 2));
		gr.fill(new Rectangle2D.Float(x+w-2, y, 2, h));
	}

	/**
	 * One raised bascule leaf in the map's oblique projection: a
	 * rigid plate projects to a (near-)parallelogram, so the tip edge
	 * stays parallel to the hinge edge (hx0,hy0)-(hx1,hy1), displaced
	 * by (dx,dy) as the leaf swings up and only slightly narrowed.
	 * Shows the road surface with the dashed center line when
	 * {@code roadFace} (the viewer sees the deck top), the ribbed
	 * gray underside otherwise; both get edge girders, a drop shadow
	 * and the red-and-white tip marking.
	 */
	static void leafPlate(Graphics2D gr, float hx0, float hy0, float hx1, float hy1,
		float dx, float dy, boolean roadFace)
	{
		final float taper = 0.92f;
		float mx = (hx0+hx1)/2f, my = (hy0+hy1)/2f;
		float tcx = mx + dx, tcy = my + dy;
		float [] h0 = { hx0, hy0 };
		float [] h1 = { hx1, hy1 };
		float [] t0 = { tcx + (hx0-mx)*taper, tcy + (hy0-my)*taper };
		float [] t1 = { tcx + (hx1-mx)*taper, tcy + (hy1-my)*taper };

		Path2D.Float plate = plateBand(h0, h1, t0, t1, 0f, 1f);

		// drop shadow on the water (and the deck below the hinge)
		AffineTransform old = gr.getTransform();
		gr.translate(6, 8);
		gr.setColor(new Color(0, 0, 40, 80));
		gr.fill(plate);
		gr.setTransform(old);

		gr.setColor(roadFace ? new Color(74, 74, 78) : new Color(96, 98, 104));
		gr.fill(plate);
		// the raised plate catches the light toward the tip
		gr.setColor(new Color(255, 255, 255, 28));
		gr.fill(plateBand(h0, h1, t0, t1, 0.55f, 1f));
		// dark band against the hinge machinery
		gr.setColor(new Color(40, 42, 48));
		gr.fill(plateBand(h0, h1, t0, t1, 0f, 0.05f));

		Stroke saved = gr.getStroke();
		if (roadFace) {
			// dashed center line climbing the plate
			gr.setColor(new Color(235, 235, 230, 235));
			gr.setStroke(new BasicStroke(2.6f));
			for (float t = 0.12f; t < 0.84f; t += 0.24f) {
				gr.draw(new Line2D.Float(
					mx + (tcx-mx)*t, my + (tcy-my)*t,
					mx + (tcx-mx)*(t+0.10f), my + (tcy-my)*(t+0.10f)));
			}
		}
		else {
			// structural ribs, parallel to the hinge
			gr.setColor(new Color(0, 0, 0, 50));
			for (float t = 0.15f; t < 0.90f; t += 0.15f) {
				gr.draw(new Line2D.Float(
					lerp(h0[0], t0[0], t), lerp(h0[1], t0[1], t),
					lerp(h1[0], t1[0], t), lerp(h1[1], t1[1], t)));
			}
		}
		// edge girders
		gr.setStroke(new BasicStroke(2.4f));
		gr.setColor(new Color(44, 46, 52));
		gr.draw(new Line2D.Float(h0[0], h0[1], t0[0], t0[1]));
		gr.draw(new Line2D.Float(h1[0], h1[1], t1[0], t1[1]));
		gr.setStroke(saved);
		// red-and-white tip marking
		gr.setColor(new Color(214, 48, 38));
		gr.fill(plateBand(h0, h1, t0, t1, 0.90f, 1f));
		gr.setColor(new Color(238, 238, 240));
		gr.fill(new Ellipse2D.Float(tcx-2, tcy-2, 4, 4));
	}

	static float lerp(float a, float b, float t)
	{
		return a + (b-a)*t;
	}

	/** The plate's quad between fractions t0..t1 of the hinge-tip run. */
	static Path2D.Float plateBand(float [] h0, float [] h1, float [] t0, float [] t1,
		float f0, float f1)
	{
		Path2D.Float p = new Path2D.Float();
		p.moveTo(lerp(h0[0], t0[0], f0), lerp(h0[1], t0[1], f0));
		p.lineTo(lerp(h0[0], t0[0], f1), lerp(h0[1], t0[1], f1));
		p.lineTo(lerp(h1[0], t1[0], f1), lerp(h1[1], t1[1], f1));
		p.lineTo(lerp(h1[0], t1[0], f0), lerp(h1[1], t1[1], f0));
		p.closePath();
		return p;
	}

	//
	// zone destruction
	//

	/**
	 * One frame of the eight-frame zone destruction: the blast flash
	 * grows into a fireball with flying debris (frames 0-3), then
	 * collapses into churning smoke that thins out over the scorched
	 * ground (frames 4-7).
	 */
	static BufferedImage renderDestructFrame(int f)
	{
		BufferedImage img = new BufferedImage(T, T, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < T; y++) {
			for (int x = 0; x < T; x++) {
				img.setRGB(x, y, pack(landTexture(x/(float)T, y/(float)T)));
			}
		}
		Graphics2D gr = gfx(img);
		float cx = T/2f, cy = T/2f;

		// the ground chars from frame 2 on
		if (f >= 2) {
			float burn = Math.min(1f, (f-1)/3f);
			gr.setColor(new Color(20, 16, 14, Math.round(150*burn)));
			gr.fill(new Ellipse2D.Float(cx-26, cy-22, 52, 44));
		}

		if (f < 4) {
			// expanding fireball
			float r = 7 + f*7.5f;
			gr.setColor(new Color(140, 30, 16, 220));
			gr.fill(blob(cx, cy, r+4, f*31));
			gr.setColor(new Color(228, 92, 24));
			gr.fill(blob(cx, cy, r, f*31+1));
			gr.setColor(new Color(252, 176, 48));
			gr.fill(blob(cx-r*0.12f, cy-r*0.15f, r*0.62f, f*31+2));
			gr.setColor(new Color(255, 240, 190));
			gr.fill(blob(cx-r*0.18f, cy-r*0.2f, r*0.30f, f*31+3));
			// flying debris
			for (int k = 0; k < 4+f*3; k++) {
				double a = Math.PI*2*hash2(k, f, 5);
				float d = (r+3) * (0.8f + 0.6f*hash2(k, f, 6));
				gr.setColor(hash2(k, f, 7) < 0.5f
					? new Color(60, 50, 44) : new Color(244, 150, 60));
				float px = cx + (float)Math.cos(a)*d;
				float py = cy + (float)Math.sin(a)*d*0.9f;
				gr.fill(new Ellipse2D.Float(px-1.5f, py-1.5f, 3, 3));
			}
		}
		else {
			// churning smoke thinning out
			float age = (f-4) / 3f;             // 0 .. 1
			float r = 24 + 8*age;
			int a = Math.round(195*(1f-age*0.88f));
			gr.setColor(new Color(44, 42, 42, a));
			gr.fill(blob(cx, cy-4-6*age, r, f*31));
			gr.setColor(new Color(84, 80, 78, Math.round(a*0.9f)));
			gr.fill(blob(cx-6, cy-10-8*age, r*0.6f, f*31+1));
			gr.setColor(new Color(130, 124, 120, Math.round(a*0.7f)));
			gr.fill(blob(cx+7, cy-12-10*age, r*0.45f, f*31+2));
			if (f == 4) {
				// last embers under the smoke
				gr.setColor(new Color(240, 120, 40, 200));
				gr.fill(blob(cx-4, cy+8, 7, 9));
				gr.fill(blob(cx+9, cy+4, 5, 10));
			}
			// debris settled on the ground
			for (int k = 0; k < 10; k++) {
				double ang = Math.PI*2*hash2(k, 4, 5);
				float d = 20 * (0.8f + 0.6f*hash2(k, 4, 6));
				gr.setColor(new Color(58, 52, 48));
				gr.fill(new Ellipse2D.Float(
					cx + (float)Math.cos(ang)*d - 2,
					cy + (float)Math.sin(ang)*d*0.9f - 1.5f, 4, 3));
			}
		}
		gr.dispose();
		return img;
	}

	/** A lumpy 8-point blob, livelier than a plain ellipse. */
	static Shape blob(float cx, float cy, float r, int seed)
	{
		Path2D.Float p = new Path2D.Float();
		for (int k = 0; k <= 8; k++) {
			double a = Math.PI*2*k/8;
			float rr = r * (0.82f + 0.36f*hash1(k % 8, seed));
			float px = cx + (float)Math.cos(a)*rr;
			float py = cy + (float)Math.sin(a)*rr;
			if (k == 0) p.moveTo(px, py);
			else p.lineTo(px, py);
		}
		p.closePath();
		return p;
	}
}
