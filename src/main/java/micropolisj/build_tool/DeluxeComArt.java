package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;

/**
 * Draws the deluxe commercial art from scratch at 64px per tile: the
 * com_zones sheet (empty zone plus 20 business blocks as 4 land values
 * x 5 densities).
 *
 * The redraw keeps the classic tile language: the blue "C" sign, a
 * sidewalk ring instead of the residential hedge, small storefronts on
 * dirt lots at low value growing into glass office towers, and the
 * classic landmarks — the red and blue dense towers, the striped
 * office tower, a gas station, the stepped ziggurat and the grand
 * white cylinder tower at top value. Sun from the northwest: shadows
 * fall diagonally to the southeast, matching the rest of the deluxe art.
 */
final class DeluxeComArt
{
	private DeluxeComArt()
	{
	}

	/** Preview harness: renders the com_zones sheet to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File f = new java.io.File(
			System.getProperty("java.io.tmpdir"), "com_zones_preview.png");
		javax.imageio.ImageIO.write(renderComZones(), "png", f);
		System.out.println("Preview: "+f);
	}

	/** The com_zones sheet: 21 stacked 3x3 blocks, 192x4032. */
	static BufferedImage renderComZones()
	{
		BufferedImage sheet = new BufferedImage(B, 21*B, BufferedImage.TYPE_INT_ARGB);
		paste(sheet, emptyComBlock(), 0, 0);
		for (int value = 0; value < 4; value++) {
			for (int density = 0; density < 5; density++) {
				int block = 1 + value*5 + density;
				paste(sheet, comBlock(value, density), 0, block*B);
			}
		}
		return sheet;
	}

	//
	// 3x3 zone blocks
	//

	/** Empty commercial zone: bare dirt, sidewalk ring, big C sign. */
	static BufferedImage emptyComBlock()
	{
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, DIRT, 31);
		paintSidewalk(img, 31);

