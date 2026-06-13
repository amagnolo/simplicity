package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;

/**
 * Draws the deluxe industrial art from scratch at 64px per tile: the
 * ind_zones sheet (empty zone plus 8 factory blocks as 2 land values x
 * 4 densities) and the matching animation overlay sheets — the ind01
 * piston pumps (8 frames) and the ind03/04/07/08 chimney smoke
 * (4 frames each).
 *
 * The redraw keeps the classic tile language: the yellow safety-line
 * border ring (so industrial reads apart from the residential hedge and
 * the commercial blue sidewalk at a glance), the yellow "I" sign, dirt
 * lots at every value, gray corrugated factory halls, and the dark
 * smokestacks with red/blue collar bands, drawn vertical in the same
 * oblique projection as the church bell tower. Sun from the northwest:
 * shadows fall diagonally to the southeast.
 *
 * Animation alignment: tiles.rc composites the smoke frames over fixed
 * tiles of each 3x3 block (ind03/ind08 over column 2 rows 0-1, ind04/
 * ind07 over row 0 columns 1-2) and swaps tile (0,0) of block 1 for the
 * piston frames. The STACKS_* mouth positions are shared between the
 * block renderer (which draws the chimneys) and the smoke renderer
 * (which spawns the plumes), so the smoke always rises exactly off the
 * stack mouths; plumes are clipped to the composited tiles so nothing
 * is lost when MakeTiles cuts the frames apart.
 */
final class DeluxeIndArt
{
	// chimney mouth positions {x, y} per animated block, 64px basis
	static final int [][] STACKS_3 = { {138, 96}, {152, 56} };
	static final int [][] STACKS_4 = { {84, 38}, {122, 34}, {158, 30} };
	static final int [][] STACKS_7 = { {96, 40}, {134, 36} };
	static final int [][] STACKS_8 = { {136, 100}, {150, 72}, {164, 44} };

	static final Color BAND_RED = new Color(188, 52, 44);
	static final Color BAND_BLUE = new Color(64, 92, 188);
	static final Color DRUM_RUST = new Color(150, 84, 48);
	static final Color DRUM_BLUE = new Color(62, 96, 180);
	static final Color DRUM_RED = new Color(188, 60, 48);

	private DeluxeIndArt()
	{
	}

	/** Preview harness: renders all the industrial sheets to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"));
		javax.imageio.ImageIO.write(renderIndZones(), "png",
			new java.io.File(dir, "ind_zones_preview.png"));
		javax.imageio.ImageIO.write(renderPistonFrames(), "png",
			new java.io.File(dir, "ind01_pistons_preview.png"));
		for (int w : new int [] { 3, 4, 7, 8 }) {
			javax.imageio.ImageIO.write(renderSmokeFrames(w), "png",
				new java.io.File(dir, "ind0"+w+"_smoke_preview.png"));
		}
		System.out.println("Previews in "+dir);
	}

	//
	// sheets
	//

	/** The ind_zones sheet: 9 stacked 3x3 blocks, 192x1728. */
	static BufferedImage renderIndZones()
	{
		BufferedImage sheet = new BufferedImage(B, 9*B, BufferedImage.TYPE_INT_ARGB);
		paste(sheet, emptyIndBlock(), 0, 0);
		paste(sheet, pumpWorksBlock(0), 0, B);
		paste(sheet, foundryYardBlock(), 0, 2*B);
		paste(sheet, factoryBlock(), 0, 3*B);
		paste(sheet, heavyPlantBlock(), 0, 4*B);
		paste(sheet, chemicalDepotBlock(), 0, 5*B);
		paste(sheet, gasWorksBlock(), 0, 6*B);
		paste(sheet, processingPlantBlock(), 0, 7*B);
		paste(sheet, megaPlantBlock(), 0, 8*B);
		return sheet;
	}

	/**
	 * The ind01 piston animation: 8 frames in 192px-wide cells, the pump
	 * tile (block 1's northwest corner) in each cell's top-left tile —
	 * same layout as the original 384x48 sheet, scaled to the 64px basis.
	 */
	static BufferedImage renderPistonFrames()
	{
		BufferedImage sheet = new BufferedImage(8*B, B, BufferedImage.TYPE_INT_ARGB);
		for (int f = 0; f < 8; f++) {
			BufferedImage block = pumpWorksBlock(f);
			paste(sheet, block.getSubimage(0, 0, T, T), f*B, 0);
		}
		return sheet;
	}

	/**
	 * A 4-frame smoke overlay sheet (which = 3, 4, 7 or 8): transparent
	 * 192px frames with looping plumes off the matching block's stack
	 * mouths, clipped to the tiles tiles.rc composites them over.
	 */
	static BufferedImage renderSmokeFrames(int which)
	{
		int [][] stacks =
			which == 3 ? STACKS_3 :
			which == 4 ? STACKS_4 :
			which == 7 ? STACKS_7 : STACKS_8;
		// drift northeast; ind03 rises steeply up its column, while
		// ind08's stacks sit on the same diagonal the smoke would ride,
		// so its plumes are thin short wisps that fade before they can
		// bury the next stack up the line
		float dx = which == 3 ? 0.55f : which == 8 ? 0.70f : 0.62f;
		float dy = which == 3 ? -0.80f : which == 8 ? -0.70f : -0.66f;
		float reach = which == 8 ? 30f : 54f;
		float width = which == 8 ? 0.8f : 1f;
		// the composited tiles: col 2 rows 0-1, or row 0 cols 1-2
		Rectangle clip = (which == 3 || which == 8)
			? new Rectangle(2*T, 0, T, 2*T)
			: new Rectangle(T, 0, 2*T, T);

		BufferedImage sheet = new BufferedImage(4*B, B, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 4; f++) {
			gr.setClip(f*B + clip.x, clip.y, clip.width, clip.height);
			for (int s = 0; s < stacks.length; s++) {
				plume(gr, f*B + stacks[s][0], stacks[s][1], dx, dy, reach, width,
					f, which*31+s);
			}
		}
		gr.dispose();
		return sheet;
	}

