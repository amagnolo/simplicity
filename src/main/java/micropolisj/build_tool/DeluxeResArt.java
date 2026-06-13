package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;

/**
 * Draws the deluxe residential art completely from scratch at 64px per
 * tile: the res_zones sheet (empty zone, 16 apartment blocks as 4 land
 * values x 4 densities, hospital, church) and the res_houses sheet
 * (12 single-family houses in 4 value tiers).
 *
 * The redraw keeps the classic tile language so the map still reads the
 * same: a green hedge border around each 3x3 zone, the green "R" sign,
 * dirt lots that turn into lush lawns as land value rises, gray slabs ->
 * blue-roofed apartments -> awning-trimmed brick -> white luxury with
 * round towers, a red roof cross for the hospital and a white steeple
 * for the church. Sun from the northwest: shadows fall diagonally
 * to the southeast, matching the procedural terrain art.
 */
final class DeluxeResArt
{
	static final int T = 64;     // px per tile
	static final int B = 3*T;    // px per 3x3 zone block

	// ground kinds, by rising land value
	static final int DIRT = 0;
	static final int PATCHY = 1;
	static final int LAWN = 2;
	static final int LUSH = 3;

	private DeluxeResArt()
	{
	}

	/** Preview harness: renders the awning style variants side by side. */
	public static void main(String [] args)
		throws Exception
	{
		int saved = awningStyle;
		awningStyle = AWNING_WIDE_SCALLOPED;
		Color [] colors = {
			new Color(38, 110, 62),    // green
			new Color(216, 122, 38),   // orange
			new Color(196, 60, 44),    // red
		};
		BufferedImage sheet = new BufferedImage((B+6)*colors.length-6, B*2+6,
			BufferedImage.TYPE_INT_ARGB);
		for (int s = 0; s < colors.length; s++) {
			wideAwningColor = colors[s];
			paste(sheet, apartmentBlock(2, 1), s*(B+6), 0);
			paste(sheet, apartmentBlock(2, 3), s*(B+6), B+6);
		}
		awningStyle = saved;
		java.io.File f = new java.io.File(
			System.getProperty("java.io.tmpdir"), "awning_variants.png");
		javax.imageio.ImageIO.write(sheet, "png", f);
		System.out.println("Preview: "+f);
	}

	//
	// sheets
	//

	/** The res_zones sheet: 19 stacked 3x3 blocks, 192x3648. */
	static BufferedImage renderResZones()
	{
		BufferedImage sheet = new BufferedImage(B, 19*B, BufferedImage.TYPE_INT_ARGB);
		paste(sheet, emptyZoneBlock(), 0, 0);
		for (int value = 0; value < 4; value++) {
			for (int density = 0; density < 4; density++) {
				int block = 1 + value*4 + density;
				paste(sheet, apartmentBlock(value, density), 0, block*B);
			}
		}
		paste(sheet, hospitalBlock(), 0, 17*B);
		paste(sheet, churchBlock(), 0, 18*B);
		return sheet;
	}

	/** The res_houses sheet: 12 stacked single-tile houses, 64x768. */
	static BufferedImage renderResHouses()
	{
		BufferedImage sheet = new BufferedImage(T, 12*T, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < 12; i++) {
			paste(sheet, houseTile(i), 0, i*T);
		}
		return sheet;
	}

	//
	// 3x3 zone blocks
	//

	static Graphics2D gfx(BufferedImage img)
	{
		Graphics2D gr = img.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return gr;
	}

	/** Empty residential zone: bare dirt, hedge ring, big R sign. */
	static BufferedImage emptyZoneBlock()
	{
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, DIRT, 11);
		paintHedge(img, false, 11);