		Graphics2D gr = gfx(img);
		// freshly graded lot: a few faint bulldozer stripes
		gr.setColor(new Color(0, 0, 0, 16));
		for (int k = 0; k < 5; k++) {
			int y = 30 + k*28;
			gr.fill(new Rectangle2D.Float(14, y, B-28, 6));
		}
		plaqueC(gr, B/2, B/2, 4);
		gr.dispose();
		return img;
	}

	static BufferedImage comBlock(int value, int density)
	{
		int seed = 3000 + value*50 + density*9;
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		int ground = value == 0 ? DIRT : value == 1 ? PATCHY : value == 2 ? LAWN : LUSH;
		paintGround(img, ground, seed);
		paintSidewalk(img, seed);

		Graphics2D gr = gfx(img);

		switch (value) {
		case 0: layoutValue0(gr, img, density, seed); break;
		case 1: layoutValue1(gr, img, density, seed); break;
		case 2: layoutValue2(gr, img, density, seed); break;
		default: layoutValue3(gr, img, density, seed); break;
		}

		plaqueC(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/** Low value: small storefronts and worn walk-ups on dirt lots. */
	static void layoutValue0(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color tan = new Color(156, 142, 120);
		Color gray = new Color(134, 130, 124);
		Color wallT = new Color(98, 88, 74);
		Color wallG = new Color(88, 86, 82);
		// the classic dense red & blue towers
		Color red = new Color(172, 62, 50);
		Color wallR = new Color(112, 44, 38);
		Color blue = new Color(74, 94, 178);
		Color wallB = new Color(52, 62, 110);
		switch (density) {
		case 0:
			shopRow(gr, img, 20, 42, 92, 30, 2, tan, seed);
			parking(gr, 122, 46, 56, 40, seed);
			dumpster(gr, 26, 142);
			break;
		case 1:
			shopRow(gr, img, 16, 20, 108, 30, 3, gray, seed);
			comSlab(gr, img, 134, 66, 46, 48, 2, tan, wallT, seed+1, 0);
			shopRow(gr, img, 16, 98, 84, 30, 2, tan, seed+2);
			dumpster(gr, 116, 156);
			break;
		case 2:
			comSlab(gr, img, 14, 14, 98, 44, 3, gray, wallG, seed, 0);
			comSlab(gr, img, 120, 64, 60, 64, 3, tan, wallT, seed+1, 0);
			shopRow(gr, img, 16, 100, 88, 30, 2, tan, seed+2);
			dumpster(gr, 22, 164);
			break;
		case 3:
			comSlab(gr, img, 14, 10, 100, 46, 5, red, wallR, seed, 0);
			comSlab(gr, img, 124, 58, 56, 68, 5, blue, wallB, seed+1, 0);
			shopRow(gr, img, 16, 102, 88, 28, 2, gray, seed+2);
			dumpster(gr, 118, 168);
			break;
		default:
			comSlab(gr, img, 12, 8, 112, 48, 7, red, wallR, seed, 0);
			comSlab(gr, img, 128, 56, 52, 78, 7, blue, wallB, seed+1, 0);
			comSlab(gr, img, 14, 100, 94, 40, 6, blue, wallB, seed+2, 0);
			dumpster(gr, 128, 172);
			break;
		}
	}

	/** Lower-middle: gray offices, signage, the striped blue tower. */
	static void layoutValue1(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color slate = new Color(104, 124, 158);
		Color gray = new Color(144, 152, 160);
		Color wallS = new Color(82, 88, 104);
		Color wallG = new Color(98, 96, 92);
		Color blue = new Color(78, 102, 188);
		Color wallB = new Color(56, 70, 120);
		switch (density) {
		case 0:
			shopRow(gr, img, 22, 40, 88, 32, 2, gray, seed);
			parking(gr, 120, 44, 58, 42, seed);
			tree(gr, 36, 148, 12, seed+5);
			break;
		case 1:
			comSlab(gr, img, 14, 16, 96, 44, 3, slate, wallS, seed, 1);
			shopRow(gr, img, 120, 24, 58, 30, 1, gray, seed+1);
			comSlab(gr, img, 16, 98, 72, 40, 2, gray, wallG, seed+2, 1);
			parking(gr, 122, 96, 56, 40, seed);
			break;
		case 2:
			// the classic diagonally striped blue office tower
			comSlab(gr, img, 18, 12, 92, 56, 5, blue, wallB, seed, 1);
			roofStripes(gr, 18, 12, 92, 56);
			shopRow(gr, img, 124, 70, 56, 28, 1, gray, seed+1);
			comSlab(gr, img, 16, 102, 84, 38, 3, slate, wallS, seed+2, 1);
			break;
		case 3:
			// big block with rows of skylight domes
			comSlab(gr, img, 14, 14, 132, 58, 4, new Color(106, 138, 110), new Color(70, 92, 74), seed, 1);
			skylights(gr, 14, 14, 132, 58, seed);
			comSlab(gr, img, 130, 90, 50, 50, 4, slate, wallS, seed+1, 1);
			shopRow(gr, img, 16, 104, 86, 28, 2, gray, seed+2);
			break;
		default:
			shopRow(gr, img, 14, 14, 106, 28, 3, gray, seed);
			glassTower(gr, img, 130, 56, 50, 62, 5, new Color(96, 128, 176), new Color(70, 76, 88), seed+1);
			shopRow(gr, img, 14, 96, 88, 28, 2, slate, seed+2);
			break;
		}
	}

	/** Upper-middle: gas station, glass offices, the dark high-rise. */
	static void layoutValue2(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color glass = new Color(110, 150, 190);
		Color frame = new Color(86, 94, 104);
		Color white = new Color(222, 222, 226);
		Color wallW = new Color(168, 168, 172);
		switch (density) {
		case 0:
			gasStation(gr, img, seed);
			break;
		case 1:
			comSlab(gr, img, 18, 22, 92, 52, 3, new Color(176, 168, 156), new Color(118, 110, 100), seed, 1);
			dome(gr, 40, 26, 48);
			shopRow(gr, img, 122, 92, 58, 30, 1, new Color(206, 196, 182), seed+1);
			parking(gr, 122, 28, 56, 42, seed);
			tree(gr, 40, 152, 13, seed+5);
			break;
		case 2:
			glassTower(gr, img, 18, 14, 66, 44, 5, glass, frame, seed);
			glassTower(gr, img, 108, 84, 66, 44, 5, glass, frame, seed+1);
			tree(gr, 152, 40, 13, seed+5);
			tree(gr, 38, 152, 12, seed+6);
			bush(gr, 64, 166, seed+7);
			break;
		case 3:
			// white terraced building, classic v2 showpiece
			steppedTower(gr, img, 26, 84, 140, 56,
				new int [] { 2, 3, 3 }, white, wallW, seed, false);
			tree(gr, 160, 158, 12, seed+5);
			bush(gr, 30, 166, seed+6);
			break;
		default:
			// dark glass high-rise pair
			glassTower(gr, img, 16, 10, 94, 52, 8, new Color(72, 88, 110), new Color(48, 52, 62), seed);
			glassTower(gr, img, 124, 78, 56, 48, 6, glass, frame, seed+1);
			shopRow(gr, img, 16, 110, 84, 26, 2, new Color(160, 156, 150), seed+2);
			break;
		}
	}

	/** High value: boutiques, glass towers, the grand cylinder. */
	static void layoutValue3(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color glass = new Color(124, 164, 200);
		Color frame = new Color(92, 100, 110);
		Color cream = new Color(226, 218, 202);
		Color white = new Color(232, 232, 236);
		Color wallW = new Color(174, 174, 180);
		switch (density) {
		case 0:
			// upscale boutique row on a plaza
			shopRow(gr, img, 18, 32, 116, 36, 3, cream, seed, true);
			parking(gr, 142, 36, 38, 44, seed);
			tree(gr, 36, 120, 14, seed+5);
			tree(gr, 152, 110, 13, seed+6);
			bush(gr, 70, 150, seed+7);
			bush(gr, 120, 158, seed+8);
			break;
		case 1:
			glassTower(gr, img, 16, 16, 100, 48, 5, glass, frame, seed);
			shopRow(gr, img, 128, 26, 52, 32, 1, cream, seed+1);
			parking(gr, 124, 96, 56, 42, seed);
			tree(gr, 40, 132, 14, seed+5);
			tree(gr, 76, 156, 11, seed+6);
			break;
		case 2:
			glassTower(gr, img, 16, 12, 70, 46, 6, glass, frame, seed);
			glassTower(gr, img, 106, 80, 70, 46, 6, glass, frame, seed+1);
			tree(gr, 150, 38, 13, seed+5);
			tree(gr, 36, 150, 13, seed+6);
			bush(gr, 64, 168, seed+7);
			break;
		case 3:
			// the stepped ziggurat skyscraper
			steppedTower(gr, img, 22, 80, 148, 58,
				new int [] { 3, 4, 4 }, white, wallW, seed, true);
			tree(gr, 164, 160, 11, seed+5);
			break;
		default:
			// the grand white cylinder tower
			cylinderTower(gr, img, 38, 8, 116, 9, seed);
			tree(gr, 22, 158, 12, seed+5);
			tree(gr, 168, 150, 12, seed+6);
			bush(gr, 60, 172, seed+7);
			bush(gr, 132, 176, seed+8);
			break;
		}
	}

	//
	// commercial building kit
	//

	// storefront sign band colors
	static final Color [] SIGN_COLORS = {
		new Color(196, 60, 44), new Color(60, 100, 180),
		new Color(38, 110, 62), new Color(216, 122, 38),
		new Color(122, 72, 152), new Color(42, 132, 130),
	};

	/**
	 * An office/retail slab: a standard slab dressed commercially so it
	 * never reads as an apartment block — a continuous glass storefront
	 * with a colored sign band at street level, and a rooftop billboard.
	 */
	static void comSlab(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int floors, Color roof, Color wall, int seed, int detail)
	{
		slab(gr, img, x, y, w, h, floors, roof, wall, seed, detail);
		Color sign = SIGN_COLORS[(int)(hash1(seed, 43)*SIGN_COLORS.length)];

		// street-level storefront over the ground-floor window row
		int bottom = y + h + 3 + floors*6;
		int gy = y + h + 2 + (floors-1)*6;
		gr.setColor(sign);
		gr.fill(new Rectangle2D.Float(x+1, gy-2, w-2, 3));
		gr.setColor(mixc(sign, Color.WHITE, 0.35f));
		gr.fill(new Rectangle2D.Float(x+1, gy-2, w-2, 1));
		gr.setColor(new Color(54, 70, 94));
		gr.fill(new Rectangle2D.Float(x+1, gy+1, w-2, bottom-3-(gy+1)));
		gr.setColor(new Color(132, 162, 192));
		gr.fill(new Rectangle2D.Float(x+1, gy+1, w-2, 1));
		gr.setColor(new Color(40, 32, 28));
		gr.fill(new Rectangle2D.Float(x+w/2f-2, gy+1, 4, bottom-3-(gy+1)));

		// rooftop billboard, facing the street
		int bw = Math.min(w-12, 26);
		float bx = x + 5, by = y + 4;
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(bx+2, by+2, bw, 8));
		gr.setColor(new Color(58, 56, 54));
		gr.fill(new Rectangle2D.Float(bx, by, bw, 8));
		gr.setColor(sign);
		gr.fill(new Rectangle2D.Float(bx+1, by+1, bw-2, 6));
		gr.setColor(new Color(255, 255, 255, 210));
		gr.fill(new Rectangle2D.Float(bx+3, by+3, bw*0.4f, 2));
		gr.fill(new Rectangle2D.Float(bx+bw*0.5f+2, by+3, bw*0.25f, 2));
	}

	/**
	 * One-story retail strip: flat roof with parapet, and a south wall
	 * split into shop units, each with a colored sign band, a wide glass
	 * display window and a door.
	 */
	static void shopRow(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int units, Color roof, int seed)
	{
		shopRow(gr, img, x, y, w, h, units, roof, seed, false);
	}

	static void shopRow(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int units, Color roof, int seed, boolean awnings)
	{
		int hgt = 14;   // one tall retail floor
		shadow(gr, x, y, w, h, hgt);

		// south wall
		Color wall = mixc(roof, Color.BLACK, 0.35f);
		gr.setColor(wall);
		gr.fill(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setColor(mixc(wall, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x, y+h+hgt-2, w, 2));

		float uw = w/(float)units;
		for (int u = 0; u < units; u++) {
			float ux = x + u*uw;
			Color sign = SIGN_COLORS[(int)(hash1(seed+u, 41)*SIGN_COLORS.length)];
			// sign band over the storefront
			gr.setColor(sign);
			gr.fill(new Rectangle2D.Float(ux+1, y+h+1, uw-2, 4));
			gr.setColor(mixc(sign, Color.WHITE, 0.35f));
			gr.fill(new Rectangle2D.Float(ux+1, y+h+1, uw-2, 1));
			// glass display window with a sky reflection
			gr.setColor(new Color(54, 70, 94));
			gr.fill(new Rectangle2D.Float(ux+2, y+h+6, uw-4, 5));
			gr.setColor(new Color(132, 162, 192));
			gr.fill(new Rectangle2D.Float(ux+2, y+h+6, uw-4, 2));
			// door
			gr.setColor(new Color(50, 40, 34));
			gr.fill(new Rectangle2D.Float(ux+uw-7, y+h+6, 4, 6));
			// scalloped canvas awning over the display window
			if (awnings) {
				Color cream = new Color(238, 232, 220);
				for (int k = 0; k*5 < uw-6; k++) {
					gr.setColor(k % 2 == 0 ? sign : cream);
					float sw = Math.min(5, uw-6 - k*5);
					gr.fill(new Rectangle2D.Float(ux+3 + k*5, y+h+5, sw, 3));
					gr.fill(new Arc2D.Float(ux+3 + k*5, y+h+6, sw, 4, 180, 180, Arc2D.CHORD));
				}
				gr.setColor(new Color(255, 255, 255, 70));
				gr.fill(new Rectangle2D.Float(ux+3, y+h+5, uw-6, 1));
			}
			// unit divider
			if (u > 0) {
				gr.setColor(mixc(wall, Color.BLACK, 0.4f));
				gr.fill(new Rectangle2D.Float(ux, y+h, 1, hgt));
			}
		}

		// flat roof with parapet
		gr.setColor(mixc(roof, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		gr.setColor(roof);
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, h-2));
		gr.setColor(mixc(roof, Color.WHITE, 0.28f));
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, 2));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 2, h-2));
		roofGrain(img, x+2, y+2, w-4, h-4, seed);

		// rooftop AC units
		for (int k = 0; k < units; k++) {
			float ax = units == 1 ? x + w/2f - 5 : x + 8 + k*(w-20f)/(units-1);
			float ay = y + 6 + 5*hash1(k, seed+22);
			gr.setColor(new Color(0, 0, 0, 60));
			gr.fill(new Rectangle2D.Float(ax+2, ay+2, 9, 7));
			gr.setColor(new Color(188, 190, 192));
			gr.fill(new Rectangle2D.Float(ax, ay, 9, 7));
			gr.setColor(new Color(120, 122, 126));
			gr.fill(new Ellipse2D.Float(ax+1.5f, ay+1.5f, 4, 4));
		}
	}

	/**
	 * An office tower with a glass curtain wall: gradient glazing with
	 * mullion grid, scattered lit offices, a diagonal sheen, and a dark
	 * roof with a mechanical penthouse.
	 */
	static void glassTower(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int floors, Color glass, Color frame, int seed)
	{
		int hgt = 3 + floors*6;
		shadow(gr, x, y, w, h, hgt);

		// curtain wall
		Paint saved = gr.getPaint();
		gr.setPaint(new GradientPaint(x, y+h, mixc(glass, Color.WHITE, 0.30f),
			x, y+h+hgt, mixc(glass, Color.BLACK, 0.38f)));
		gr.fill(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setPaint(saved);

		// mullion grid
		gr.setColor(new Color(0, 0, 0, 70));
		for (int f = 1; f < floors; f++) {
			gr.fill(new Rectangle2D.Float(x, y+h + f*6, w, 1));
		}
		for (int wx = x+6; wx < x+w-2; wx += 7) {
			gr.fill(new Rectangle2D.Float(wx, y+h, 1, hgt));
		}
		// scattered lit offices
		for (int f = 0; f < floors; f++) {
			for (int wx = x+1; wx + 5 < x+w; wx += 7) {
				if (hash2(wx, f, seed) < 0.10f) {
					gr.setColor(new Color(238, 212, 130, 200));
					gr.fill(new Rectangle2D.Float(wx, y+h + f*6 + 1, 5, 4));
				}
			}
		}
		// diagonal sky sheen across the glazing
		Shape clip = gr.getClip();
		gr.clip(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setColor(new Color(255, 255, 255, 36));
		int sx = x + w/4;
		gr.fill(new Polygon(
			new int [] { sx, sx+10, sx-w/3+10, sx-w/3 },
			new int [] { y+h, y+h, y+h+hgt, y+h+hgt }, 4));
		gr.setClip(clip);
		// entrance
		gr.setColor(new Color(36, 40, 48));
		gr.fill(new Rectangle2D.Float(x+w/2f-6, y+h+hgt-5, 12, 5));
		gr.setColor(mixc(glass, Color.BLACK, 0.5f));
		gr.fill(new Rectangle2D.Float(x, y+h+hgt-1, w, 1));

		// dark roof with penthouse
		gr.setColor(mixc(frame, Color.BLACK, 0.3f));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		gr.setColor(frame);
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, h-2));
		gr.setColor(mixc(frame, Color.WHITE, 0.22f));
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, 2));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 2, h-2));
		roofGrain(img, x+2, y+2, w-4, h-4, seed);
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+w/2f-7+2, y+h/2f-5+2, 14, 10));
		gr.setColor(mixc(frame, Color.WHITE, 0.12f));
		gr.fill(new Rectangle2D.Float(x+w/2f-7, y+h/2f-5, 14, 10));
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+5+2, y+4+2, 8, 6));
		gr.setColor(new Color(178, 180, 184));
		gr.fill(new Rectangle2D.Float(x+5, y+4, 8, 6));
	}

	/**
	 * A stepped ziggurat tower: tiers shrink as they rise, each standing
	 * on the roof of the one below. (x, y, w, h) is the base tier roof;
	 * floors[] runs bottom to top.
	 */
	static void steppedTower(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int [] floors, Color roof, Color wall, int seed, boolean glassTop)
	{
		int curX = x, curY = y, curW = w, curH = h;
		for (int t = 0; t < floors.length; t++) {
			boolean top = t == floors.length-1;
			if (top && glassTop) {
				glassTower(gr, img, curX, curY, curW, curH, floors[t],
					new Color(124, 164, 200), mixc(wall, Color.BLACK, 0.2f), seed+t);
			}
			else if (t == 0) {
				// street level gets the commercial storefront dressing
				comSlab(gr, img, curX, curY, curW, curH, floors[t], roof, wall, seed+t, 1);
			}
			else {
				slab(gr, img, curX, curY, curW, curH, floors[t],
					mixc(roof, Color.BLACK, t*0.06f), wall, seed+t, top ? 3 : 1);
			}
			if (!top) {
				int nw = curW - 28;
				int nh = Math.max(26, curH - 12);
				int hgt = 3 + floors[t+1]*6;
				// the next tier's wall meets the middle of this roof
				int bottom = curY + curH/2;
				int nx = curX + (curW-nw)/2;
				curX = nx;
				curY = bottom - nh - hgt;
				curW = nw;
				curH = nh;
			}
		}
	}

	/** Diagonal white accent stripes across a roof (the classic tower). */
	static void roofStripes(Graphics2D gr, int x, int y, int w, int h)
	{
		Shape clip = gr.getClip();
		gr.clip(new Rectangle2D.Float(x+1, y+1, w-2, h-2));
		gr.setColor(new Color(255, 255, 255, 150));
		for (int k = -h; k < w; k += 14) {
			gr.fill(new Polygon(
				new int [] { x+k, x+k+5, x+k+h+5, x+k+h },
				new int [] { y, y, y+h, y+h }, 4));
		}
		gr.setClip(clip);
	}

	/** Rows of skylight domes on a big-box roof. */
	static void skylights(Graphics2D gr, int x, int y, int w, int h, int seed)
	{
		for (int j = y+10; j + 8 < y+h-6; j += 14) {
			for (int i = x+10; i + 8 < x+w-8; i += 16) {
				gr.setColor(new Color(0, 0, 0, 50));
				gr.fill(new Ellipse2D.Float(i+1, j+1, 8, 6));
				gr.setColor(new Color(168, 196, 220));
				gr.fill(new Ellipse2D.Float(i, j, 8, 6));
				gr.setColor(new Color(228, 240, 248));
				gr.fill(new Ellipse2D.Float(i+1, j+1, 3, 2));
			}
		}
	}

	/** A dark glass dome sitting on a roof, lit from the northwest. */
	static void dome(Graphics2D gr, int x, int y, int d)
	{
		int eh = Math.round(d*0.8f);
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Ellipse2D.Float(x+4, y+6, d, eh));
		gr.setColor(new Color(52, 60, 76));
		gr.fill(new Ellipse2D.Float(x, y, d, eh));
		gr.setColor(new Color(84, 96, 118));
		gr.fill(new Ellipse2D.Float(x+d*0.12f, y+eh*0.10f, d*0.62f, eh*0.58f));
		gr.setColor(new Color(150, 170, 196));
		gr.fill(new Ellipse2D.Float(x+d*0.20f, y+eh*0.16f, d*0.30f, eh*0.24f));
	}

	/** Gas station: asphalt lot, kiosk, pump canopy with a red stripe. */
	static void gasStation(Graphics2D gr, BufferedImage img, int seed)
	{
		// asphalt apron
		gr.setColor(new Color(92, 92, 96));
		gr.fill(new Rectangle2D.Float(10, 64, 172, 118));
		gr.setColor(new Color(110, 110, 114));
		gr.fill(new Rectangle2D.Float(10, 64, 172, 2));

		// kiosk shop at the back
		shopRow(gr, img, 18, 18, 80, 28, 1, new Color(206, 200, 190), seed);

		// pump island canopy on four pillars
		int cx = 56, cy = 96, cw = 96, ch = 38;
		gr.setColor(new Color(0, 0, 0, 45));
		gr.fill(new Rectangle2D.Float(cx+8, cy+10, cw, ch));
		// pumps beneath, peeking out south of the canopy
		gr.setColor(new Color(190, 50, 44));
		gr.fill(new Rectangle2D.Float(cx+22, cy+ch+2, 9, 12));
		gr.fill(new Rectangle2D.Float(cx+62, cy+ch+2, 9, 12));
		gr.setColor(new Color(240, 238, 234));
		gr.fill(new Rectangle2D.Float(cx+23, cy+ch+3, 7, 4));
		gr.fill(new Rectangle2D.Float(cx+63, cy+ch+3, 7, 4));
		// pillars
		gr.setColor(new Color(70, 70, 74));
		gr.fill(new Rectangle2D.Float(cx+6, cy+ch-2, 4, 10));
		gr.fill(new Rectangle2D.Float(cx+cw-10, cy+ch-2, 4, 10));
		// canopy roof
		gr.setColor(new Color(236, 236, 238));
		gr.fill(new Rectangle2D.Float(cx, cy, cw, ch));
		gr.setColor(new Color(196, 50, 44));
		gr.fill(new Rectangle2D.Float(cx, cy+ch-7, cw, 7));
		gr.setColor(new Color(255, 255, 255, 90));
		gr.fill(new Rectangle2D.Float(cx, cy, cw, 3));
		// brand dot on the canopy
		gr.setColor(new Color(196, 50, 44));
		gr.fill(new Ellipse2D.Float(cx+cw/2f-7, cy+8, 14, 14));
		gr.setColor(new Color(240, 200, 60));
		gr.fill(new Ellipse2D.Float(cx+cw/2f-4, cy+11, 8, 8));

		// a car at the pumps and one leaving
		car(gr, cx+36, cy+ch+2, 12, 18, carColor(seed));
		car(gr, 160, 130, 11, 17, carColor(seed+1));

		// price sign by the street
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Rectangle2D.Float(22+2, 150+2, 14, 22));
		gr.setColor(new Color(60, 62, 70));
		gr.fill(new Rectangle2D.Float(22, 150, 14, 22));
		gr.setColor(new Color(240, 200, 60));
		gr.fill(new Rectangle2D.Float(24, 152, 10, 8));
	}

	//
	// sidewalk ring and the "C" sign
	//

	/**
	 * Blue paver ring around the block — the classic commercial-zone
	 * blue box, so commercial reads apart from residential (hedge) and
	 * industrial at a glance. Textured like a sidewalk, with expansion
	 * seams and a curb shadow on the inner edge.
	 */
	static void paintSidewalk(BufferedImage img, int seed)
	{
		int n = img.getWidth();
		final int W = 6;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int d = Math.min(Math.min(x, n-1-x), Math.min(y, n-1-y));
				if (d >= W) continue;
				float u = (x % T) / (float)T;
				float v = (y % T) / (float)T;
				float lum = 0.93f + 0.12f*noise(u, v, 24, seed+4);
				float [] col = scale(rgb(88, 118, 246), lum);
				boolean onNS = y < W || y > n-1-W;
				boolean onEW = x < W || x > n-1-W;
				if ((onNS && x % 16 == 0) || (onEW && y % 16 == 0)) {
					col = scale(col, 0.78f);   // expansion seam
				}
				if (d == 0) col = scale(col, 1.18f);     // sunlit outer edge
				if (d == W-1) col = scale(col, 0.66f);   // curb shadow
				img.setRGB(x, y, pack(col));
			}
		}
	}

	static final String [] GLYPH_C = {
		".XXX.",
		"X...X",
		"X....",
		"X....",
		"X....",
		"X...X",
		".XXX.",
	};

	/** Dark plaque with the classic blue "C", centered at (cx, cy). */
	static void plaqueC(Graphics2D gr, int cx, int cy, int scale)
	{
		int gw = 5*scale, gh = 7*scale;
		int pw = gw + 4*scale, ph = gh + 4*scale;
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f+2, cy-ph/2f+2, pw, ph, 4, 4));
		gr.setColor(new Color(44, 48, 54));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		gr.setColor(new Color(86, 92, 102));
		gr.draw(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		glyph(gr, GLYPH_C, cx-gw/2, cy-gh/2, scale, new Color(96, 156, 240));
	}
}