	/**
	 * One looping plume of dirty smoke: puffs spawn at the stack mouth,
	 * drift along (dx, dy), swell and fade. Every puff advances a quarter
	 * of the puff spacing per frame, so the 4-frame cycle loops
	 * seamlessly. That phase motion is sub-pixel once the sheet is halved
	 * down to 16px, so the wobble term doubles as the low-zoom motion: its
	 * phase rewinds a quarter turn per frame (a traveling wave, looping
	 * after 4 frames), swaying the whole column sideways by a couple of
	 * pixels even on the small sheets. Smoke starts dark brown off the
	 * stack and grays out as it disperses; each puff carries a southeast
	 * shade rim and a bright northwest highlight so it keeps definition
	 * against the gray roofs after downscaling.
	 */
	static void plume(Graphics2D gr, float mx, float my, float dx, float dy,
		float reach, float width, int frame, int seed)
	{
		final int K = 7;
		final float spacing = reach / K;
		final float L = reach;
		for (int k = K-1; k >= 0; k--) {
			float d = (k + frame/4f) * spacing;
			float fade = 1f - d/L;
			if (fade <= 0) continue;
			float wob = (2.5f + 0.07f*d) * (float)Math.sin(
				d*0.26f + hash1(seed, 51)*6.28f - frame*(float)Math.PI/2);
			float cx = mx + dx*d - dy*wob;
			float cy = my + dy*d + dx*wob;
			float r = (6f + 0.38f*d) * width;
			int a = Math.round((175 + 45*width) * (float)Math.pow(fade, 1.1));
			float t = d / L;
			gr.setColor(new Color(58, 50, 44, Math.round(a*0.55f)));
			gr.fill(new Ellipse2D.Float(cx-r+1.5f, cy-r*0.85f+1.5f, 2*r, r*1.7f));
			gr.setColor(new Color(
				Math.round(118 + 38*t), Math.round(100 + 50*t), Math.round(86 + 62*t), a));
			gr.fill(new Ellipse2D.Float(cx-r, cy-r*0.85f, 2*r, r*1.7f));
			gr.setColor(new Color(228, 218, 205, Math.round(a*0.8f)));
			gr.fill(new Ellipse2D.Float(cx-r*0.85f, cy-r*0.95f, r*1.2f, r*0.95f));
		}
	}

	//
	// 3x3 zone blocks
	//

	/** Dirt lot with the yellow safety-line ring, ready to draw on. */
	static BufferedImage baseBlock(int seed)
	{
		BufferedImage img = new BufferedImage(B, B, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, DIRT, seed);
		paintBorder(img, seed);
		return img;
	}

	/** Empty industrial zone: bare dirt, yellow ring, big I sign. */
	static BufferedImage emptyIndBlock()
	{
		BufferedImage img = baseBlock(4000);
		Graphics2D gr = gfx(img);
		// freshly graded lot: a few faint bulldozer stripes
		gr.setColor(new Color(0, 0, 0, 16));
		for (int k = 0; k < 5; k++) {
			int y = 30 + k*28;
			gr.fill(new Rectangle2D.Float(14, y, B-28, 6));
		}
		plaqueI(gr, B/2, B/2, 4);
		gr.dispose();
		return img;
	}

