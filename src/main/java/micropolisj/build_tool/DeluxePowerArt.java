package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;
import static micropolisj.build_tool.DeluxeIndArt.*;
import static micropolisj.build_tool.DeluxeCivicArt.pad;

/**
 * Draws the deluxe power plants from scratch at 64px per tile: the coal
 * plant (4x4) with its coal_smoke_frames overlay sheet, and the nuclear
 * plant (4x4) with the reactor swirl frames that GenerateDeluxeArt
 * patches into misc_animation.
 *
 * Both keep the classic identity marks: the yellow safety ring shared
 * with the industrial zones, the coal plant's four collar-banded stacks
 * marching up the northeast diagonal (the smoke animation composites
 * over that 2x2 quadrant), the nuclear plant's two huge cooling-tower
 * mouths seen straight from above, and the yellow lightning-bolt plaque
 * both plants carry. The stack mouths and the reactor pad are shared
 * constants between the block renderers and the animation renderers, so
 * the overlays line up pixel-exact. Sun from the northwest.
 */
final class DeluxePowerArt
{
	/** Coal stack mouths {x, y}, up the NE diagonal; 64px basis. */
	static final int [][] STACKS_COAL = {
		{ 146, 106 }, { 174, 80 }, { 202, 54 }, { 230, 28 },
	};

	/** The reactor pad: tile (1,2) of the nuclear block, 64px basis. */
	static final Rectangle REACTOR = new Rectangle(T, 2*T, T, T);

	private DeluxePowerArt()
	{
	}

	/** Preview harness: renders the power sheets to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"));
		javax.imageio.ImageIO.write(renderCoal(), "png",
			new java.io.File(dir, "coal_preview.png"));
		javax.imageio.ImageIO.write(renderCoalSmoke(), "png",
			new java.io.File(dir, "coal_smoke_preview.png"));
		javax.imageio.ImageIO.write(renderNuclear(), "png",
			new java.io.File(dir, "nuclear_preview.png"));
		javax.imageio.ImageIO.write(renderNuclearSwirl(), "png",
			new java.io.File(dir, "nuclear_swirl_preview.png"));
		System.out.println("Previews in "+dir);
	}

	//
	// coal plant
	//

	/**
	 * Coal plant, 4x4: coal yard and conveyor feeding the boiler house,
	 * the long turbine hall, a transformer yard, and the four banded
	 * smokestacks up the northeast diagonal.
	 */
	static BufferedImage renderCoal()
	{
		int seed = 5500;
		BufferedImage img = new BufferedImage(4*T, 4*T, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, DIRT, seed);
		paintBorder(img, seed);

		Graphics2D gr = gfx(img);

		oilStain(gr, 120, 150, 12, seed);
		oilStain(gr, 60, 96, 9, seed+1);

		// coal yard in the northwest: two heaps and the conveyor east
		coalHeap(gr, 42, 42, 28, seed+2);
		coalHeap(gr, 70, 62, 22, seed+3);
		conveyor(gr, 64, 44, 116, 78);

		// boiler house, center-west, feeding the stack manifold
		hall(gr, img, 100, 60, 76, 50, 3, new Color(96, 98, 106),
			new Color(60, 62, 70), seed);
		ventGrid(gr, 110, 70, 2, 3);
		roofPipe(gr, 150, 70, 168, 70);

		// flue duct from the boiler house up the stack line
		pipeline(gr, 176, 96, 226, 50);

		// turbine hall, the long south block
		hall(gr, img, 22, 138, 150, 48, 3, new Color(82, 84, 92),
			new Color(52, 54, 62), seed+1);
		ventGrid(gr, 34, 148, 2, 4);
		ventGrid(gr, 120, 148, 2, 4);
		louvers(gr, 80, 146, 28, 10);

		// transformer yard southeast, where the power leaves the plant
		pad(gr, 186, 142, 56, 44, seed);
		cabinet(gr, 192, 150, 16, 12, new Color(98, 100, 104));
		cabinet(gr, 214, 150, 16, 12, new Color(98, 100, 104));
		cabinet(gr, 202, 168, 16, 12, new Color(108, 110, 118));
		statusLight(gr, 234, 148, new Color(64, 210, 84));

		// the four stacks up the northeast diagonal
		for (int [] s : STACKS_COAL) {
			chimney(gr, s[0], s[1], 38, 10, BAND_RED, false);
		}

		drum(gr, 26, 116, DRUM_RUST);
		drum(gr, 35, 120, DRUM_RUST);
		crate(gr, 178, 112, seed);

		boltPlaque(gr, 96, 120, 3);
		gr.dispose();
		return img;
	}

