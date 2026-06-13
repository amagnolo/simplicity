package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;

/**
 * Draws the deluxe fire station and police station from scratch at 64px
 * per tile, and hosts the small kit shared by the other civic redraws:
 * the painted ring border in any color (the civic counterpart of the
 * industrial yellow safety ring) and the big rooftop letters ("FD",
 * "PD") that keep the classic at-a-glance identity. Sun from the
 * northwest: shadows fall diagonally to the southeast.
 */
final class DeluxeCivicArt
{
	static final Color RING_RED = new Color(196, 44, 36);
	static final Color RING_BLUE = new Color(58, 86, 196);
	static final Color RING_GREEN = new Color(40, 148, 44);
	static final Color LETTER_YELLOW = new Color(246, 210, 56);

	// beacon lamp positions, shared between the block renderers and the
	// light animation frames so the glow sits exactly on the fixture.
	// The fire beacon lives in tile (2,0), the police pair in tile (1,0).
	static final Point FIRE_BEACON = new Point(167, 21);
	static final Point POLICE_BEACON_R = new Point(107, 44);
	static final Point POLICE_BEACON_B = new Point(115, 44);

	private DeluxeCivicArt()
	{
	}

	/** Preview harness: renders both stations to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"));
		javax.imageio.ImageIO.write(renderFireStation(), "png",
			new java.io.File(dir, "firestation_preview.png"));
		javax.imageio.ImageIO.write(renderPoliceStation(), "png",
			new java.io.File(dir, "police_preview.png"));
		System.out.println("Previews in "+dir);
	}

	//
	// shared civic kit
	//

	/**
	 * The painted ring border around a civic block, like the industrial
	 * yellow safety ring but in the building's identity color: worn
	 * paint, expansion seams, sunlit outer edge and curb shadow.
	 */
	static void paintRing(BufferedImage img, Color base, int seed)
	{
		int n = img.getWidth();
		final int W = 6;
		float [] paint = rgb(base.getRed(), base.getGreen(), base.getBlue());
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int d = Math.min(Math.min(x, n-1-x), Math.min(y, n-1-y));
				if (d >= W) continue;
				float u = (x % T) / (float)T;
				float v = (y % T) / (float)T;
				float lum = 0.92f + 0.14f*noise(u, v, 24, seed+4);
				float [] col = scale(paint, lum);
				if (noise(u, v, 16, seed+9) > 0.80f) {
					col = mix(col, rgb(150, 146, 138), 0.5f);   // worn paint
				}
				boolean onNS = y < W || y > n-1-W;
				boolean onEW = x < W || x > n-1-W;
				if ((onNS && x % 16 == 0) || (onEW && y % 16 == 0)) {
					col = scale(col, 0.80f);   // expansion seam
				}
				if (d == 0) col = scale(col, 1.12f);     // sunlit outer edge
				if (d == W-1) col = scale(col, 0.62f);   // curb shadow
				img.setRGB(x, y, pack(col));
			}
		}
	}

	static final String [] GLYPH_F = {
		"XXXXX",
		"X....",
		"X....",
		"XXXX.",
		"X....",
		"X....",
		"X....",
	};

	static final String [] GLYPH_D = {
		"XXXX.",
		"X...X",
		"X...X",
		"X...X",
		"X...X",
		"X...X",
		"XXXX.",
	};

	static final String [] GLYPH_P = {
		"XXXX.",
		"X...X",
		"X...X",
		"XXXX.",
		"X....",
		"X....",
		"X....",
	};

	/**
	 * Big yellow letters painted on a roof, with a soft dark halo so they
	 * stay readable after downscaling — the classic "FD"/"PD" rooftop ID.
	 */
	static void roofLetters(Graphics2D gr, String [][] glyphs, int cx, int cy, int scale)
	{
		int gw = 5*scale, gap = 2*scale;
		int total = glyphs.length*gw + (glyphs.length-1)*gap;
		int x = cx - total/2;
		int y = cy - 7*scale/2;
		for (String [] g : glyphs) {
			glyph(gr, g, x+1, y+1, scale, new Color(0, 0, 0, 110));
			glyph(gr, g, x, y, scale, LETTER_YELLOW);
			x += gw + gap;
		}
	}

	/** A concrete pad with seams, for aprons and forecourts. */
	static void pad(Graphics2D gr, float x, float y, float w, float h, int seed)
	{
		gr.setColor(new Color(166, 164, 158));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		gr.setColor(new Color(0, 0, 0, 22));
		for (float j = y+12; j < y+h-2; j += 16) {
			gr.fill(new Rectangle2D.Float(x, j, w, 1));
		}
		for (float i = x+12; i < x+w-2; i += 16) {
			gr.fill(new Rectangle2D.Float(i, y, 1, h));
		}
		gr.setColor(new Color(255, 255, 255, 26));
		gr.fill(new Rectangle2D.Float(x, y, w, 1));
		gr.fill(new Rectangle2D.Float(x, y, 1, h));
	}

	//
	// fire station
	//

	/**
	 * Fire station, 3x3: a red-brick engine house with three white
	 * roller-door bays opening onto a concrete apron, the big yellow
	 * "FD" on the gray roof, a hose-drying tower with the alarm bell,
	 * and an engine rolling out.
	 */
	static BufferedImage renderFireStation()
	{
		int seed = 5100;
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, LAWN, seed);
		paintRing(img, RING_RED, seed);

		Graphics2D gr = gfx(img);

		// apron from the bays to the street, with bay guide lines
		pad(gr, 34, 118, 124, 68, seed);
		gr.setColor(new Color(222, 196, 60, 170));
		for (int k = 0; k < 2; k++) {
			gr.fill(new Rectangle2D.Float(74 + k*42, 120, 2, 64));
		}

		Color brick = new Color(172, 56, 44);
		Color brickDark = new Color(118, 38, 30);
		Color roof = new Color(152, 150, 152);

		// engine house: gray flat roof over red brick walls
		int x = 22, y = 34, w = 148, h = 64, hgt = 22;
		shadow(gr, x, y, w, h, hgt);
		// south wall with the three bays
		gr.setColor(brick);
		gr.fill(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setColor(new Color(255, 255, 255, 30));
		gr.fill(new Rectangle2D.Float(x, y+h, w, 2));
		gr.setColor(brickDark);
		gr.fill(new Rectangle2D.Float(x, y+h+hgt-2, w, 2));
		// brick courses
		gr.setColor(new Color(0, 0, 0, 28));
		for (int j = y+h+4; j < y+h+hgt-2; j += 4) {
			gr.fill(new Rectangle2D.Float(x, j, w, 1));
		}
		// three white roller doors with red lintels
		for (int k = 0; k < 3; k++) {
			float dx = 38 + k*42, dw = 30;
			gr.setColor(brickDark);
			gr.fill(new Rectangle2D.Float(dx-2, y+h+2, dw+4, hgt-4));
			gr.setColor(new Color(228, 228, 230));
			gr.fill(new Rectangle2D.Float(dx, y+h+4, dw, hgt-7));
			gr.setColor(new Color(0, 0, 0, 45));
			for (int j = 0; j < 4; j++) {
				gr.fill(new Rectangle2D.Float(dx, y+h+6+j*4, dw, 1));
			}
			gr.setColor(new Color(196, 44, 36));
			gr.fill(new Rectangle2D.Float(dx-2, y+h+2, dw+4, 2));
		}

		// roof with parapet, grain and the FD letters
		gr.setColor(mixc(roof, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		gr.setColor(roof);
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, h-2));
		gr.setColor(mixc(roof, Color.WHITE, 0.25f));
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, 2));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 2, h-2));
		roofGrain(img, x+2, y+2, w-4, h-4, seed);
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+26, y+10, 13, 9));
		gr.setColor(mixc(roof, Color.BLACK, 0.18f));
		gr.fill(new Rectangle2D.Float(x+24, y+8, 13, 9));
		roofLetters(gr, new String [][] { GLYPH_F, GLYPH_D }, x+w/2, y+h/2, 5);

		// hose-drying tower on the northeast corner, with the alarm bell;
		// no long ground shadow — it stands against the tall engine house,
		// so just a short offset shade
		int tx = 146, ty = 16, tw = 26;
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Rectangle2D.Float(tx+3, ty+4, tw, 18));
		gr.setColor(brickDark);
		gr.fill(new Rectangle2D.Float(tx-1, ty-1, tw+2, 20));
		gr.setColor(brick);
		gr.fill(new Rectangle2D.Float(tx, ty, tw, 18));
		gr.setColor(mixc(brick, Color.WHITE, 0.25f));
		gr.fill(new Rectangle2D.Float(tx, ty, tw, 2));
		gr.fill(new Rectangle2D.Float(tx, ty, 2, 18));
		gr.setColor(new Color(226, 188, 80));
		gr.fill(new Ellipse2D.Float(tx+tw/2f-4, ty+6, 8, 7));
		gr.setColor(new Color(150, 116, 40));
		gr.draw(new Ellipse2D.Float(tx+tw/2f-4, ty+6, 8, 7));
		// the alarm beacon the light animation glows from
		beaconFixture(gr, FIRE_BEACON.x, FIRE_BEACON.y, new Color(150, 30, 24));

		// the engine rolling out of the middle bay
		fireEngine(gr, 80, 132);

		// hydrant by the apron
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Ellipse2D.Float(24, 168, 7, 5));
		gr.setColor(new Color(206, 52, 40));
		gr.fill(new Ellipse2D.Float(22, 162, 8, 9));
		gr.setColor(new Color(244, 120, 100));
		gr.fill(new Ellipse2D.Float(23.5f, 163.5f, 3, 3));

		bush(gr, 174, 130, seed+6);
		bush(gr, 20, 108, seed+7);
		gr.dispose();
		return img;
	}

	/** A fire engine seen from above, nose south: red body, white ladder. */
	static void fireEngine(Graphics2D gr, float x, float y)
	{
		float w = 18, h = 38;
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new RoundRectangle2D.Float(x+2, y+3, w, h, 4, 4));
		gr.setColor(new Color(196, 36, 30));
		gr.fill(new RoundRectangle2D.Float(x, y, w, h, 4, 4));
		// cab windshield at the south nose
		gr.setColor(new Color(52, 62, 80));
		gr.fill(new Rectangle2D.Float(x+2, y+h-8, w-4, 4));
		gr.setColor(new Color(240, 240, 244));
		// ladder along the body
		gr.fill(new Rectangle2D.Float(x+w/2-2.5f, y+3, 5, h-16));
		gr.setColor(new Color(150, 150, 156));
		for (float j = y+5; j < y+h-15; j += 4) {
			gr.fill(new Rectangle2D.Float(x+w/2-2.5f, j, 5, 1));
		}
		// beacon
		gr.setColor(new Color(255, 210, 90));
		gr.fill(new Ellipse2D.Float(x+w/2-1.5f, y+h-7, 3, 3));
	}

	//
	// police station
	//

	/**
	 * Police station, 3x3: a bright blue precinct house with the yellow
	 * "PD" on the roof, a columned entrance with a blue lamp, the
	 * rooftop wig-wag beacon the light animation glows from, and a
	 * cruiser lot on the east side.
	 */
	static BufferedImage renderPoliceStation()
	{
		int seed = 5200;
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, LAWN, seed);
		paintRing(img, RING_BLUE, seed);

		Graphics2D gr = gfx(img);

		// front walk to the street
		path(gr, 64, 124, 18, B-124);

		Color roof = new Color(70, 108, 224);
		Color wall = new Color(46, 68, 150);

		// cruiser lot on the east side, paved before the precinct house
		// so the building's shadow falls across it
		pad(gr, 134, 92, 50, 72, seed);
		gr.setColor(new Color(222, 222, 226, 190));
		for (int k = 0; k < 2; k++) {
			gr.fill(new Rectangle2D.Float(148 + k*18, 96, 1, 24));
		}
		cruiser(gr, 138, 98);
		cruiser(gr, 156, 100);
		cruiser(gr, 146, 134);

		// precinct house
		slab(gr, img, 18, 32, 112, 70, 3, roof, wall, seed, 1);
		roofLetters(gr, new String [][] { GLYPH_P, GLYPH_D }, 18+112/2, 32+70/2, 4);

		// columned entrance porch with the blue lamp
		gr.setColor(new Color(212, 212, 216));
		gr.fill(new Rectangle2D.Float(58, 118, 30, 6));
		gr.setColor(new Color(160, 160, 168));
		gr.fill(new Rectangle2D.Float(58, 123, 30, 2));
		gr.setColor(new Color(232, 232, 236));
		gr.fill(new Rectangle2D.Float(60, 112, 3, 8));
		gr.fill(new Rectangle2D.Float(83, 112, 3, 8));
		gr.setColor(new Color(70, 130, 246));
		gr.fill(new Ellipse2D.Float(52, 116, 5, 5));
		gr.setColor(new Color(160, 200, 255));
		gr.fill(new Ellipse2D.Float(53, 117, 2, 2));

		// the rooftop wig-wag beacon: red and blue lamps on a bar
		beaconFixture(gr, POLICE_BEACON_R.x, POLICE_BEACON_R.y, new Color(150, 30, 24));
		beaconFixture(gr, POLICE_BEACON_B.x, POLICE_BEACON_B.y, new Color(34, 52, 140));

		// flag pole by the walk
		checkFlagBlue(gr, 46, 130);

		bush(gr, 22, 116, seed+5);
		bush(gr, 110, 168, seed+6);
		tree(gr, 30, 156, 12, seed+7);
		tree(gr, 158, 36, 13, seed+8);
		bush(gr, 140, 60, seed+9);
		gr.dispose();
		return img;
	}

	/** A black-and-white police cruiser, nose south, with the light bar. */
	static void cruiser(Graphics2D gr, float x, float y)
	{
		float w = 11, h = 20;
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new RoundRectangle2D.Float(x+1, y+2, w, h, 3, 3));
		gr.setColor(new Color(232, 232, 236));
		gr.fill(new RoundRectangle2D.Float(x, y, w, h, 3, 3));
		gr.setColor(new Color(34, 36, 42));
		gr.fill(new Rectangle2D.Float(x+1, y+2, w-2, 4));
		gr.fill(new Rectangle2D.Float(x+1, y+h-6, w-2, 4));
		gr.setColor(new Color(52, 62, 80));
		gr.fill(new Rectangle2D.Float(x+2, y+h*0.35f, w-4, h*0.3f));
		gr.setColor(new Color(220, 60, 50));
		gr.fill(new Rectangle2D.Float(x+2, y+h/2-1, 3, 2));
		gr.setColor(new Color(70, 110, 240));
		gr.fill(new Rectangle2D.Float(x+w-5, y+h/2-1, 3, 2));
	}

	/** A blue station flag on a pole (the police take on checkFlag). */
	static void checkFlagBlue(Graphics2D gr, float x, float y)
	{
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 1.5f, 16));
		gr.setColor(new Color(206, 208, 214));
		gr.fill(new Rectangle2D.Float(x, y-2, 2, 18));
		gr.setColor(new Color(58, 86, 196));
		gr.fill(new Polygon(
			new int [] { (int)x+2, (int)x+14, (int)x+2 },
			new int [] { (int)y-2, (int)y+1, (int)y+6 }, 3));
	}

	//
	// station light animations
	//

	/** The beacon lamp fixture: a small base with a dark, unlit dome. */
	static void beaconFixture(Graphics2D gr, float cx, float cy, Color off)
	{
		gr.setColor(new Color(50, 52, 56));
		gr.fill(new Rectangle2D.Float(cx-3.5f, cy+1.5f, 7, 3));
		gr.setColor(off);
		gr.fill(new Ellipse2D.Float(cx-3, cy-3, 6, 6));
		gr.setColor(new Color(255, 255, 255, 90));
		gr.fill(new Ellipse2D.Float(cx-2, cy-2, 2.5f, 2.5f));
	}

	/** One beacon lamp lit up: a soft halo around a hot core. */
	static void beaconGlow(Graphics2D gr, float cx, float cy, Color c, float intensity)
	{
		if (intensity <= 0f) return;
		gr.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(),
			Math.round(95*intensity)));
		gr.fill(new Ellipse2D.Float(cx-14, cy-14, 28, 28));
		gr.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(),
			Math.round(185*intensity)));
		gr.fill(new Ellipse2D.Float(cx-7.5f, cy-7.5f, 15, 15));
		gr.setColor(new Color(
			Math.min(255, c.getRed()+130), Math.min(255, c.getGreen()+130),
			Math.min(255, c.getBlue()+130), Math.round(245*intensity)));
		gr.fill(new Ellipse2D.Float(cx-4, cy-4, 8, 8));
	}

	/**
	 * The fire station light frames: 4 one-block cells, transparent but
	 * for the alarm beacon on the hose tower pulsing red — clipped to
	 * tile (2,0), the tile tiles.rc composites the animation over.
	 */
	static BufferedImage renderFireLightFrames()
	{
		final float [] pulse = { 1f, 0.45f, 0.1f, 0.45f };
		BufferedImage sheet = new BufferedImage(4*B, B, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 4; f++) {
			gr.setClip(f*B + 2*T, 0, T, T);
			beaconGlow(gr, f*B + FIRE_BEACON.x, FIRE_BEACON.y,
				new Color(255, 60, 36), pulse[f]);
		}
		gr.dispose();
		return sheet;
	}

	/**
	 * The police station light frames: 4 one-block cells, transparent
	 * but for the rooftop pair wig-wagging red and blue — clipped to
	 * tile (1,0), the tile tiles.rc composites the animation over.
	 */
	static BufferedImage renderPoliceLightFrames()
	{
		final float [] red =  { 1f, 0.3f, 0f, 0.3f };
		final float [] blue = { 0f, 0.3f, 1f, 0.3f };
		BufferedImage sheet = new BufferedImage(4*B, B, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 4; f++) {
			gr.setClip(f*B + T, 0, T, T);
			beaconGlow(gr, f*B + POLICE_BEACON_R.x, POLICE_BEACON_R.y,
				new Color(255, 50, 36), red[f]);
			beaconGlow(gr, f*B + POLICE_BEACON_B.x, POLICE_BEACON_B.y,
				new Color(60, 120, 255), blue[f]);
		}
		gr.dispose();
		return sheet;
	}
}