	/**
	 * Block 1 (low value, density 0): a pumping station. The 2x2 bay of
	 * piston pumps fills the northwest corner tile — the part tiles.rc
	 * swaps for the ind01 animation when the zone is powered — feeding
	 * gathering pipes that run out to silver separator tanks with their
	 * dark intake mouths turned to the yard.
	 */
	static BufferedImage pumpWorksBlock(int frame)
	{
		int seed = 4100;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		oilStain(gr, 70, 122, 10, seed);
		oilStain(gr, 150, 70, 8, seed+1);
		pipeline(gr, 36, 56, 36, 84, 142, 84);
		pipeline(gr, 100, 84, 100, 140);

		pistonBay(gr, frame);

		boiler(gr, 28, 116, 30, 8);
		boiler(gr, 120, 102, 34, 9);
		boiler(gr, 96, 164, 30, 8);
		boiler(gr, 148, 156, 34, 9);
		cabinet(gr, 58, 136, 15, 12, new Color(98, 100, 104));
		drum(gr, 166, 28, DRUM_RUST);
		drum(gr, 174, 36, DRUM_RUST);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 2 (low value, density 1): an ore yard — a tan storage hall
	 * with an open ore bin on the roof and an intake hopper at its door,
	 * beside the tall dark foundry hall with its glowing window grid.
	 */
	static BufferedImage foundryYardBlock()
	{
		int seed = 4200;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		oilStain(gr, 120, 152, 9, seed);

		// ore storage hall
		hall(gr, img, 12, 16, 84, 64, 2, new Color(176, 154, 118),
			new Color(112, 98, 76), seed);
		oreBin(gr, 24, 28, 56, 38, seed);
		// intake hopper against the south wall
		cabinet(gr, 30, 100, 42, 14, new Color(70, 72, 78));
		gr.setColor(new Color(44, 45, 50));
		gr.fill(new Polygon(new int [] { 42, 62, 52 }, new int [] { 114, 114, 124 }, 3));
		statusLight(gr, 84, 112, new Color(64, 210, 84));

		// foundry hall
		hall(gr, img, 108, 12, 72, 86, 3, new Color(82, 84, 92),
			new Color(54, 56, 62), seed+1);
		ventGrid(gr, 118, 22, 2, 3);
		ventGrid(gr, 152, 22, 2, 3);
		cabinet(gr, 122, 66, 14, 10, new Color(108, 110, 118));
		cabinet(gr, 150, 70, 14, 10, new Color(108, 110, 118));

		checkFlag(gr, 22, 144);
		drum(gr, 20, 168, DRUM_BLUE);
		drum(gr, 29, 172, DRUM_BLUE);
		crate(gr, 148, 162, seed);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 3 (low value, density 2): the big factory — an office wing
	 * with a hazard-striped service stack, the main production hall with
	 * rooftop vent grids, and the twin leaning smokestacks whose smoke
	 * (ind03) animates over the east column.
	 */
	static BufferedImage factoryBlock()
	{
		int seed = 4300;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		oilStain(gr, 84, 158, 9, seed);

		// office wing
		hall(gr, img, 12, 30, 48, 60, 3, new Color(140, 142, 146),
			new Color(96, 98, 102), seed+1);
		// main production hall
		hall(gr, img, 58, 36, 114, 92, 2, new Color(138, 140, 144),
			new Color(90, 92, 96), seed);
		ventGrid(gr, 70, 52, 3, 3);
		ventGrid(gr, 102, 46, 3, 3);
		roofPipe(gr, 66, 112, 150, 112);

		// hazard-striped service stack on the wing
		chimney(gr, 38, 24, 24, 5.5f, null, true);
		// the twin smokestacks (ind03 smoke rises off these mouths)
		chimney(gr, STACKS_3[0][0], STACKS_3[0][1], 30, 7, BAND_RED, false);
		chimney(gr, STACKS_3[1][0], STACKS_3[1][1], 30, 7, BAND_RED, false);

		checkFlag(gr, 28, 152);
		crate(gr, 128, 160, seed);
		crate(gr, 144, 166, seed+1);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 4 (low value, density 3): the heavy plant — two long halls,
	 * the north one carrying the triple blue-collared stacks (ind04
	 * smoke animates over the top row), the south one with vent batteries
	 * and a transformer cabinet.
	 */
	static BufferedImage heavyPlantBlock()
	{
		int seed = 4400;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		oilStain(gr, 100, 84, 9, seed);

		hall(gr, img, 12, 10, 168, 56, 2, new Color(134, 136, 140),
			new Color(88, 90, 94), seed);
		for (int [] s : STACKS_4) {
			chimney(gr, s[0], s[1], 26, 6.5f, BAND_BLUE, false);
		}

		hall(gr, img, 12, 92, 138, 50, 2, new Color(120, 122, 128),
			new Color(82, 84, 88), seed+1);
		ventGrid(gr, 24, 104, 2, 4);
		ventGrid(gr, 58, 104, 2, 4);
		roofPipe(gr, 92, 100, 92, 136);
		// transformer cabinet east of the lower hall
		cabinet(gr, 156, 96, 24, 38, new Color(64, 66, 72));
		louvers(gr, 159, 102, 18, 22);

		drum(gr, 160, 148, DRUM_RUST);
		drum(gr, 169, 152, DRUM_RUST);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 5 (high value, density 0): a chemical depot — the long
	 * horizontal storage tank, a rack of white sphere tanks plumbed to a
	 * shared header, a tall silo and the indicator-light control board on
	 * a concrete apron.
	 */
	static BufferedImage chemicalDepotBlock()
	{
		int seed = 4500;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		apron(gr, 104, 12, 78, 72, seed);
		controlPanel(gr, 140, 20, seed);
		tankH(gr, 26, 30, 64, 11, seed);

		// manifold linking the sphere tanks
		pipeline(gr, 36, 96, 114, 96);
		pipeline(gr, 36, 108, 36, 96);
		pipeline(gr, 62, 116, 62, 96);
		pipeline(gr, 88, 108, 88, 96);
		pipeline(gr, 114, 116, 114, 96);
		sphereTank(gr, 36, 118, 11);
		sphereTank(gr, 62, 126, 11);
		sphereTank(gr, 88, 118, 11);
		sphereTank(gr, 114, 126, 11);

		silo(gr, 144, 104, 26, 30, new Color(178, 180, 186), seed);
		drum(gr, 26, 156, DRUM_RED);
		drum(gr, 35, 160, DRUM_BLUE);
		drum(gr, 26, 165, DRUM_BLUE);
		crate(gr, 158, 160, seed);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 6 (high value, density 1): the gas works — the huge round
	 * holder with its dark riveted bell, fed by elevated hoppers on the
	 * west side, with a pipe run heading for the south street.
	 */
	static BufferedImage gasWorksBlock()
	{
		int seed = 4600;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		oilStain(gr, 60, 162, 10, seed);
		pipeline(gr, 110, 150, 110, 178);
		pipeline(gr, 20, 96, 52, 96);

		gasHolder(gr, 110, 92, 62, seed);
		hopper(gr, 14, 34, seed);
		hopper(gr, 20, 66, seed+1);

		crate(gr, 22, 150, seed);
		crate(gr, 38, 158, seed+1);
		drum(gr, 162, 158, DRUM_RUST);
		drum(gr, 170, 164, DRUM_RUST);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 7 (high value, density 2): the processing plant — a wide hall
	 * with a louvered vent bank and the twin blue-collared stacks (ind07
	 * smoke animates over the top row), a finishing shop in the southeast
	 * and two colored feed silos.
	 */
	static BufferedImage processingPlantBlock()
	{
		int seed = 4700;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		oilStain(gr, 70, 166, 9, seed);

		hall(gr, img, 12, 12, 160, 54, 2, new Color(134, 136, 140),
			new Color(88, 90, 94), seed);
		louvers(gr, 152, 20, 14, 34);
		for (int [] s : STACKS_7) {
			chimney(gr, s[0], s[1], 26, 6.5f, BAND_BLUE, false);
		}

		// finishing shop
		hall(gr, img, 108, 108, 72, 38, 2, new Color(120, 122, 128),
			new Color(82, 84, 88), seed+1);
		ventGrid(gr, 118, 116, 2, 3);
		silo(gr, 82, 116, 17, 18, new Color(172, 76, 64), seed);
		silo(gr, 58, 122, 15, 16, new Color(84, 102, 176), seed+1);

		crate(gr, 22, 128, seed);
		crate(gr, 20, 144, seed+1);
		drum(gr, 38, 160, DRUM_RUST);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	/**
	 * Block 8 (high value, density 3): the mega plant — one huge dark
	 * hall filling the block, louvered vent banks on the west roof, and
	 * the three red-collared stacks marching up the roof diagonal (ind08
	 * smoke animates over the east column).
	 */
	static BufferedImage megaPlantBlock()
	{
		int seed = 4800;
		BufferedImage img = baseBlock(seed);
		Graphics2D gr = gfx(img);

		hall(gr, img, 14, 12, 158, 118, 3, new Color(84, 86, 94),
			new Color(56, 58, 64), seed);
		// roof panel seams
		gr.setColor(new Color(0, 0, 0, 22));
		for (int k = 54; k < 166; k += 40) {
			gr.fill(new Rectangle2D.Float(k, 14, 1, 114));
		}
		gr.fill(new Rectangle2D.Float(16, 52, 154, 1));
		gr.fill(new Rectangle2D.Float(16, 92, 154, 1));
		louvers(gr, 24, 26, 14, 30);
		louvers(gr, 24, 70, 14, 30);
		ventGrid(gr, 48, 102, 3, 3);

		for (int [] s : STACKS_8) {
			chimney(gr, s[0], s[1], 28, 6.5f, BAND_RED, false);
		}

		// loading dock along the south wall
		apron(gr, 36, 160, 74, 22, seed);
		crate(gr, 44, 164, seed);
		crate(gr, 58, 168, seed+1);
		drum(gr, 80, 168, DRUM_BLUE);
		oilStain(gr, 132, 168, 10, seed);

		plaqueI(gr, B/2, B/2 + 18, 2);
		gr.dispose();
		return img;
	}

	//
	// the yellow border and the "I" sign
	//

	/**
	 * Yellow safety-line ring around the block — the classic industrial
	 * yellow box. Painted concrete: worn patches let the gray show
	 * through, expansion seams cross it, the outer edge catches the sun
	 * and the inner edge drops a curb shadow.
	 */
	static void paintBorder(BufferedImage img, int seed)
	{
		int n = img.getWidth();
		final int W = 6;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int d = Math.min(Math.min(x, n-1-x), Math.min(y, n-1-y));
				if (d >= W) continue;
				float u = (x % T) / (float)T;
				float v = (y % T) / (float)T;
				float lum = 0.92f + 0.14f*noise(u, v, 24, seed+4);
				float [] col = scale(rgb(238, 196, 44), lum);
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

	static final String [] GLYPH_I = {
		"XXXXX",
		"..X..",
		"..X..",
		"..X..",
		"..X..",
		"..X..",
		"XXXXX",
	};

	/** Dark plaque with the classic yellow "I", centered at (cx, cy). */
	static void plaqueI(Graphics2D gr, int cx, int cy, int scale)
	{
		int gw = 5*scale, gh = 7*scale;
		int pw = gw + 4*scale, ph = gh + 4*scale;
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f+2, cy-ph/2f+2, pw, ph, 4, 4));
		gr.setColor(new Color(52, 50, 44));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		gr.setColor(new Color(102, 98, 86));
		gr.draw(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		glyph(gr, GLYPH_I, cx-gw/2, cy-gh/2, scale, new Color(244, 208, 60));
	}

	//
	// industrial building kit
	//

	/**
	 * A factory hall: corrugated metal roof with parapet and rib lines,
	 * south wall with one row of wide workshop windows per floor (a few
	 * glowing warm from the work inside) and a roller door. The
	 * industrial counterpart of the residential slab.
	 */
	static void hall(Graphics2D gr, BufferedImage img, int x, int y, int w, int h,
		int floors, Color roof, Color wall, int seed)
	{
		int hgt = 5 + floors*7;
		shadow(gr, x, y, w, h, hgt);

		// south wall: corrugated metal
		gr.setColor(wall);
		gr.fill(new Rectangle2D.Float(x, y+h, w, hgt));
		gr.setColor(new Color(0, 0, 0, 35));
		for (int wx = x+4; wx < x+w-1; wx += 5) {
			gr.fill(new Rectangle2D.Float(wx, y+h, 1, hgt));
		}
		gr.setColor(mixc(wall, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x, y+h+hgt-2, w, 2));
		// window rows
		for (int f = 0; f < floors; f++) {
			int wy = y + h + 3 + f*7;
			for (int wx = x+4; wx + 8 <= x+w-3; wx += 11) {
				boolean lit = hash2(wx, wy, seed) < 0.16f;
				gr.setColor(lit ? new Color(244, 152, 56) : new Color(38, 44, 56));
				gr.fill(new Rectangle2D.Float(wx, wy, 8, 4));
				gr.setColor(lit ? new Color(255, 210, 120) : new Color(94, 104, 122));
				gr.fill(new Rectangle2D.Float(wx, wy, 8, 1));
			}
		}
		// roller door
		float dw = Math.min(20, w*0.3f);
		float dx = x + w*0.62f - dw/2;
		float dy = y + h + hgt - 2;
		float dh = Math.min(hgt-4, 12);
		gr.setColor(mixc(wall, Color.BLACK, 0.5f));
		gr.fill(new Rectangle2D.Float(dx-1, dy-dh-1, dw+2, dh+1));
		gr.setColor(mixc(wall, Color.WHITE, 0.32f));
		gr.fill(new Rectangle2D.Float(dx, dy-dh, dw, dh));
		gr.setColor(new Color(0, 0, 0, 50));
		for (float j = dy-dh+2; j < dy-1; j += 3) {
			gr.fill(new Rectangle2D.Float(dx, j, dw, 1));
		}

		// roof with parapet
		gr.setColor(mixc(roof, Color.BLACK, 0.35f));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		gr.setColor(roof);
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, h-2));
		gr.setColor(mixc(roof, Color.WHITE, 0.25f));
		gr.fill(new Rectangle2D.Float(x+1, y+1, w-2, 2));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 2, h-2));
		roofGrain(img, x+2, y+2, w-4, h-4, seed);
		// corrugation ribs
		gr.setColor(new Color(0, 0, 0, 26));
		for (int wx = x+6; wx < x+w-4; wx += 6) {
			gr.fill(new Rectangle2D.Float(wx, y+2, 1, h-4));
		}
	}

	/**
	 * A vertical smokestack in the tile's oblique projection, like the
	 * church bell tower: the foreshortened mouth ellipse on top — the
	 * point the smoke frames spawn from — a cylindrical wall dropping
	 * south with a colored collar band (or red-and-white hazard bands)
	 * and a long shadow sweeping southeast. (mx, my) is the mouth
	 * center; the stack stands on its base at (mx, my + hgt).
	 */
	static void chimney(Graphics2D gr, float mx, float my, float hgt, float r,
		Color band, boolean striped)
	{
		float d = 2*r;
		float x = mx - r;
		float eh = d*0.6f;
		float wallB = my + hgt;
		float capH = eh*0.5f;

		// long shadow sweeping southeast from the base
		float len = hgt*0.7f;
		Area shade = new Area();
		for (int k = 0; k <= 6; k++) {
			float t = k/6f;
			shade.add(new Area(new Ellipse2D.Float(
				x + t*len, wallB - capH/2 + t*len*0.55f, d, capH)));
		}
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(shade);

		// wall: flat paint first, curvature shading on top
		Area wall = new Area(new Rectangle2D.Float(x, my, d, hgt));
		wall.add(new Area(new Ellipse2D.Float(x, wallB - capH/2, d, capH)));
		gr.setColor(striped ? new Color(214, 208, 198) : new Color(54, 54, 58));
		gr.fill(wall);
		if (striped) {
			gr.setColor(new Color(192, 56, 44));
			for (float yy = my + 6; yy < wallB - 3; yy += 12) {
				gr.fill(new Rectangle2D.Float(x, yy, d, 6));
			}
		}
		if (band != null) {
			gr.setColor(band);
			gr.fill(new Rectangle2D.Float(x, my + 3, d, 6));
		}
		// cylinder curvature: lit toward the west, falling into shade east
		Paint saved = gr.getPaint();
		gr.setPaint(new LinearGradientPaint(x, 0, x+d, 0,
			new float [] { 0f, 0.28f, 0.65f, 1f },
			new Color [] { new Color(255, 255, 255, 30), new Color(255, 255, 255, 80),
				new Color(0, 0, 0, 30), new Color(0, 0, 0, 110) }));
		gr.fill(wall);
		gr.setPaint(saved);

		// mouth: rim ellipse lit from the northwest, sooty bore inside
		gr.setColor(new Color(118, 120, 126));
		gr.fill(new Ellipse2D.Float(x, my-eh/2, d, eh));
		gr.setColor(new Color(174, 176, 182));
		gr.fill(new Arc2D.Float(x+0.5f, my-eh/2+0.5f, d-1, eh-1, 70, 130, Arc2D.PIE));
		gr.setColor(new Color(66, 67, 72));
		gr.fill(new Arc2D.Float(x+0.5f, my-eh/2+0.5f, d-1, eh-1, 250, 130, Arc2D.PIE));
		gr.setColor(new Color(18, 18, 20));
		gr.fill(new Ellipse2D.Float(x + d*0.16f, my - eh*0.34f, d*0.68f, eh*0.68f));
	}

	/** 2x2 bay of piston pump units in the block's northwest tile. */
	static void pistonBay(Graphics2D gr, int frame)
	{
		for (int k = 0; k < 4; k++) {
			float ux = 8 + (k % 2)*27;
			float uy = 8 + (k / 2)*27;
			pistonUnit(gr, ux, uy, ((frame + k*3) % 8) / 8f);
		}
	}

	/**
	 * One pump unit: a concrete skid carrying a red pump frame, a crank
	 * housing and a silver piston rod stroking diagonally northeast, with
	 * a status lamp blinking green/red with the stroke.
	 */
	static void pistonUnit(Graphics2D gr, float x, float y, float phase)
	{
		// concrete skid
		gr.setColor(new Color(0, 0, 0, 45));
		gr.fill(new Rectangle2D.Float(x+2, y+2, 24, 24));
		gr.setColor(new Color(144, 142, 138));
		gr.fill(new Rectangle2D.Float(x, y, 24, 24));
		gr.setColor(new Color(172, 170, 166));
		gr.fill(new Rectangle2D.Float(x, y, 24, 2));
		// red pump frame
		gr.setColor(new Color(150, 40, 34));
		gr.fill(new Rectangle2D.Float(x+2, y+4, 4, 17));
		gr.fill(new Rectangle2D.Float(x+18, y+4, 4, 17));
		gr.setColor(new Color(196, 58, 46));
		gr.fill(new Rectangle2D.Float(x+2, y+4, 20, 4));
		gr.setColor(new Color(232, 110, 92));
		gr.fill(new Rectangle2D.Float(x+2, y+4, 20, 1.5f));
		// crank housing
		gr.setColor(new Color(64, 66, 70));
		gr.fill(new Rectangle2D.Float(x+3, y+15, 9, 8));
		gr.setColor(new Color(96, 98, 104));
		gr.fill(new Rectangle2D.Float(x+3, y+15, 9, 2));
		// piston rod stroking northeast
		float ext = 5.5f + 4.5f*(float)Math.sin(phase * 2 * Math.PI);
		float px = x + 8, py = y + 18;
		float hx = px + 0.707f*(4 + ext), hy = py - 0.707f*(4 + ext);
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.setColor(new Color(208, 210, 214));
		gr.draw(new Line2D.Float(px, py, hx, hy));
		gr.setStroke(saved);
		// piston head
		gr.setColor(new Color(34, 35, 38));
		gr.fill(new Ellipse2D.Float(hx-3.4f, hy-3.4f, 6.8f, 6.8f));
		gr.setColor(new Color(238, 240, 244));
		gr.fill(new Ellipse2D.Float(hx-2.2f, hy-2.2f, 4.4f, 4.4f));
		// status lamp blinks with the stroke
		boolean up = Math.sin(phase * 2 * Math.PI) > 0;
		Color lamp = up ? new Color(72, 220, 92) : new Color(232, 64, 48);
		gr.setColor(new Color(lamp.getRed(), lamp.getGreen(), lamp.getBlue(), 90));
		gr.fill(new Ellipse2D.Float(x+14.5f, y+0.5f, 7, 7));
		gr.setColor(lamp);
		gr.fill(new Ellipse2D.Float(x+16, y+2, 4, 4));
	}

	/**
	 * A separator/boiler tank leaning toward the viewer: a silver capsule
	 * pointing southwest with a dark round intake mouth. (mx, my) is the
	 * mouth center; the body runs len px northeast.
	 */
	static void boiler(Graphics2D gr, float mx, float my, float len, float r)
	{
		float c = 0.707f;
		float ex = mx + len*c, ey = my - len*c;
		Stroke saved = gr.getStroke();
		// ground shadow
		gr.setColor(new Color(0, 0, 0, 50));
		gr.setStroke(new BasicStroke(2*r, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(mx+3, my+6, ex+3, ey+6));
		// body
		gr.setColor(new Color(126, 128, 132));
		gr.draw(new Line2D.Float(mx, my, ex, ey));
		gr.setColor(new Color(176, 178, 182));
		gr.setStroke(new BasicStroke(r*1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		float o1 = -c*r*0.25f;
		gr.draw(new Line2D.Float(mx+o1, my+o1, ex+o1, ey+o1));
		gr.setColor(new Color(218, 220, 224));
		gr.setStroke(new BasicStroke(r*0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		float o2 = -c*r*0.55f;
		gr.draw(new Line2D.Float(mx+o2, my+o2, ex+o2, ey+o2));
		gr.setStroke(saved);
		// intake mouth
		gr.setColor(new Color(150, 152, 156));
		gr.fill(new Ellipse2D.Float(mx-r, my-r, 2*r, 2*r));
		gr.setColor(new Color(30, 30, 32));
		gr.fill(new Ellipse2D.Float(mx-r*0.7f, my-r*0.7f, r*1.4f, r*1.4f));
		gr.setColor(new Color(232, 234, 238));
		gr.draw(new Arc2D.Float(mx-r, my-r, 2*r, 2*r, 90, 90, Arc2D.OPEN));
	}

	/** Long horizontal storage tank; (x, y) is the west end of the axis. */
	static void tankH(Graphics2D gr, float x, float y, float len, float r, int seed)
	{
		// ground shadow
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new RoundRectangle2D.Float(x-r+3, y-r+6, len+2*r, 2*r, 2*r, 2*r));
		// saddles
		gr.setColor(new Color(70, 72, 76));
		gr.fill(new Rectangle2D.Float(x+6, y+r-2, 5, r*0.6f+2));
		gr.fill(new Rectangle2D.Float(x+len-11, y+r-2, 5, r*0.6f+2));
		// body
		gr.setColor(new Color(142, 144, 148));
		gr.fill(new RoundRectangle2D.Float(x-r, y-r, len+2*r, 2*r, 2*r, 2*r));
		gr.setColor(new Color(186, 188, 192));
		gr.fill(new RoundRectangle2D.Float(x-r+2, y-r+2, len+2*r-4, r, r, r));
		gr.setColor(new Color(226, 228, 232));
		gr.fill(new RoundRectangle2D.Float(x-r+6, y-r+3, len+2*r-16, r*0.4f, r*0.4f, r*0.4f));
		// weld seams
		gr.setColor(new Color(0, 0, 0, 35));
		for (float k = x+8; k < x+len-4; k += 16) {
			gr.fill(new Rectangle2D.Float(k, y-r+1, 1, 2*r-2));
		}
		// filler hatch
		gr.setColor(new Color(54, 56, 60));
		gr.fill(new Ellipse2D.Float(x+len/2-3, y-r+1, 6, 4));
	}

	/** A white pressure sphere with a red relief valve on top. */
	static void sphereTank(Graphics2D gr, float cx, float cy, float r)
	{
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Ellipse2D.Float(cx-r+4, cy+r*0.1f, 2*r, r*1.1f));
		gr.setColor(new Color(162, 164, 168));
		gr.fill(new Ellipse2D.Float(cx-r, cy-r, 2*r, 2*r));
		gr.setColor(new Color(212, 214, 218));
		gr.fill(new Ellipse2D.Float(cx-r*0.95f, cy-r*0.95f, r*1.72f, r*1.72f));
		gr.setColor(new Color(240, 242, 244));
		gr.fill(new Ellipse2D.Float(cx-r*0.65f, cy-r*0.7f, r*0.8f, r*0.7f));
		gr.setColor(new Color(188, 52, 44));
		gr.fill(new Ellipse2D.Float(cx-2.2f, cy-r-1.5f, 4.5f, 4.5f));
	}

	/** A vertical cylinder tank/silo in the tile's oblique projection. */
	static void silo(Graphics2D gr, float x, float y, float d, float wallH,
		Color tint, int seed)
	{
		float eh = d*0.5f;
		// ground shadow
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Ellipse2D.Float(x+4, y+wallH+5, d, eh));
		// cylinder wall, lit toward the west
		Paint saved = gr.getPaint();
		gr.setPaint(new LinearGradientPaint(x, 0, x+d, 0,
			new float [] { 0f, 0.28f, 1f },
			new Color [] { mixc(tint, Color.WHITE, 0.10f),
				mixc(tint, Color.WHITE, 0.42f), mixc(tint, Color.BLACK, 0.38f) }));
		gr.fill(new Rectangle2D.Float(x, y+eh/2, d, wallH));
		gr.fill(new Ellipse2D.Float(x, y+wallH-eh/2, d, eh));
		gr.setPaint(saved);
		// roof ellipse: NW lit, SE shaded
		gr.setColor(mixc(tint, Color.BLACK, 0.12f));
		gr.fill(new Ellipse2D.Float(x, y, d, eh));
		gr.setColor(mixc(tint, Color.WHITE, 0.35f));
		gr.fill(new Arc2D.Float(x+1, y+1, d-2, eh-2, 70, 130, Arc2D.PIE));
		gr.setColor(mixc(tint, Color.BLACK, 0.30f));
		gr.fill(new Arc2D.Float(x+1, y+1, d-2, eh-2, 250, 130, Arc2D.PIE));
		gr.setColor(mixc(tint, Color.WHITE, 0.16f));
		gr.fill(new Ellipse2D.Float(x+d*0.25f, y+eh*0.25f, d*0.5f, eh*0.5f));
		// filler hatch
		gr.setColor(new Color(40, 41, 44));
		gr.fill(new Ellipse2D.Float(x+d*0.42f, y+eh*0.38f, d*0.16f, eh*0.2f));
	}

	/**
	 * The gas holder: a huge steel ring with a dark riveted bell, rim
	 * walkway seams and a center vent hub.
	 */
	static void gasHolder(Graphics2D gr, float cx, float cy, float r, int seed)
	{
		// ground shadow
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Ellipse2D.Float(cx-r+7, cy-r+9, 2*r, 2*r));
		// outer steel ring: NW lit, SE shaded
		gr.setColor(new Color(150, 152, 156));
		gr.fill(new Ellipse2D.Float(cx-r, cy-r, 2*r, 2*r));
		gr.setColor(new Color(206, 208, 212));
		gr.fill(new Arc2D.Float(cx-r+1, cy-r+1, 2*r-2, 2*r-2, 70, 130, Arc2D.PIE));
		gr.setColor(new Color(110, 112, 118));
		gr.fill(new Arc2D.Float(cx-r+1, cy-r+1, 2*r-2, 2*r-2, 250, 130, Arc2D.PIE));
		gr.setColor(new Color(165, 167, 171));
		gr.fill(new Ellipse2D.Float(cx-r*0.88f, cy-r*0.88f, r*1.76f, r*1.76f));
		// rim walkway seams
		gr.setColor(new Color(0, 0, 0, 45));
		float ir = r - 8;
		for (int k = 0; k < 12; k++) {
			double a = Math.PI*2*k/12;
			gr.draw(new Line2D.Float(
				cx + (float)(Math.cos(a)*ir), cy - (float)(Math.sin(a)*ir),
				cx + (float)(Math.cos(a)*r), cy - (float)(Math.sin(a)*r)));
		}
		// the dark gas bell with its riveted frame grid
		gr.setColor(new Color(46, 47, 50));
		gr.fill(new Ellipse2D.Float(cx-ir, cy-ir, 2*ir, 2*ir));
		Shape clip = gr.getClip();
		gr.clip(new Ellipse2D.Float(cx-ir+1, cy-ir+1, 2*ir-2, 2*ir-2));
		int row = 0;
		for (float j = cy-ir; j < cy+ir; j += 8, row++) {
			for (float i = cx-ir + (row % 2)*4; i < cx+ir; i += 8) {
				gr.setColor(new Color(24, 25, 27));
				gr.fill(new Ellipse2D.Float(i, j, 4, 4));
				gr.setColor(new Color(86, 88, 94));
				gr.fill(new Ellipse2D.Float(i+0.7f, j+0.7f, 1.6f, 1.6f));
			}
		}
		// NW sheen on the bell
		gr.setColor(new Color(255, 255, 255, 26));
		gr.fill(new Arc2D.Float(cx-ir+3, cy-ir+3, 2*ir-6, 2*ir-6, 75, 110, Arc2D.PIE));
		gr.setClip(clip);
		// center vent hub
		gr.setColor(new Color(70, 72, 76));
		gr.fill(new Ellipse2D.Float(cx-7, cy-7, 14, 14));
		gr.setColor(new Color(122, 124, 130));
		gr.fill(new Arc2D.Float(cx-7, cy-7, 14, 14, 70, 130, Arc2D.PIE));
		gr.setColor(new Color(36, 37, 40));
		gr.fill(new Ellipse2D.Float(cx-3.5f, cy-3.5f, 7, 7));
	}

	/** An elevated feed hopper on legs with a discharge chute. */
	static void hopper(Graphics2D gr, float x, float y, int seed)
	{
		// legs and their shadow
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Rectangle2D.Float(x+4, y+18, 20, 4));
		gr.setColor(new Color(58, 60, 64));
		gr.fill(new Rectangle2D.Float(x+2, y+10, 3, 10));
		gr.fill(new Rectangle2D.Float(x+17, y+10, 3, 10));
		// bin
		gr.setColor(new Color(132, 134, 138));
		gr.fill(new Rectangle2D.Float(x, y, 22, 13));
		gr.setColor(new Color(168, 170, 174));
		gr.fill(new Rectangle2D.Float(x, y, 22, 3));
		gr.setColor(new Color(40, 41, 44));
		gr.fill(new Rectangle2D.Float(x+3, y+3, 16, 7));
		// discharge chute
		gr.setColor(new Color(90, 92, 96));
		Path2D.Float chute = new Path2D.Float();
		chute.moveTo(x+7, y+13);
		chute.lineTo(x+15, y+13);
		chute.lineTo(x+11, y+20);
		chute.closePath();
		gr.fill(chute);
	}

	/** A ground-level pipe run along the given polyline, flanged joints. */
	static void pipeline(Graphics2D gr, float... pts)
	{
		Path2D.Float p = new Path2D.Float();
		p.moveTo(pts[0], pts[1]);
		for (int i = 2; i < pts.length; i += 2) {
			p.lineTo(pts[i], pts[i+1]);
		}
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.setColor(new Color(0, 0, 0, 45));
		gr.translate(2, 3);
		gr.draw(p);
		gr.translate(-2, -3);
		gr.setColor(new Color(98, 100, 104));
		gr.draw(p);
		gr.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.setColor(new Color(158, 160, 166));
		gr.translate(-1, -1);
		gr.draw(p);
		gr.translate(1, 1);
		gr.setStroke(saved);
		for (int i = 0; i < pts.length; i += 2) {
			gr.setColor(new Color(72, 74, 78));
			gr.fill(new Ellipse2D.Float(pts[i]-4, pts[i+1]-4, 8, 8));
			gr.setColor(new Color(132, 134, 140));
			gr.fill(new Ellipse2D.Float(pts[i]-2.5f, pts[i+1]-3.5f, 5, 5));
		}
	}

	/** A thin pipe gallery running across a roof. */
	static void roofPipe(Graphics2D gr, float x0, float y0, float x1, float y1)
	{
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.setColor(new Color(0, 0, 0, 45));
		gr.draw(new Line2D.Float(x0+1, y0+2, x1+1, y1+2));
		gr.setColor(new Color(78, 80, 84));
		gr.draw(new Line2D.Float(x0, y0, x1, y1));
		gr.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.setColor(new Color(132, 134, 140));
		gr.draw(new Line2D.Float(x0-0.5f, y0-1, x1-0.5f, y1-1));
		gr.setStroke(saved);
	}

	/** Rows of dark intake vents (the classic rooftop dot grids). */
	static void ventGrid(Graphics2D gr, float x, float y, int rows, int cols)
	{
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				float cx = x + c*7;
				float cy = y + r*7;
				gr.setColor(new Color(0, 0, 0, 55));
				gr.fill(new Ellipse2D.Float(cx+1, cy+1, 4.5f, 4.5f));
				gr.setColor(new Color(30, 32, 34));
				gr.fill(new Ellipse2D.Float(cx, cy, 4.5f, 4.5f));
				gr.setColor(new Color(118, 122, 126));
				gr.fill(new Ellipse2D.Float(cx+0.8f, cy+0.8f, 1.6f, 1.6f));
			}
		}
	}

	/** A bank of horizontal louver slats. */
	static void louvers(Graphics2D gr, float x, float y, float w, float h)
	{
		gr.setColor(new Color(0, 0, 0, 40));
		gr.fill(new Rectangle2D.Float(x-1, y-1, w+2, h+2));
		for (float j = y; j+2 <= y+h; j += 3) {
			gr.setColor(new Color(56, 58, 62));
			gr.fill(new Rectangle2D.Float(x, j, w, 2));
			gr.setColor(new Color(120, 122, 128));
			gr.fill(new Rectangle2D.Float(x, j, w, 0.8f));
		}
	}

	/** A small equipment cabinet/junction box. */
	static void cabinet(Graphics2D gr, float x, float y, float w, float h, Color c)
	{
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Rectangle2D.Float(x+2, y+2, w, h));
		gr.setColor(mixc(c, Color.BLACK, 0.3f));
		gr.fill(new Rectangle2D.Float(x-0.5f, y-0.5f, w+1, h+1));
		gr.setColor(c);
		gr.fill(new Rectangle2D.Float(x, y, w, h-2));
		gr.setColor(mixc(c, Color.WHITE, 0.25f));
		gr.fill(new Rectangle2D.Float(x, y, w, 1.5f));
	}