	/**
	 * The coal_smoke_frames sheet: 4 frames of the full 4x4 block, with
	 * looping plumes off the four stacks. Like the ind08 wisps, the
	 * stacks sit on the very diagonal the smoke rides, so the plumes are
	 * kept short and thin to fade before they bury the next stack;
	 * tiles.rc composites them over the northeast 2x2 quadrant.
	 */
	static BufferedImage renderCoalSmoke()
	{
		BufferedImage sheet = new BufferedImage(4*4*T, 4*T, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 4; f++) {
			gr.setClip(f*4*T + 2*T, 0, 2*T, 2*T);
			for (int s = 0; s < STACKS_COAL.length; s++) {
				plume(gr, f*4*T + STACKS_COAL[s][0], STACKS_COAL[s][1],
					0.70f, -0.70f, 30f, 0.8f, f, 15*31+s);
			}
		}
		gr.dispose();
		return sheet;
	}

	/** A dark coal heap with ridges and a sheen along the lit slope. */
	static void coalHeap(Graphics2D gr, float x, float y, float r, int seed)
	{
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Ellipse2D.Float(x-r+4, y-r*0.7f+5, 2*r, r*1.5f));
		gr.setColor(new Color(38, 38, 42));
		gr.fill(new Ellipse2D.Float(x-r, y-r*0.75f, 2*r, r*1.6f));
		for (int k = 0; k < 4; k++) {
			float lx = x - r*0.5f + hash1(k, seed)*r;
			float ly = y - r*0.4f + hash1(k, seed+1)*r*0.7f;
			float lr = r*0.30f;
			gr.setColor(lx < x && ly < y ? new Color(82, 82, 90) : new Color(54, 54, 60));
			gr.fill(new Ellipse2D.Float(lx-lr, ly-lr*0.8f, 2*lr, lr*1.6f));
		}
		gr.setColor(new Color(140, 140, 150, 120));
		gr.fill(new Ellipse2D.Float(x-r*0.45f, y-r*0.55f, r*0.5f, r*0.3f));
	}

	/** An elevated belt conveyor between two points, on A-frame legs. */
	static void conveyor(Graphics2D gr, float x0, float y0, float x1, float y1)
	{
		Stroke saved = gr.getStroke();
		gr.setColor(new Color(0, 0, 0, 45));
		gr.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(x0+3, y0+5, x1+3, y1+5));
		gr.setColor(new Color(74, 76, 82));
		gr.draw(new Line2D.Float(x0, y0, x1, y1));
		gr.setColor(new Color(128, 130, 136));
		gr.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(x0, y0, x1, y1));
		gr.setStroke(new BasicStroke(1.4f));
		gr.setColor(new Color(40, 41, 45));
		float dx = x1-x0, dy = y1-y0;
		float len = (float)Math.hypot(dx, dy);
		for (float t = 8; t < len-6; t += 14) {
			float px = x0 + dx*t/len, py = y0 + dy*t/len;
			gr.draw(new Line2D.Float(px-2, py+3, px-2, py+9));
			gr.draw(new Line2D.Float(px+2, py+3, px+2, py+9));
		}
		gr.setStroke(saved);
	}

	//
	// nuclear plant
	//

	/**
	 * Nuclear plant, 4x4: the two huge cooling-tower mouths along the
	 * north, the white containment dome, the turbine hall with the navy
	 * atom painted on its roof, and the fenced reactor pad whose swirl
	 * animation lights up when the plant powers on.
	 */
	static BufferedImage renderNuclear()
	{
		int seed = 5600;
		BufferedImage img = new BufferedImage(4*T, 4*T, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, PATCHY, seed);
		paintBorder(img, seed);

		Graphics2D gr = gfx(img);

		// the two cooling towers, straight from above
		coolingTower(gr, 78, 64, 46);
		coolingTower(gr, 178, 64, 46);

		// containment dome southwest: the white reactor sphere
		sphereTank(gr, 44, 156, 28);

		// reactor pad: the swirl frames replace this tile when powered
		reactorPad(gr, seed);

		// turbine hall southeast, atom painted on the roof
		hall(gr, img, 140, 142, 102, 44, 3, new Color(108, 110, 118),
			new Color(64, 66, 74), seed);
		atomSymbol(gr, 191, 164, 16, new Color(36, 48, 96), 2.2f);

		// switchyard between the dome and the hall
		cabinet(gr, 96, 196, 15, 11, new Color(98, 100, 104));
		cabinet(gr, 116, 198, 15, 11, new Color(108, 110, 118));
		statusLight(gr, 136, 196, new Color(64, 210, 84));

		boltPlaque(gr, 226, 116, 3);
		gr.dispose();
		return img;
	}

	/**
	 * The nuclear swirl frames: 4 tiles for misc_animation — the reactor
	 * pad with the atom orbits glowing and turning 15 degrees per frame.
	 * Three orbits 60 degrees apart give the loop its symmetry: after 4
	 * frames the figure maps onto itself.
	 */
	static BufferedImage renderNuclearSwirl()
	{
		BufferedImage nuclear = renderNuclear();
		BufferedImage sheet = new BufferedImage(T, 4*T, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 4; f++) {
			gr.drawImage(nuclear.getSubimage(REACTOR.x, REACTOR.y, T, T),
				0, f*T, null);
			float cx = T/2f;
			float cy = f*T + T/2f;
			// the glow breathes with the turn
			float glow = 0.8f + 0.2f*(float)Math.sin(2*Math.PI*f/4);
			gr.setColor(new Color(255, 150, 40, Math.round(60*glow)));
			gr.fill(new Ellipse2D.Float(cx-20, cy-20, 40, 40));
			gr.setColor(new Color(255, 220, 120, Math.round(90*glow)));
			gr.fill(new Ellipse2D.Float(cx-9, cy-9, 18, 18));
			atomOrbits(gr, cx, cy, 17, f*15f,
				new Color(255, 196, 60), new Color(255, 240, 180), 2f);
		}
		gr.dispose();
		return sheet;
	}

	/** The fenced concrete reactor pad with the etched, unlit atom. */
	static void reactorPad(Graphics2D gr, int seed)
	{
		float x = REACTOR.x + 4, y = REACTOR.y + 4, s = T - 8;
		pad(gr, x, y, s, s, seed);
		// hazard edging
		gr.setColor(new Color(212, 178, 40));
		gr.fill(new Rectangle2D.Float(x, y, s, 3));
		gr.fill(new Rectangle2D.Float(x, y+s-3, s, 3));
		gr.fill(new Rectangle2D.Float(x, y, 3, s));
		gr.fill(new Rectangle2D.Float(x+s-3, y, 3, s));
		gr.setColor(new Color(40, 40, 44));
		for (int k = 0; k < (int)s; k += 8) {
			gr.fill(new Rectangle2D.Float(x+k, y, 4, 3));
			gr.fill(new Rectangle2D.Float(x+s-4-k, y+s-3, 4, 3));
			gr.fill(new Rectangle2D.Float(x, y+s-4-k, 3, 4));
			gr.fill(new Rectangle2D.Float(x+s-3, y+k, 3, 4));
		}
		// the reactor cap and the unlit orbits
		gr.setColor(new Color(120, 122, 128));
		gr.fill(new Ellipse2D.Float(x+s/2-14, y+s/2-14, 28, 28));
		gr.setColor(new Color(96, 98, 104));
		gr.draw(new Ellipse2D.Float(x+s/2-14, y+s/2-14, 28, 28));
		atomOrbits(gr, x+s/2, y+s/2, 15, 0,
			new Color(70, 72, 80), new Color(150, 152, 160), 1.6f);
	}

	/** Three elliptical orbits with electrons and a bright nucleus. */
	static void atomOrbits(Graphics2D gr, float cx, float cy, float r,
		float angle, Color orbit, Color bright, float stroke)
	{
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(stroke));
		gr.setColor(orbit);
		for (int k = 0; k < 3; k++) {
			double a = Math.toRadians(angle + k*60);
			AffineTransform old = gr.getTransform();
			gr.rotate(a, cx, cy);
			gr.draw(new Ellipse2D.Float(cx-r, cy-r*0.42f, 2*r, r*0.84f));
			gr.setTransform(old);
		}
		// electrons riding the orbits
		gr.setColor(bright);
		for (int k = 0; k < 3; k++) {
			double a = Math.toRadians(angle + k*60);
			double th = Math.toRadians(angle*4 + k*120);
			float ex = (float)(Math.cos(th)*r), ey = (float)(Math.sin(th)*r*0.42f);
			float px = cx + (float)(ex*Math.cos(a) - ey*Math.sin(a));
			float py = cy + (float)(ex*Math.sin(a) + ey*Math.cos(a));
			gr.fill(new Ellipse2D.Float(px-1.6f, py-1.6f, 3.2f, 3.2f));
		}
		gr.fill(new Ellipse2D.Float(cx-2.5f, cy-2.5f, 5, 5));
		gr.setStroke(saved);
	}

	/**
	 * A cooling tower seen straight from above: the concrete rim ring,
	 * the dark throat with the water glint at the bottom, and a faint
	 * vapor sheen curling off the northwest rim.
	 */
	static void coolingTower(Graphics2D gr, float cx, float cy, float r)
	{
		shadowDisc(gr, cx-r, cy-r, 2*r);
		// concrete shell: NW lit, SE shaded
		gr.setColor(new Color(168, 168, 172));
		gr.fill(new Ellipse2D.Float(cx-r, cy-r, 2*r, 2*r));
		gr.setColor(new Color(206, 206, 210));
		gr.fill(new Arc2D.Float(cx-r+1, cy-r+1, 2*r-2, 2*r-2, 70, 130, Arc2D.PIE));
		gr.setColor(new Color(126, 126, 132));
		gr.fill(new Arc2D.Float(cx-r+1, cy-r+1, 2*r-2, 2*r-2, 250, 130, Arc2D.PIE));
		// rim ribs
		gr.setColor(new Color(0, 0, 0, 40));
		for (int k = 0; k < 16; k++) {
			double a = Math.PI*2*k/16;
			gr.draw(new Line2D.Float(
				cx + (float)Math.cos(a)*(r-7), cy - (float)Math.sin(a)*(r-7),
				cx + (float)Math.cos(a)*(r-1), cy - (float)Math.sin(a)*(r-1)));
		}
		// the throat falling away into the dark
		float ir = r - 8;
		gr.setColor(new Color(52, 54, 58));
		gr.fill(new Ellipse2D.Float(cx-ir, cy-ir, 2*ir, 2*ir));
		gr.setColor(new Color(26, 27, 30));
		gr.fill(new Ellipse2D.Float(cx-ir*0.75f, cy-ir*0.75f, ir*1.5f, ir*1.5f));
		// water glint at the bottom of the shaft
		gr.setColor(new Color(48, 66, 76));
		gr.fill(new Ellipse2D.Float(cx-ir*0.32f, cy-ir*0.32f, ir*0.64f, ir*0.64f));
		gr.setColor(new Color(96, 124, 138, 130));
		gr.fill(new Ellipse2D.Float(cx-ir*0.26f, cy-ir*0.28f, ir*0.22f, ir*0.16f));
		// vapor sheen off the NW rim
		gr.setColor(new Color(255, 255, 255, 60));
		gr.fill(new Arc2D.Float(cx-ir*0.9f, cy-ir*0.9f, ir*1.8f, ir*1.8f, 95, 70, Arc2D.PIE));
	}

	/** Soft round ground shadow under a disc-footprint structure. */
	static void shadowDisc(Graphics2D gr, float x, float y, float d)
	{
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Ellipse2D.Float(x+5, y+7, d, d));
	}

	/** A navy atom symbol painted flat on a roof. */
	static void atomSymbol(Graphics2D gr, float cx, float cy, float r, Color c, float stroke)
	{
		atomOrbits(gr, cx, cy, r, 0, c, mixc(c, Color.WHITE, 0.35f), stroke);
	}

	/** Dark plaque with the classic yellow lightning bolt. */
	static void boltPlaque(Graphics2D gr, int cx, int cy, int scale)
	{
		int pw = 9*scale, ph = 11*scale;
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f+2, cy-ph/2f+2, pw, ph, 4, 4));
		gr.setColor(new Color(52, 50, 44));
		gr.fill(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		gr.setColor(new Color(102, 98, 86));
		gr.draw(new RoundRectangle2D.Float(cx-pw/2f, cy-ph/2f, pw, ph, 4, 4));
		boltGlyph(gr, cx, cy, scale, new Color(244, 208, 60));
	}

	/** The lightning bolt zigzag, centered at (cx, cy). */
	static void boltGlyph(Graphics2D gr, float cx, float cy, float scale, Color c)
	{
		Path2D.Float p = new Path2D.Float();
		p.moveTo(cx + 1.5f*scale, cy - 4.5f*scale);
		p.lineTo(cx - 2.5f*scale, cy + 0.5f*scale);
		p.lineTo(cx - 0.5f*scale, cy + 0.5f*scale);
		p.lineTo(cx - 1.5f*scale, cy + 4.5f*scale);
		p.lineTo(cx + 2.5f*scale, cy - 0.5f*scale);
		p.lineTo(cx + 0.5f*scale, cy - 0.5f*scale);
		p.closePath();
		gr.setColor(new Color(0, 0, 0, 90));
		AffineTransform old = gr.getTransform();
		gr.translate(1, 1);
		gr.fill(p);
		gr.setTransform(old);
		gr.setColor(c);
		gr.fill(p);
	}
}