		Graphics2D gr = gfx(img);
		// freshly graded lot: a few faint bulldozer stripes
		gr.setColor(new Color(0, 0, 0, 16));
		for (int k = 0; k < 5; k++) {
			int y = 30 + k*28;
			gr.fill(new Rectangle2D.Float(14, y, B-28, 6));
		}
		plaqueR(gr, B/2, B/2, 4);
		gr.dispose();
		return img;
	}

	static BufferedImage apartmentBlock(int value, int density)
	{
		int seed = 1000 + value*40 + density*7;
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		int ground = value <= 1 ? DIRT : value == 2 ? PATCHY : LUSH;
		paintGround(img, ground, seed);
		paintHedge(img, true, seed);

		Graphics2D gr = gfx(img);
		// entry walk from the street on the south edge
		path(gr, B/2-9, 100, 18, B-100);

		switch (value) {
		case 0: layoutValue0(gr, img, density, seed); break;
		case 1: layoutValue1(gr, img, density, seed); break;
		case 2: layoutValue2(gr, img, density, seed); break;
		default: layoutValue3(gr, img, density, seed); break;
		}

		plaqueR(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/** Low value: worn gray/tan slabs on dirt, parking, dumpsters. */
	static void layoutValue0(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color roofA = new Color(150, 140, 126);
		Color roofB = new Color(128, 120, 110);
		Color roofC = new Color(160, 144, 116);
		Color wall = new Color(94, 86, 78);
		switch (density) {
		case 0:
			parking(gr, 102, 22, 74, 44, seed);
			slab(gr, img, 16, 20, 74, 42, 2, roofA, wall, seed, 0);
			slab(gr, img, 98, 96, 78, 46, 2, roofB, wall, seed+1, 0);
			dumpster(gr, 22, 130);
			break;
		case 1:
			slab(gr, img, 14, 14, 106, 44, 3, roofA, wall, seed, 0);
			slab(gr, img, 128, 84, 50, 56, 3, roofC, wall, seed+1, 0);
			parking(gr, 130, 16, 48, 44, seed);
			slab(gr, img, 16, 98, 66, 42, 3, roofB, wall, seed+2, 0);
			dumpster(gr, 92, 158);
			break;
		case 2:
			slab(gr, img, 14, 10, 120, 46, 4, roofB, wall, seed, 0);
			slab(gr, img, 118, 68, 60, 76, 4, roofA, wall, seed+1, 0);
			slab(gr, img, 16, 92, 84, 46, 4, roofC, wall, seed+2, 0);
			dumpster(gr, 20, 168);
			break;
		default:
			slab(gr, img, 12, 8, 164, 46, 6, roofA, wall, seed, 0);
			slab(gr, img, 118, 64, 62, 74, 5, roofB, wall, seed+1, 0);
			slab(gr, img, 14, 98, 88, 42, 5, roofC, wall, seed+2, 0);
			dumpster(gr, 104, 174);
			break;
		}
	}

	/** Lower-middle: blue-roofed apartments, dirt with grass patches. */
	static void layoutValue1(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color blue = new Color(86, 102, 188);
		Color gray = new Color(152, 152, 158);
		Color slate = new Color(116, 126, 168);
		Color wall = new Color(72, 78, 116);
		Color wallG = new Color(96, 96, 102);
		switch (density) {
		case 0:
			slab(gr, img, 18, 22, 76, 44, 2, blue, wall, seed, 1);
			slab(gr, img, 100, 98, 76, 44, 2, gray, wallG, seed+1, 1);
			parking(gr, 104, 24, 70, 42, seed);
			tree(gr, 36, 152, 13, seed+5);
			break;
		case 1:
			slab(gr, img, 14, 14, 104, 46, 3, blue, wall, seed, 1);
			slab(gr, img, 126, 84, 52, 58, 3, slate, wall, seed+1, 1);
			parking(gr, 128, 16, 50, 46, seed);
			slab(gr, img, 16, 98, 70, 42, 3, gray, wallG, seed+2, 1);
			tree(gr, 100, 164, 11, seed+5);
			break;
		case 2:
			slab(gr, img, 14, 10, 122, 46, 4, slate, wall, seed, 1);
			slab(gr, img, 116, 68, 62, 76, 4, blue, wall, seed+1, 1);
			slab(gr, img, 16, 92, 84, 46, 4, gray, wallG, seed+2, 1);
			tree(gr, 36, 172, 11, seed+5);
			break;
		default:
			slab(gr, img, 12, 8, 164, 46, 6, blue, wall, seed, 1);
			slab(gr, img, 116, 64, 64, 74, 5, slate, wall, seed+1, 1);
			slab(gr, img, 14, 98, 88, 42, 5, gray, wallG, seed+2, 1);
			bush(gr, 108, 176, seed+5);
			break;
		}
	}

	/** Upper-middle: brick with striped awnings, real lawns, bushes. */
	static void layoutValue2(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color brick = new Color(168, 96, 70);
		Color warm = new Color(178, 124, 84);
		Color gray = new Color(162, 152, 142);
		Color wall = new Color(116, 66, 48);
		Color wallG = new Color(104, 96, 88);
		switch (density) {
		case 0:
			slab(gr, img, 18, 22, 78, 46, 2, brick, wall, seed, 2);
			slab(gr, img, 102, 98, 74, 44, 2, warm, wall, seed+1, 2);
			tree(gr, 130, 44, 14, seed+5);
			bush(gr, 30, 96, seed+6);
			bush(gr, 60, 152, seed+7);
			break;
		case 1:
			slab(gr, img, 14, 14, 106, 46, 3, brick, wall, seed, 2);
			slab(gr, img, 128, 86, 50, 56, 3, gray, wallG, seed+1, 2);
			slab(gr, img, 16, 98, 72, 42, 3, warm, wall, seed+2, 2);
			tree(gr, 152, 38, 14, seed+5);
			bush(gr, 100, 162, seed+6);
			break;
		case 2:
			slab(gr, img, 14, 10, 122, 48, 4, warm, wall, seed, 2);
			slab(gr, img, 116, 70, 62, 74, 4, brick, wall, seed+1, 2);
			slab(gr, img, 16, 92, 84, 46, 4, gray, wallG, seed+2, 2);
			tree(gr, 158, 34, 12, seed+5);
			bush(gr, 104, 176, seed+6);
			break;
		default:
			slab(gr, img, 12, 8, 164, 48, 6, brick, wall, seed, 2);
			slab(gr, img, 116, 66, 64, 72, 5, warm, wall, seed+1, 2);
			slab(gr, img, 14, 98, 88, 42, 5, gray, wallG, seed+2, 2);
			bush(gr, 106, 176, seed+5);
			break;
		}
	}

	/** High value: white/glass condos on lush lawns, pools and trees. */
	static void layoutValue3(Graphics2D gr, BufferedImage img, int density, int seed)
	{
		Color white = new Color(228, 228, 232);
		Color cream = new Color(224, 218, 204);
		Color wall = new Color(168, 168, 174);
		switch (density) {
		case 0:
			slab(gr, img, 18, 22, 80, 46, 2, white, wall, seed, 3);
			slab(gr, img, 102, 100, 76, 42, 2, cream, wall, seed+1, 3);
			pool(gr, 116, 36);
			tree(gr, 40, 104, 15, seed+5);
			tree(gr, 70, 152, 12, seed+6);
			break;
		case 1:
			slab(gr, img, 14, 16, 108, 46, 3, white, wall, seed, 3);
			slab(gr, img, 126, 86, 52, 56, 3, cream, wall, seed+1, 3);
			pool(gr, 136, 32);
			tree(gr, 36, 100, 14, seed+5);
			tree(gr, 96, 162, 11, seed+6);
			break;
		case 2:
			// the classic twin round towers
			cylinderTower(gr, img, 20, 10, 70, 6, seed);
			slab(gr, img, 104, 24, 62, 38, 4, cream, wall, seed+2, 3);
			cylinderTower(gr, img, 104, 88, 70, 6, seed+1);
			tree(gr, 40, 130, 14, seed+5);
			tree(gr, 66, 158, 11, seed+6);
			break;
		default:
			slab(gr, img, 12, 8, 164, 48, 6, white, wall, seed, 3);
			slab(gr, img, 118, 64, 60, 76, 5, cream, wall, seed+1, 3);
			slab(gr, img, 14, 98, 86, 42, 5, white, wall, seed+2, 3);
			tree(gr, 106, 176, 10, seed+5);
			break;
		}
	}

	/** Hospital: white block, helipad with the classic red cross. */
	static BufferedImage hospitalBlock()
	{
		int seed = 1700;
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, LAWN, seed);
		paintHedge(img, true, seed);

		Graphics2D gr = gfx(img);
		path(gr, B/2-12, 96, 24, B-96);

		Color white = new Color(230, 232, 234);
		Color wall = new Color(176, 180, 186);
		slab(gr, img, 16, 18, 160, 92, 3, white, wall, seed, 3);
		slab(gr, img, 16, 124, 70, 34, 2, new Color(214, 218, 222), wall, seed+1, 3);

		// helipad on the main roof
		float hx = 130, hy = 60;
		gr.setColor(new Color(196, 200, 204));
		gr.fill(new Ellipse2D.Float(hx-30, hy-30, 60, 60));
		gr.setColor(new Color(160, 164, 170));
		gr.setStroke(new BasicStroke(2f));
		gr.draw(new Ellipse2D.Float(hx-30, hy-30, 60, 60));
		gr.setStroke(new BasicStroke(1f));
		// red cross
		gr.setColor(new Color(202, 40, 40));
		gr.fill(new Rectangle2D.Float(hx-22, hy-7, 44, 14));
		gr.fill(new Rectangle2D.Float(hx-7, hy-22, 14, 44));

		// emergency canopy with red stripe over the entrance
		gr.setColor(new Color(238, 240, 242));
		gr.fill(new Rectangle2D.Float(98, 126, 40, 12));
		gr.setColor(new Color(202, 40, 40));
		gr.fill(new Rectangle2D.Float(98, 132, 40, 4));

		// ambulance by the path
		car(gr, 118, 152, 24, 12, new Color(240, 240, 244));
		gr.setColor(new Color(202, 40, 40));
		gr.fill(new Rectangle2D.Float(120, 156, 18, 3));

		gr.dispose();
		return img;
	}

	/**
	 * Church, in the same top-down oblique style as the houses: slate
	 * gable roof with lit/shaded slopes, transept arms, a stone gable
	 * front with door and rose window, and a steeple tower standing in
	 * front with a pyramid spire and a long shadow.
	 */
	static BufferedImage churchBlock()
	{
		int seed = 1800;
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, LAWN, seed);
		paintHedge(img, true, seed);

		Graphics2D gr = gfx(img);
		path(gr, B/2-9, 134, 18, B-134);

		Color slateLit = new Color(116, 112, 128);
		Color slateDark = new Color(70, 66, 82);
		Color eave = new Color(44, 42, 52);
		Color stone = new Color(208, 202, 190);
		Color stoneShade = new Color(172, 166, 154);

		int x = 68, y = 26, w = 56, h = 86;
		int hgt = 18;    // gable-end wall height
		int sx = x + w/2;

		// transept arms (east-west ridge), tucked behind the nave roof
		int ty = y + 28, th = 30, aw = 22, ahgt = 10;
		for (int side = 0; side < 2; side++) {
			int ax = side == 0 ? x - aw : x + w;
			shadow(gr, ax, ty, aw, th, ahgt);
			gr.setColor(eave);
			gr.fill(new Rectangle2D.Float(ax-1, ty-1, aw+2, th+2));
			gr.setColor(slateLit);
			gr.fill(new Rectangle2D.Float(ax, ty, aw, th/2f));
			gr.setColor(slateDark);
			gr.fill(new Rectangle2D.Float(ax, ty+th/2f, aw, th/2f));
			gr.setColor(mixc(slateLit, Color.WHITE, 0.4f));
			gr.fill(new Rectangle2D.Float(ax, ty+th/2f-1, aw, 2));
			// transept south wall with a lancet window
			gr.setColor(side == 0 ? stone : stoneShade);
			gr.fill(new Rectangle2D.Float(ax, ty+th, aw, ahgt));
			gr.setColor(new Color(86, 110, 150));
			gr.fill(new RoundRectangle2D.Float(ax+aw/2f-2, ty+th+2, 4, ahgt-4, 4, 4));
		}

		// nave (north-south ridge): west slope lit, east in shade
		shadow(gr, x, y, w, h, hgt);
		gr.setColor(eave);
		gr.fill(new Rectangle2D.Float(x-2, y-2, w+4, h+4));
		gr.setColor(slateLit);
		gr.fill(new Rectangle2D.Float(x, y, w/2f, h));
		gr.setColor(slateDark);
		gr.fill(new Rectangle2D.Float(x+w/2f, y, w/2f, h));
		gr.setColor(mixc(slateLit, Color.WHITE, 0.45f));
		gr.fill(new Rectangle2D.Float(x+w/2f-1, y, 2, h));

		// stone gable front: cornice, buttresses, windows either side
		// of the bell tower
		gr.setColor(stone);
		gr.fill(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setColor(stoneShade);
		gr.fill(new Rectangle2D.Float(x, y+h, w, 2));
		gr.fill(new Rectangle2D.Float(x+3, y+h, 3, hgt));
		gr.fill(new Rectangle2D.Float(x+w-6, y+h, 3, hgt));
		gr.setColor(mixc(stone, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x, y+h+hgt-2, w, 2));
		gr.setColor(new Color(96, 120, 168));
		gr.fill(new Ellipse2D.Float(sx-20, y+h+4, 8, 8));
		gr.fill(new Ellipse2D.Float(sx+12, y+h+4, 8, 8));
		gr.setColor(new Color(150, 160, 190));
		gr.draw(new Ellipse2D.Float(sx-20, y+h+4, 8, 8));
		gr.draw(new Ellipse2D.Float(sx+12, y+h+4, 8, 8));

		// bell tower engaged in the front of the nave: the spire rises
		// out of the roofline, the tower base carries the entrance
		int sp = 13;                       // spire half-size
		int towerTop = y + h - 22;         // spire overlaps the nave roof
		int wallY = towerTop + 2*sp;       // top of the tower wall
		int wallB = y + h + hgt + 4;       // taller than the gable wall
		int apexY = towerTop + sp;
		// long spire shadow over the east slope and the lawn
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Polygon(
			new int [] { sx+9, sx+48, sx+34, sx+5 },
			new int [] { apexY+5, apexY+34, apexY+46, apexY+17 }, 4));
		// pyramid spire from above: north/west faces lit, south/east shaded
		gr.setColor(new Color(248, 246, 240));
		gr.fill(new Polygon(new int [] { sx-sp, sx+sp, sx },
			new int [] { towerTop, towerTop, apexY }, 3));
		gr.setColor(new Color(222, 218, 208));
		gr.fill(new Polygon(new int [] { sx-sp, sx-sp, sx },
			new int [] { towerTop, wallY, apexY }, 3));
		gr.setColor(new Color(178, 172, 160));
		gr.fill(new Polygon(new int [] { sx+sp, sx+sp, sx },
			new int [] { towerTop, wallY, apexY }, 3));
		gr.setColor(new Color(150, 144, 134));
		gr.fill(new Polygon(new int [] { sx-sp, sx+sp, sx },
			new int [] { wallY, wallY, apexY }, 3));
		// gold finial at the apex
		gr.setColor(new Color(226, 188, 80));
		gr.fill(new Ellipse2D.Float(sx-2, apexY-2, 4, 4));
		// tower wall, flush with the gable front and a touch taller
		gr.setColor(stone);
		gr.fill(new Rectangle2D.Float(sx-11, wallY, 22, wallB-wallY));
		gr.setColor(stoneShade);
		gr.fill(new Rectangle2D.Float(sx+4, wallY, 7, wallB-wallY));
		gr.setColor(mixc(stone, Color.BLACK, 0.25f));
		gr.fill(new Rectangle2D.Float(sx-11, wallY, 1, wallB-wallY));
		gr.fill(new Rectangle2D.Float(sx+10, wallY, 1, wallB-wallY));
		// belfry louvers and the entrance door
		gr.setColor(new Color(70, 66, 60));
		gr.fill(new RoundRectangle2D.Float(sx-6, wallY+3, 5, 9, 4, 4));
		gr.fill(new RoundRectangle2D.Float(sx+1, wallY+3, 5, 9, 4, 4));
		gr.setColor(new Color(64, 50, 40));
		gr.fill(new RoundRectangle2D.Float(sx-5, wallB-12, 10, 11, 7, 7));
		gr.setColor(mixc(stone, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(sx-11, wallB-2, 22, 2));

		tree(gr, 34, 56, 14, seed+5);
		tree(gr, 158, 92, 13, seed+6);
		bush(gr, 40, 150, seed+7);
		bush(gr, 152, 146, seed+8);
		gr.dispose();
		return img;
	}

	//
	// single-family houses
	//

	static BufferedImage houseTile(int index)
	{
		int tier = index / 3;
		int seed = 2000 + index*13;
		BufferedImage img = new BufferedImage(T, T, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, tier == 0 ? DIRT : tier == 1 ? PATCHY : tier == 2 ? LAWN : LUSH, seed);

		Graphics2D gr = gfx(img);

		int w = 26 + tier*4 + (int)(4*hash1(index, 3));
		int h = 20 + tier*3;
		int x = 6 + (int)(hash1(index, 1) * (T - w - 16));
		int y = 6 + (int)(hash1(index, 2) * (T - h - 26));
		boolean ridgeAcross = hash1(index, 4) < 0.6f;

		// driveway and car
		if (tier >= 1) {
			boolean right = x + w + 12 <= T - 2;
			int dx = right ? x + w + 2 : Math.max(2, x - 12);
			gr.setColor(new Color(168, 166, 162));
			gr.fill(new Rectangle2D.Float(dx, y+h/2f, 10, T-(y+h/2f)));
			if (tier >= 2) {
				car(gr, dx+1, Math.min(y+h+6, T-14), 8, 12, carColor(index));
			}
		}
		else {
			gr.setColor(new Color(150, 128, 104));
			gr.fill(new Rectangle2D.Float(x+w/2f-3, y+h, 6, T-(y+h)));
		}

		gableHouse(gr, x, y, w, h, ridgeAcross, tier, index);

		// yard greenery
		if (tier >= 2) {
			bush(gr, x-4 < 4 ? x+w+6 : x-4, y+h+8, seed+1);
			tree(gr, hash1(index, 5) < 0.5f ? 10 : T-10, T-12, 8 + tier, seed+2);
		}
		if (tier == 3) {
			pool(gr, x+w/2 > T/2 ? 12 : T-26, T-18);
		}
		gr.dispose();
		return img;
	}

	static Color carColor(int seed)
	{
		Color [] colors = {
			new Color(170, 44, 40), new Color(44, 84, 160),
			new Color(216, 214, 210), new Color(60, 62, 70),
		};
		return colors[(int)(hash1(seed, 6)*colors.length)];
	}

	/** A pitched-roof house seen from above, with eaves and chimney. */
	static void gableHouse(Graphics2D gr, int x, int y, int w, int h,
		boolean ridgeAcross, int tier, int index)
	{
		Color [] [] roofs = {
			{ new Color(140, 122, 102), new Color(82, 72, 62) },    // worn
			{ new Color(134, 140, 154), new Color(84, 88, 100) },   // asphalt
			{ new Color(198, 102, 64), new Color(134, 62, 42) },    // terracotta
			{ new Color(238, 238, 242), new Color(178, 180, 190) }, // luxury
		};
		Color lit = roofs[tier][0];
		Color dark = roofs[tier][1];

		shadow(gr, x, y, w, h, 5);
		// eaves
		gr.setColor(mixc(dark, Color.BLACK, 0.5f));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		if (ridgeAcross) {
			gr.setColor(mixc(lit, Color.WHITE, 0.12f));
			gr.fill(new Rectangle2D.Float(x, y, w, h/2f));
			gr.setColor(dark);
			gr.fill(new Rectangle2D.Float(x, y+h/2f, w, h/2f));
			gr.setColor(mixc(lit, Color.WHITE, 0.55f));
			gr.fill(new Rectangle2D.Float(x, y+h/2f-1, w, 2));
		}
		else {
			gr.setColor(mixc(lit, Color.WHITE, 0.12f));
			gr.fill(new Rectangle2D.Float(x, y, w/2f, h));
			gr.setColor(dark);
			gr.fill(new Rectangle2D.Float(x+w/2f, y, w/2f, h));
			gr.setColor(mixc(lit, Color.WHITE, 0.55f));
			gr.fill(new Rectangle2D.Float(x+w/2f-1, y, 2, h));
		}

		// roof wear or skylight
		if (tier == 0) {
			gr.setColor(new Color(0, 0, 0, 50));
			gr.fill(new Rectangle2D.Float(x+3+8*hash1(index, 7), y+3, 7, 5));
		}
		if (tier == 3) {
			gr.setColor(new Color(150, 180, 210));
			gr.fill(new Rectangle2D.Float(x+5, y+4, 7, 5));
			gr.setColor(new Color(120, 140, 170));
			gr.draw(new Rectangle2D.Float(x+5, y+4, 7, 5));
		}

		// chimney
		int cx = x + (int)(w*0.72f);
		int cy = y + (ridgeAcross ? h/2 - 3 : 4);
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new Rectangle2D.Float(cx+2, cy+2, 6, 6));
		gr.setColor(tier >= 2 ? new Color(146, 84, 66) : new Color(110, 102, 96));
		gr.fill(new Rectangle2D.Float(cx, cy, 6, 6));
		gr.setColor(new Color(50, 46, 44));
		gr.fill(new Rectangle2D.Float(cx+1, cy+1, 4, 4));

		// south wall with door and windows
		gr.setColor(tier == 3 ? new Color(210, 208, 202) : mixc(dark, Color.BLACK, 0.4f));
		gr.fill(new Rectangle2D.Float(x, y+h, w, 7));
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new Rectangle2D.Float(x, y+h+6, w, 1));
		gr.setColor(new Color(56, 44, 36));
		gr.fill(new Rectangle2D.Float(x+w/2f-2, y+h+1, 4, 6));
		gr.setColor(tier == 3 ? new Color(90, 110, 140) : new Color(212, 190, 120));
		gr.fill(new Rectangle2D.Float(x+4, y+h+2, 4, 3));
		gr.fill(new Rectangle2D.Float(x+w-8, y+h+2, 4, 3));
	}

	//
	// ground and hedge
	//

	/**
	 * Textured ground, periodic per 64px tile so it joins the terrain.
	 * Lawn quality rises with land value; LUSH gets mower stripes.
	 */
	static void paintGround(BufferedImage img, int kind, int seed)
	{
		int w = img.getWidth();
		int h = img.getHeight();
		for (int y = 0; y < h; y++) {
			float v = (y % T) / (float)T;
			for (int x = 0; x < w; x++) {
				float u = (x % T) / (float)T;
				float [] col;
				switch (kind) {
				case DIRT:
					col = landTexture(u, v);
					// sparse weeds, like the classic green speckles
					if (noise(u, v, 16, seed+2) > 0.78f) {
						col = mix(col, rgb(74, 118, 56), 0.7f);
					}
					break;
				case PATCHY: {
					float t = smoothstep(0.38f, 0.62f, fbm(u, v, seed+3));
					col = mix(landTexture(u, v), grassTexture(u, v), t);
					break;
				}
				case LAWN:
					col = grassTexture(u, v);
					break;
				default: {
					col = scale(grassTexture(u, v), 1.12f);
					int band = ((x + y) / 10) % 2;
					col = scale(col, band == 0 ? 1.04f : 0.97f);
					break;
				}
				}
				img.setRGB(x, y, pack(col));
			}
		}
	}

	/** The classic green hedge ring around a 3x3 zone block. */
	static void paintHedge(BufferedImage img, boolean southGap, int seed)
	{
		int n = img.getWidth();
		final int W = 5;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int d = Math.min(Math.min(x, n-1-x), Math.min(y, n-1-y));
				if (d >= W) continue;
				if (southGap && y > n-1-W && x > n/2-10 && x < n/2+10) continue;
				float u = (x % T) / (float)T;
				float v = (y % T) / (float)T;
				float lump = noise(u*1f, v*1f, 32, seed+4);
				float [] col = mix(rgb(38, 148, 42), rgb(76, 200, 62), lump);
				if (d == W-1) col = scale(col, 0.72f);   // inner shadow edge
				img.setRGB(x, y, pack(col));
			}
		}
	}

	//
	// building kit
	//

	static Color mixc(Color a, Color b, float t)
	{
		return new Color(
			Math.round(a.getRed() + (b.getRed()-a.getRed())*t),
			Math.round(a.getGreen() + (b.getGreen()-a.getGreen())*t),
			Math.round(a.getBlue() + (b.getBlue()-a.getBlue())*t));
	}

	/**
	 * Ground shadow cast diagonally to the southeast: a sheared band off
	 * the south face that reaches into the lower-right corner, and a
	 * matching sheared band along the east face.
	 */
	static void shadow(Graphics2D gr, int x, int y, int w, int h, int hgt)
	{
		int s = Math.round(hgt*0.7f) + 4;
		int b = y + h + hgt;
		Area a = new Area(new Polygon(
			new int [] { x, x+w, x+w+s, x+s },
			new int [] { b, b, b+s, b+s }, 4));
		a.add(new Area(new Polygon(
			new int [] { x+w, x+w, x+w+s, x+w+s },
			new int [] { y+2, b, b+s, y+2+s }, 4)));
		gr.setColor(new Color(0, 0, 0, 80));
		gr.fill(a);
	}

	/**
	 * A flat-roofed apartment slab: drop shadow, south wall with one
	 * window row per floor (the wall grows with the floor count, so
	 * denser zones read taller), detailed roof. Detail level by land
	 * value: 0 = worn (stains, many AC units), 1 = standard, 2 =
	 * striped awning over the entrance, 3 = luxury (clean roof, glass
	 * corner).
	 */
	static void slab(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int floors, Color roof, Color wall, int seed, int detail)
	{
		int hgt = 3 + floors*6;
		shadow(gr, x, y, w, h, hgt);

		// south wall, one window row per floor
		gr.setColor(wall);
		gr.fill(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setColor(mixc(wall, Color.BLACK, 0.3f));
		gr.fill(new Rectangle2D.Float(x, y+h+hgt-2, w, 2));
		for (int f = 0; f < floors; f++) {
			int wy = y + h + 2 + f*6;
			if (f > 0) {
				gr.setColor(mixc(wall, Color.BLACK, 0.18f));
				gr.fill(new Rectangle2D.Float(x, wy-2, w, 1));
			}
			for (int wx = x+4; wx + 5 <= x+w-3; wx += 8) {
				boolean ground = f == floors-1;
				boolean door = ground && wx + 8 > x + w/2 && wx <= x + w/2;
				boolean litWin = hash2(wx, wy, seed) < 0.14f;
				gr.setColor(door ? new Color(58, 46, 38)
					: litWin ? new Color(238, 206, 120) : new Color(42, 50, 66));
				gr.fill(new Rectangle2D.Float(wx, wy, 5, door ? 5 : 4));
			}
		}

		// roof with a parapet edge
		gr.setColor(mixc(roof, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		gr.setColor(roof);
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, h-2));
		gr.setColor(mixc(roof, Color.WHITE, 0.28f));
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, 2));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 2, h-2));

		// roof texture grain
		roofGrain(img, x+2, y+2, w-4, h-4, seed);

		if (detail == 0) {
			// water stains
			gr.setColor(new Color(0, 0, 0, 28));
			for (int k = 0; k < 3; k++) {
				float sx = x + 4 + hash1(k, seed+11)*(w-18);
				float sy = y + 4 + hash1(k, seed+12)*(h-14);
				gr.fill(new Ellipse2D.Float(sx, sy, 12 + 8*hash1(k, seed+13), 8));
			}
		}
		if (detail == 2) {
			awning(gr, x, y, w, h, seed);
		}
		if (detail == 3) {
			// glass corner: a skylight strip along the north edge
			GradientPaint gp = new GradientPaint(x, y, new Color(188, 208, 228),
				x+w, y+h, new Color(120, 146, 182));
			Paint saved = gr.getPaint();
			gr.setPaint(gp);
			gr.fill(new Rectangle2D.Float(x+4, y+4, w-8, Math.max(6, h/5)));
			gr.setPaint(saved);
			gr.setColor(new Color(255, 255, 255, 90));
			gr.fill(new Rectangle2D.Float(x+4, y+4, w-8, 2));
		}

		// rooftop boxes: AC units and a stair bulkhead
		int units = detail == 0 ? 4 : detail == 3 ? 1 : 2;
		for (int k = 0; k < units; k++) {
			float ax = x + 6 + hash1(k, seed+21)*(w-20);
			float ay = y + 8 + hash1(k, seed+22)*(h-22);
			gr.setColor(new Color(0, 0, 0, 60));
			gr.fill(new Rectangle2D.Float(ax+2, ay+2, 9, 7));
			gr.setColor(new Color(188, 190, 192));
			gr.fill(new Rectangle2D.Float(ax, ay, 9, 7));
			gr.setColor(new Color(120, 122, 126));
			gr.fill(new Ellipse2D.Float(ax+1.5f, ay+1.5f, 4, 4));
		}
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+w-20+2, y+5+2, 13, 9));
		gr.setColor(mixc(roof, Color.BLACK, 0.18f));
		gr.fill(new Rectangle2D.Float(x+w-20, y+5, 13, 9));
	}

	// awning styles for the value-2 buildings (see awning())
	static final int AWNING_RED_STRIPES = 0;
	static final int AWNING_GREEN_STRIPES = 1;
	static final int AWNING_WIDE_SCALLOPED = 2;
	static final int AWNING_ORANGE_STRIPES = 3;
	static final int AWNING_PER_WINDOW = 4;
	static int awningStyle = AWNING_WIDE_SCALLOPED;
	static Color wideAwningColor = new Color(216, 122, 38);

	/** Entrance awning on the value-2 buildings, in the selected style. */
	static void awning(Graphics2D gr, int x, int y, int w, int h, int seed)
	{
		Color cream = new Color(238, 232, 220);
		switch (awningStyle) {
		case AWNING_GREEN_STRIPES:
			stripedAwning(gr, x, y, w, h, Math.min(w-10, 34), 6,
				new Color(38, 110, 62), cream, false);
			break;
		case AWNING_WIDE_SCALLOPED:
			stripedAwning(gr, x, y, w, h, w-8, 8, wideAwningColor, cream, true);
			break;
		case AWNING_ORANGE_STRIPES:
			stripedAwning(gr, x, y, w, h, Math.min(w-10, 34), 6,
				new Color(216, 122, 38), cream, false);
			break;
		case AWNING_PER_WINDOW: {
			// a small scalloped canopy over each ground-floor window
			Color canvas = new Color(140, 38, 44);
			Color canvasLit = new Color(178, 62, 60);
			int wallB = y + h + 1;   // just under the parapet, over row 1
			for (int wx = x+4; wx + 5 <= x+w-3; wx += 8) {
				gr.setColor(new Color(0, 0, 0, 50));
				gr.fill(new Rectangle2D.Float(wx-1, wallB+4, 7, 1));
				gr.setColor(canvas);
				gr.fill(new Rectangle2D.Float(wx-1, wallB, 7, 4));
				gr.setColor(canvasLit);
				gr.fill(new Rectangle2D.Float(wx-1, wallB, 7, 2));
				gr.setColor(canvas);
				gr.fill(new Ellipse2D.Float(wx-1, wallB+3, 3.5f, 2.5f));
				gr.fill(new Ellipse2D.Float(wx+2.5f, wallB+3, 3.5f, 2.5f));
			}
			break;
		}
		default:
			stripedAwning(gr, x, y, w, h, Math.min(w-10, 34), 6,
				new Color(196, 60, 44), cream, false);
			break;
		}
	}

	static void stripedAwning(Graphics2D gr, int x, int y, int w, int h,
		int aw, int depth, Color a, Color b, boolean scalloped)
	{
		int ax = x + (w-aw)/2;
		int ay = y + h - 3;
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Rectangle2D.Float(ax, ay+depth + (scalloped ? 2 : 0), aw, 1));
		for (int k = 0; k*6 < aw; k++) {
			gr.setColor(k % 2 == 0 ? a : b);
			int sw = Math.min(6, aw - k*6);
			gr.fill(new Rectangle2D.Float(ax + k*6, ay, sw, depth));
			if (scalloped) {
				// each stripe ends in a deep half-round scallop
				gr.fill(new Arc2D.Float(ax + k*6, ay+depth-4, sw, 8, 180, 180, Arc2D.CHORD));
			}
		}
		// canvas highlight along the top
		gr.setColor(new Color(255, 255, 255, 70));
		gr.fill(new Rectangle2D.Float(ax, ay, aw, 2));
	}

	/** Subtle per-pixel grain over a roof area. */
	static void roofGrain(BufferedImage img, int x, int y, int w, int h, int seed)
	{
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				int px = x+i, py = y+j;
				if (px < 0 || py < 0 || px >= img.getWidth() || py >= img.getHeight()) continue;
				int p = img.getRGB(px, py);
				float f = 1f + 0.07f*(hash2(px, py, seed) - 0.5f);
				int r = Math.min(255, Math.round(((p>>16)&0xff)*f));
				int g = Math.min(255, Math.round(((p>>8)&0xff)*f));
				int b = Math.min(255, Math.round((p&0xff)*f));
				img.setRGB(px, py, (p & 0xff000000) | (r<<16) | (g<<8) | b);
			}
		}
	}

	/**
	 * The classic luxury round tower, in the same oblique projection as
	 * the slabs: a foreshortened elliptical roof and a tall cylindrical
	 * wall with one window ring per floor and a curved base.
	 */
	static void cylinderTower(Graphics2D gr, BufferedImage img, int x, int y, int d,
		int floors, int seed)
	{
		int eh = Math.round(d*0.55f);      // foreshortened roof ellipse height
		int roofMid = y + eh/2;
		int wallH = eh/2 + 3 + floors*6;   // wall below the roof midline
		int wallB = roofMid + wallH;       // straight wall bottom
		int capH = Math.round(eh*0.5f);    // curved base height

		// ground shadow, cast from the base and sweeping southeast
		int len = Math.round(wallH*0.6f);
		Area shade = new Area();
		for (int k = 0; k <= 6; k++) {
			float t = k/6f;
			shade.add(new Area(new Ellipse2D.Float(
				x + t*len, wallB - capH/2f + t*len*0.55f, d, capH)));
		}
		gr.setColor(new Color(0, 0, 0, 45));
		gr.fill(shade);

		// cylindrical wall: lit toward the west, falling into shade east
		Paint saved = gr.getPaint();
		LinearGradientPaint curve = new LinearGradientPaint(x, 0, x+d, 0,
			new float [] { 0f, 0.28f, 1f },
			new Color [] { new Color(178, 180, 188), new Color(228, 230, 234),
				new Color(104, 106, 118) });
		gr.setPaint(curve);
		gr.fill(new Rectangle2D.Float(x, roofMid, d, wallH));
		gr.fill(new Ellipse2D.Float(x, wallB-capH/2f, d, capH));
		gr.setPaint(saved);

		// window rings wrap the cylinder: each floor line follows the
		// ellipse a horizontal ring traces in this projection, dipping
		// at the front center and rising toward the silhouette edges,
		// with the windows narrowing as they turn away
		float cx = x + d/2f;
		float bowR = eh/2f - 1;
		final int nw = 9;
		for (int f = 0; f < floors; f++) {
			float ringY = roofMid + 4 + f*6;
			for (int k = 0; k < nw; k++) {
				double th = Math.toRadians(-75 + 150.0*k/(nw-1));
				float ww = Math.max(2f, 5.5f*(float)Math.cos(th));
				float wx = cx + (float)Math.sin(th)*(d/2f - 3) - ww/2f;
				float wy = ringY + bowR*(float)Math.cos(th);
				boolean litWin = hash2(Math.round(wx), Math.round(ringY), seed) < 0.14f;
				gr.setColor(litWin ? new Color(238, 206, 120) : new Color(46, 54, 70));
				gr.fill(new Rectangle2D.Float(wx, wy, ww, 4));
			}
		}
		// ground-floor entrance on the curved base
		gr.setColor(new Color(58, 60, 70));
		gr.fill(new Rectangle2D.Float(cx-5, wallB-4, 10, 8));

		// roof ellipse: NW light, SE shade, with a rooftop core
		gr.setColor(new Color(170, 172, 180));
		gr.fill(new Ellipse2D.Float(x, y, d, eh));
		gr.setColor(new Color(216, 218, 224));
		gr.fill(new Arc2D.Float(x+2, y+2, d-4, eh-4, 70, 130, Arc2D.PIE));
		gr.setColor(new Color(136, 138, 148));
		gr.fill(new Arc2D.Float(x+2, y+2, d-4, eh-4, 250, 130, Arc2D.PIE));
		gr.setColor(new Color(192, 194, 202));
		gr.fill(new Ellipse2D.Float(x+d*0.2f, y+eh*0.2f, d*0.6f, eh*0.6f));
		gr.setColor(new Color(158, 160, 170));
		gr.draw(new Ellipse2D.Float(x+d*0.2f, y+eh*0.2f, d*0.6f, eh*0.6f));
		gr.setColor(new Color(118, 122, 132));
		gr.fill(new Ellipse2D.Float(x+d*0.42f, y+eh*0.40f, d*0.16f, eh*0.20f));
	}

	static void path(Graphics2D gr, int x, int y, int w, int h)
	{
		gr.setColor(new Color(186, 184, 178));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		gr.setColor(new Color(160, 158, 152));
		for (int j = y; j < y+h; j += 14) {
			gr.fill(new Rectangle2D.Float(x, j, w, 1));
		}
	}

	static void parking(Graphics2D gr, int x, int y, int w, int h, int seed)
	{
		gr.setColor(new Color(92, 92, 96));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		gr.setColor(new Color(206, 206, 210, 190));
		for (int k = x+6; k < x+w-4; k += 14) {
			gr.fill(new Rectangle2D.Float(k, y+3, 1, h/2f-4));
		}
		int cars = 1 + (int)(hash1(seed, 31)*2.5f);
		for (int k = 0; k < cars; k++) {
			int slot = (int)(hash1(k, seed+32) * ((w-12)/14));
			car(gr, x+7+slot*14, y+5, 11, h/2-8, carColor(seed+k));
		}
	}

	static void car(Graphics2D gr, int x, int y, int w, int h, Color color)
	{
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new RoundRectangle2D.Float(x+1, y+1, w, h, 3, 3));
		gr.setColor(color);
		gr.fill(new RoundRectangle2D.Float(x, y, w, h, 3, 3));
		gr.setColor(mixc(color, Color.BLACK, 0.45f));
		if (w >= h) {
			gr.fill(new Rectangle2D.Float(x+w*0.3f, y+1, w*0.4f, h-2));
		}
		else {
			gr.fill(new Rectangle2D.Float(x+1, y+h*0.3f, w-2, h*0.4f));
		}
	}

	static void dumpster(Graphics2D gr, int x, int y)
	{
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+2, y+2, 14, 9));
		gr.setColor(new Color(48, 84, 72));
		gr.fill(new Rectangle2D.Float(x, y, 14, 9));
		gr.setColor(new Color(34, 62, 54));
		gr.fill(new Rectangle2D.Float(x, y+4, 14, 1));
	}

	static void pool(Graphics2D gr, int x, int y)
	{
		gr.setColor(new Color(212, 208, 198));
		gr.fill(new RoundRectangle2D.Float(x-3, y-3, 26, 20, 6, 6));
		gr.setColor(new Color(70, 140, 210));
		gr.fill(new RoundRectangle2D.Float(x, y, 20, 14, 5, 5));
		gr.setColor(new Color(140, 196, 240));
		gr.draw(new Arc2D.Float(x+3, y+3, 10, 7, 30, 120, Arc2D.OPEN));
	}

	static void tree(Graphics2D gr, float cx, float cy, float r, int seed)
	{
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Ellipse2D.Float(cx-r+r*0.5f, cy-r*0.8f+r*0.5f, r*2, r*1.6f));
		gr.setColor(new Color(24, 66, 30));
		gr.fill(new Ellipse2D.Float(cx-r, cy-r, r*2, r*2));
		for (int k = 0; k < 5; k++) {
			float a = (float)(2*Math.PI*(k + hash1(k, seed))/5);
			float lx = cx + (float)Math.cos(a)*r*0.45f;
			float ly = cy + (float)Math.sin(a)*r*0.45f;
			float lr = r*0.55f;
			boolean litLobe = lx < cx && ly < cy;
			gr.setColor(litLobe ? new Color(86, 138, 64) : new Color(46, 96, 44));
			gr.fill(new Ellipse2D.Float(lx-lr, ly-lr, lr*2, lr*2));
		}
		gr.setColor(new Color(110, 160, 80));
		gr.fill(new Ellipse2D.Float(cx-r*0.55f, cy-r*0.6f, r*0.7f, r*0.55f));
	}

	static void bush(Graphics2D gr, float cx, float cy, int seed)
	{
		gr.setColor(new Color(0, 0, 0, 45));
		gr.fill(new Ellipse2D.Float(cx-4+2, cy-3+2, 9, 7));
		gr.setColor(new Color(36, 84, 40));
		gr.fill(new Ellipse2D.Float(cx-5, cy-4, 10, 8));
		gr.setColor(new Color(72, 124, 58));
		gr.fill(new Ellipse2D.Float(cx-4, cy-3.5f, 6, 4));
	}

	//
	// the "R" zone sign
	//

	static final String [] GLYPH_R = {
		"XXXX.",
		"X...X",
		"X...X",
		"XXXX.",
		"X.X..",
		"X..X.",
		"X...X",
	};

	/** Dark plaque with the classic green "R", centered at (cx, cy). */
	static void plaqueR(Graphics2D gr, int cx, int cy, int scale)
	{
		int gw = 5*scale, gh = 7*scale;
		int pw = gw + 4*scale, ph = gh + 4*scale;
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f+2, cy-ph/2f+2, pw, ph, 4, 4));
		gr.setColor(new Color(46, 50, 46));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		gr.setColor(new Color(88, 94, 88));
		gr.draw(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		glyph(gr, GLYPH_R, cx-gw/2, cy-gh/2, scale, new Color(74, 214, 86));
	}

	static void glyph(Graphics2D gr, String [] rows, int x, int y, int scale, Color color)
	{
		gr.setColor(color);
		for (int j = 0; j < rows.length; j++) {
			for (int i = 0; i < rows[j].length(); i++) {
				if (rows[j].charAt(i) == 'X') {
					gr.fill(new Rectangle2D.Float(x+i*scale, y+j*scale, scale, scale));
				}
			}
		}
	}
}