	/** The control board with rows of colored indicator lights. */
	static void controlPanel(Graphics2D gr, float x, float y, int seed)
	{
		Color [] lights = {
			new Color(224, 60, 48), new Color(64, 200, 84),
			new Color(70, 110, 230), new Color(238, 196, 60),
		};
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Rectangle2D.Float(x+3, y+4, 34, 52));
		gr.setColor(new Color(50, 52, 58));
		gr.fill(new Rectangle2D.Float(x, y, 34, 52));
		gr.setColor(new Color(96, 100, 108));
		gr.draw(new Rectangle2D.Float(x, y, 34, 52));
		for (int j = 0; j < 7; j++) {
			for (int i = 0; i < 4; i++) {
				float lx = x + 4.5f + i*7;
				float ly = y + 4.5f + j*7;
				Color c = lights[(int)(hash2(i, j, seed)*lights.length)];
				boolean lit = hash2(i, j, seed+1) < 0.6f;
				gr.setColor(lit ? c : mixc(c, Color.BLACK, 0.6f));
				gr.fill(new Ellipse2D.Float(lx, ly, 4, 4));
				if (lit) {
					gr.setColor(new Color(255, 255, 255, 170));
					gr.fill(new Ellipse2D.Float(lx+0.8f, ly+0.8f, 1.6f, 1.6f));
				}
			}
		}
	}

	/** Open ore/sand bin set into a roof, filled with granular material. */
	static void oreBin(Graphics2D gr, int x, int y, int w, int h, int seed)
	{
		gr.setColor(new Color(70, 62, 50));
		gr.fill(new Rectangle2D.Float(x-2, y-2, w+4, h+4));
		gr.setColor(new Color(112, 90, 64));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		for (int k = 0; k < w*h/6; k++) {
			float gx = x + hash1(k, seed+71)*(w-2);
			float gy = y + hash1(k, seed+72)*(h-2);
			float s = 1.5f + hash1(k, seed+73);
			gr.setColor(hash1(k, seed+74) < 0.5f
				? new Color(86, 68, 46) : new Color(150, 124, 88));
			gr.fill(new Ellipse2D.Float(gx, gy, s, s));
		}
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x, y, w, 2));
		gr.fill(new Rectangle2D.Float(x, y, 2, h));
	}

	/** Cracked concrete work apron with expansion seams. */
	static void apron(Graphics2D gr, float x, float y, float w, float h, int seed)
	{
		gr.setColor(new Color(134, 132, 128));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		gr.setColor(new Color(158, 156, 152));
		gr.fill(new Rectangle2D.Float(x, y, w, 2));
		gr.setColor(new Color(0, 0, 0, 30));
		for (float k = x+16; k < x+w-2; k += 16) {
			gr.fill(new Rectangle2D.Float(k, y, 1, h));
		}
		for (float k = y+16; k < y+h-2; k += 16) {
			gr.fill(new Rectangle2D.Float(x, k, w, 1));
		}
	}

	/** The blue-and-white checkered yard flag. */
	static void checkFlag(Graphics2D gr, float x, float y)
	{
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Rectangle2D.Float(x+1.5f, y+2, 1.5f, 18));
		gr.setColor(new Color(70, 72, 76));
		gr.fill(new Rectangle2D.Float(x, y, 2, 19));
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < 4; i++) {
				gr.setColor((i+j) % 2 == 0
					? new Color(58, 92, 196) : new Color(238, 240, 244));
				gr.fill(new Rectangle2D.Float(x+2 + i*3.5f, y + j*3f, 3.5f, 3f));
			}
		}
	}

	/** A pole-mounted status beacon. */
	static void statusLight(Graphics2D gr, float x, float y, Color c)
	{
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Rectangle2D.Float(x+1.5f, y+2, 2, 12));
		gr.setColor(new Color(64, 66, 70));
		gr.fill(new Rectangle2D.Float(x, y, 2, 13));
		gr.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
		gr.fill(new Ellipse2D.Float(x-3, y-7, 8, 8));
		gr.setColor(c);
		gr.fill(new Ellipse2D.Float(x-1.5f, y-5.5f, 5, 5));
		gr.setColor(new Color(255, 255, 255, 200));
		gr.fill(new Ellipse2D.Float(x-0.5f, y-4.5f, 2, 2));
	}

	/** A 55-gallon drum seen from above. */
	static void drum(Graphics2D gr, float x, float y, Color c)
	{
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Ellipse2D.Float(x+1.5f, y+1.5f, 8, 8));
		gr.setColor(mixc(c, Color.BLACK, 0.35f));
		gr.fill(new Ellipse2D.Float(x, y, 8, 8));
		gr.setColor(c);
		gr.fill(new Ellipse2D.Float(x+1, y+1, 6, 6));
		gr.setColor(mixc(c, Color.WHITE, 0.45f));
		gr.fill(new Ellipse2D.Float(x+1.8f, y+1.8f, 2.6f, 2.6f));
	}

	/** A wooden shipping crate. */
	static void crate(Graphics2D gr, float x, float y, int seed)
	{
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Rectangle2D.Float(x+2, y+2, 12, 10));
		gr.setColor(new Color(120, 96, 62));
		gr.fill(new Rectangle2D.Float(x, y, 12, 10));
		gr.setColor(new Color(158, 128, 86));
		gr.fill(new Rectangle2D.Float(x, y, 12, 2));
		gr.setColor(new Color(86, 68, 44));
		gr.fill(new Rectangle2D.Float(x+5.5f, y, 1, 10));
	}

	/** A dark oil stain soaked into the dirt. */
	static void oilStain(Graphics2D gr, float cx, float cy, float r, int seed)
	{
		gr.setColor(new Color(20, 16, 12, 30));
		for (int k = 0; k < 3; k++) {
			float ox = (hash1(k, seed+61) - 0.5f) * r;
			float oy = (hash1(k, seed+62) - 0.5f) * r * 0.7f;
			float rr = r * (0.6f + 0.5f*hash1(k, seed+63));
			gr.fill(new Ellipse2D.Float(cx+ox-rr, cy+oy-rr*0.7f, rr*2, rr*1.4f));
		}
	}
}
