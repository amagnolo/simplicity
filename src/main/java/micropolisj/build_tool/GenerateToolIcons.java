package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import static micropolisj.build_tool.ProcArt.halve;

/**
 * Draws the toolbar tool icons (icdozr.png, icres.png, ...) as stylized
 * glyphs for the modern and deluxe graphics skins, so the toolbar can
 * follow the selected theme (the classic skin keeps the original 1980s
 * pixel-art icons that live at the resources root).
 *
 * Every icon is drawn at the exact pixel size the classic icon uses so
 * the toolbar layout is unchanged. Each tool has a normal and a selected
 * ("...hi") variant; the selected variant adds a highlighted plate behind
 * the glyph. The two skins share one set of glyph shapes and differ only
 * in palette/shading: modern is flat with a crisp outline, deluxe adds
 * vertical gradients and brighter highlights.
 *
 * Glyphs are rendered antialiased at 4x and box-filtered down (halve
 * twice) for smooth edges.
 *
 * Outputs into &lt;resources&gt;/tools (modern) and
 * &lt;resources&gt;/deluxe/tools (deluxe).
 *
 * Usage: java micropolisj.build_tool.GenerateToolIcons &lt;resources-dir&gt;
 */
public class GenerateToolIcons
{
	static final int SC = 4;   // supersample factor (halved twice => /4)

	enum Style { MODERN, DELUXE }

	interface Glyph
	{
		/** Draws the glyph in the target [0,w] x [0,h] coordinate space. */
		void draw(Graphics2D gr, float w, float h, Style style);
	}

	static final class Spec
	{
		final String base;
		final int w, h;
		final Glyph glyph;
		Spec(String base, int w, int h, Glyph glyph)
		{
			this.base = base; this.w = w; this.h = h; this.glyph = glyph;
		}
	}

	public static void main(String [] args)
		throws Exception
	{
		File res = new File(args.length > 0 ? args[0] : "src/main/resources");
		new GenerateToolIcons().run(res);
	}

	void run(File res)
		throws Exception
	{
		File modern = new File(res, "tools");
		File deluxe = new File(res, "deluxe/tools");
		modern.mkdirs();
		deluxe.mkdirs();

		for (Spec s : SPECS) {
			write(modern, s, Style.MODERN);
			write(deluxe, s, Style.DELUXE);
		}
	}

	void write(File dir, Spec s, Style style)
		throws Exception
	{
		writeOne(new File(dir, s.base + ".png"), s, style, false);
		writeOne(new File(dir, s.base + "hi.png"), s, style, true);
	}

	void writeOne(File f, Spec s, Style style, boolean selected)
		throws Exception
	{
		System.out.println("Generating tool icon: " + f);
		ImageIO.write(render(s, style, selected), "png", f);
	}

	BufferedImage render(Spec s, Style style, boolean selected)
	{
		int w = s.w * SC, h = s.h * SC;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = img.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gr.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		gr.scale(SC, SC);
		if (selected) {
			drawSelectedPlate(gr, s.w, s.h, style);
		}
		s.glyph.draw(gr, s.w, s.h, style);
		gr.dispose();
		return halve(halve(img));
	}

	/** The accent panel behind a pressed tool's glyph. */
	static void drawSelectedPlate(Graphics2D gr, float w, float h, Style style)
	{
		float pad = 1.5f;
		RoundRectangle2D r = new RoundRectangle2D.Float(
			pad, pad, w - 2*pad, h - 2*pad, 6, 6);
		if (style == Style.DELUXE) {
			gr.setPaint(new GradientPaint(0, 0, new Color(255, 226, 138),
				0, h, new Color(247, 178, 56)));
		}
		else {
			gr.setColor(new Color(255, 205, 92));
		}
		gr.fill(r);
		gr.setColor(new Color(190, 120, 24));
		gr.setStroke(new BasicStroke(1f));
		gr.draw(r);
	}

	//
	// fill helpers (flat for modern, vertical gradient for deluxe)
	//

	static void fill(Graphics2D gr, Shape s, Color c, Style style)
	{
		if (style == Style.DELUXE) {
			Rectangle2D b = s.getBounds2D();
			float y0 = (float) b.getMinY(), y1 = (float) Math.max(b.getMaxY(), b.getMinY() + 1);
			gr.setPaint(new GradientPaint(0, y0, lighten(c, 0.28f), 0, y1, darken(c, 0.20f)));
		}
		else {
			gr.setColor(c);
		}
		gr.fill(s);
	}

	static void outline(Graphics2D gr, Shape s, Color c)
	{
		gr.setColor(c);
		gr.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(s);
	}

	/** Fill plus a darker outline — the common case. */
	static void solid(Graphics2D gr, Shape s, Color c, Style style)
	{
		fill(gr, s, c, style);
		outline(gr, s, darken(c, 0.42f));
	}

	static Color lighten(Color c, float t)
	{
		return new Color(
			Math.round(c.getRed()   + (255 - c.getRed())   * t),
			Math.round(c.getGreen() + (255 - c.getGreen()) * t),
			Math.round(c.getBlue()  + (255 - c.getBlue())  * t));
	}

	static Color darken(Color c, float t)
	{
		return new Color(
			Math.round(c.getRed()   * (1 - t)),
			Math.round(c.getGreen() * (1 - t)),
			Math.round(c.getBlue()  * (1 - t)));
	}

	static RoundRectangle2D rr(float x, float y, float w, float h, float arc)
	{
		return new RoundRectangle2D.Float(x, y, w, h, arc, arc);
	}

	//
	// the 16 tool glyphs (drawn centered in their [0,w] x [0,h] frame)
	//

	static final Spec [] SPECS = {
		new Spec("icdozr", 34, 34, GenerateToolIcons::bulldozer),
		new Spec("icwire", 34, 34, GenerateToolIcons::wire),
		new Spec("icroad", 56, 24, GenerateToolIcons::road),
		new Spec("icrail", 56, 24, GenerateToolIcons::rail),
		new Spec("icres",  34, 50, GenerateToolIcons::residential),
		new Spec("iccom",  34, 50, GenerateToolIcons::commercial),
		new Spec("icind",  34, 50, GenerateToolIcons::industrial),
		new Spec("icpark", 34, 34, GenerateToolIcons::park),
		new Spec("icfire", 34, 34, GenerateToolIcons::fire),
		new Spec("icpol",  34, 34, GenerateToolIcons::police),
		new Spec("icqry",  34, 34, GenerateToolIcons::query),
		new Spec("icstad", 42, 42, GenerateToolIcons::stadium),
		new Spec("icseap", 42, 42, GenerateToolIcons::seaport),
		new Spec("iccoal", 42, 42, GenerateToolIcons::coal),
		new Spec("icnuc",  42, 42, GenerateToolIcons::nuclear),
		new Spec("icairp", 58, 58, GenerateToolIcons::airport),
	};

	static final Color ASPHALT = new Color(86, 90, 96);
	static final Color STEEL    = new Color(150, 156, 164);

	static void bulldozer(Graphics2D gr, float w, float h, Style style)
	{
		// side view, facing right (like the classic icon): orange body, a
		// rear cab with a blue window over tracked wheels, blade at front
		Color body = new Color(238, 120, 36);
		Color cab  = new Color(247, 150, 64);
		Color trk  = new Color(54, 56, 62);
		float wy = h-7;

		// blade push-arm and the curved blade at the front (right)
		gr.setColor(new Color(58, 60, 66));
		gr.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(23, 18, 28, 19));
		// the concave (scooping) face opens forward, toward the road
		Path2D.Float bladeCurve = new Path2D.Float();
		bladeCurve.moveTo(31, 10);
		bladeCurve.quadTo(27f, 18, 31, 26.5f);
		Shape blade = new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
			.createStrokedShape(bladeCurve);
		solid(gr, blade, new Color(122, 126, 134), style);

		// tracks with road wheels and a larger drive sprocket at the rear
		solid(gr, rr(2, h-12, w-8, 10, 6), trk, style);
		float [] wx = { 8, 13.5f, 19, 24 };
		for (float x : wx) {
			fill(gr, new Ellipse2D.Float(x-2.5f, wy-2.5f, 5, 5),
				new Color(150, 154, 160), style);
			outline(gr, new Ellipse2D.Float(x-2.5f, wy-2.5f, 5, 5),
				new Color(96, 100, 106));
		}
		solid(gr, new Ellipse2D.Float(2.5f, wy-4, 8, 8), new Color(120, 124, 132), style);

		// body, exhaust stack and the rear cab with its window
		solid(gr, rr(5, 12, 21, 9, 3), body, style);
		gr.setColor(new Color(48, 50, 56));
		gr.fill(rr(11.5f, 5, 2.4f, 7, 1));
		Path2D.Float cabS = new Path2D.Float();
		cabS.moveTo(6, 13); cabS.lineTo(6, 8);
		cabS.curveTo(7, 6, 9, 6, 10, 6);
		cabS.lineTo(15, 6); cabS.lineTo(15, 13); cabS.closePath();
		solid(gr, cabS, cab, style);
		fill(gr, rr(7.5f, 8.5f, 6, 4.2f, 1), new Color(120, 170, 210), style);
	}

	static void wire(Graphics2D gr, float w, float h, Style style)
	{
		float cx = w/2;
		// pylon
		gr.setColor(STEEL);
		gr.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(cx-7, h-3, cx, 6));
		gr.draw(new Line2D.Float(cx+7, h-3, cx, 6));
		gr.draw(new Line2D.Float(cx-9, h-3, cx+9, h-3));
		// cross-arms
		gr.draw(new Line2D.Float(cx-9, 12, cx+9, 12));
		gr.draw(new Line2D.Float(cx-6, 18, cx+6, 18));
		gr.setColor(darken(STEEL, 0.3f));
		gr.setStroke(new BasicStroke(1.3f));
		gr.draw(new Line2D.Float(cx-7, h-4, cx+5, 10));
		gr.draw(new Line2D.Float(cx+7, h-4, cx-5, 10));
		// electricity: a lightning bolt
		solid(gr, bolt(cx+6, 20), new Color(255, 222, 70), style);
	}

	/** A five-pointed star with its top point up. */
	static Path2D star5(float cx, float cy, float r)
	{
		Path2D.Float p = new Path2D.Float();
		for (int i = 0; i < 10; i++) {
			double a = -Math.PI/2 + i * Math.PI/5;
			float rr = (i % 2 == 0) ? r : r*0.40f;
			float x = cx + (float)Math.cos(a)*rr;
			float y = cy + (float)Math.sin(a)*rr;
			if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
		}
		p.closePath();
		return p;
	}

	/** A lightning bolt centered at (cx,cy), about 13px tall. */
	static Path2D bolt(float cx, float cy)
	{
		Path2D.Float p = new Path2D.Float();
		p.moveTo(cx+1.6f, cy-6.5f);
		p.lineTo(cx-3.2f, cy+0.6f);
		p.lineTo(cx-0.3f, cy+0.6f);
		p.lineTo(cx-2.0f, cy+6.5f);
		p.lineTo(cx+3.4f, cy-1.6f);
		p.lineTo(cx+0.5f, cy-1.6f);
		p.closePath();
		return p;
	}

	static void road(Graphics2D gr, float w, float h, Style style)
	{
		float cy = h/2;
		solid(gr, rr(2, cy-7, w-4, 14, 4), ASPHALT, style);
		// dashed center line
		gr.setColor(new Color(248, 224, 96));
		for (float x = 5; x < w-6; x += 9) {
			gr.fill(new Rectangle2D.Float(x, cy-1.2f, 5, 2.4f));
		}
		// edge highlights
		gr.setColor(lighten(ASPHALT, 0.2f));
		gr.setStroke(new BasicStroke(1f));
		gr.draw(new Line2D.Float(4, cy-5.5f, w-4, cy-5.5f));
	}

	static void rail(Graphics2D gr, float w, float h, Style style)
	{
		float cy = h/2;
		solid(gr, rr(2, cy-7, w-4, 14, 4), new Color(112, 84, 58), style);
		// ties
		gr.setColor(new Color(70, 50, 34));
		for (float x = 5; x < w-5; x += 7) {
			gr.fill(new Rectangle2D.Float(x, cy-6, 3, 12));
		}
		// rails
		gr.setColor(STEEL);
		gr.setStroke(new BasicStroke(1.8f));
		gr.draw(new Line2D.Float(4, cy-3, w-4, cy-3));
		gr.draw(new Line2D.Float(4, cy+3, w-4, cy+3));
	}

	static void building(Graphics2D gr, float w, float h, Style style,
		Color wall, Color roof, int floors, int cols, boolean litWindows)
	{
		float bw = w-12, bx = 6, by = 8, bh = h-12;
		// roof block
		solid(gr, rr(bx-1, by-4, bw+2, 7, 2), roof, style);
		// body
		solid(gr, rr(bx, by, bw, bh, 2), wall, style);
		// windows grid
		float mx = 3, gw = (bw-2*mx)/cols, gh = (bh-8)/floors;
		for (int r = 0; r < floors; r++) {
			for (int c = 0; c < cols; c++) {
				Color win = litWindows && ((r+c) % 2 == 0)
					? new Color(255, 226, 130) : new Color(120, 170, 205);
				gr.setColor(win);
				gr.fill(rr(bx+mx + c*gw + gw*0.18f, by+5 + r*gh + gh*0.18f,
					gw*0.64f, gh*0.6f, 1));
			}
		}
		// door
		gr.setColor(darken(wall, 0.5f));
		gr.fill(rr(bx + bw/2 - 3, by+bh-7, 6, 7, 1));
	}

	static void residential(Graphics2D gr, float w, float h, Style style)
	{
		Color wall = new Color(118, 184, 96);
		// pitched roof house
		float bx = 6, bw = w-12, by = 18, bh = h-24;
		Path2D.Float roof = new Path2D.Float();
		roof.moveTo(bx-2, by+2); roof.lineTo(w/2, by-12); roof.lineTo(bx+bw+2, by+2);
		roof.closePath();
		solid(gr, roof, new Color(176, 78, 56), style);
		solid(gr, rr(bx, by, bw, bh, 2), wall, style);
		// door + windows
		gr.setColor(new Color(96, 60, 40));
		gr.fill(rr(w/2-3, by+bh-9, 6, 9, 1));
		gr.setColor(new Color(255, 226, 130));
		gr.fill(rr(bx+3, by+4, 6, 6, 1));
		gr.fill(rr(bx+bw-9, by+4, 6, 6, 1));
	}

	static void commercial(Graphics2D gr, float w, float h, Style style)
	{
		building(gr, w, h, style, new Color(84, 150, 210),
			new Color(60, 110, 168), 5, 3, true);
	}

	static void industrial(Graphics2D gr, float w, float h, Style style)
	{
		Color wall = new Color(214, 176, 70);
		float bx = 5, bw = w-10, by = 20, bh = h-26;
		// sawtooth factory roof
		Path2D.Float roof = new Path2D.Float();
		roof.moveTo(bx, by);
		for (int i = 0; i < 3; i++) {
			float x0 = bx + i*bw/3f;
			roof.lineTo(x0, by-7);
			roof.lineTo(x0 + bw/3f, by);
		}
		roof.closePath();
		solid(gr, roof, darken(wall, 0.25f), style);
		solid(gr, rr(bx, by, bw, bh, 2), wall, style);
		// smokestack + smoke
		solid(gr, rr(bx+bw-7, by-18, 5, 18, 1), new Color(150, 96, 70), style);
		gr.setColor(new Color(170, 170, 174, 200));
		gr.fill(new Ellipse2D.Float(bx+bw-9, by-24, 9, 9));
		// windows
		gr.setColor(new Color(120, 170, 205));
		for (int c = 0; c < 3; c++) {
			gr.fill(rr(bx+3 + c*(bw-6)/3f, by+bh/2f, (bw-6)/3f-3, 6, 1));
		}
	}

	static void park(Graphics2D gr, float w, float h, Style style)
	{
		float cx = w/2;
		// grassy mound
		fill(gr, new Ellipse2D.Float(3, h-12, w-6, 10), new Color(120, 178, 92), style);
		// trunk
		solid(gr, rr(cx-2.5f, h-18, 5, 11, 1), new Color(120, 82, 48), style);
		// canopy
		Color leaf = new Color(76, 158, 74);
		solid(gr, new Ellipse2D.Float(cx-12, 5, 24, 20), leaf, style);
		fill(gr, new Ellipse2D.Float(cx-9, 6, 12, 11), lighten(leaf, 0.22f), style);
	}

	static void fire(Graphics2D gr, float w, float h, Style style)
	{
		float cx = w/2;
		Path2D.Float flame = new Path2D.Float();
		flame.moveTo(cx, 4);
		flame.curveTo(cx+13, 14, cx+10, 27, cx, h-4);
		flame.curveTo(cx-11, 27, cx-13, 13, cx, 4);
		flame.closePath();
		solid(gr, flame, new Color(232, 86, 40), style);
		Path2D.Float inner = new Path2D.Float();
		inner.moveTo(cx, 13);
		inner.curveTo(cx+7, 19, cx+5, 27, cx, h-8);
		inner.curveTo(cx-6, 27, cx-7, 19, cx, 13);
		inner.closePath();
		fill(gr, inner, new Color(252, 196, 64), style);
	}

	static void police(Graphics2D gr, float w, float h, Style style)
	{
		float cx = w/2, cy = h/2;
		// shield
		Path2D.Float shield = new Path2D.Float();
		shield.moveTo(cx, 4);
		shield.lineTo(w-7, 9);
		shield.curveTo(w-7, h-13, cx+6, h-5, cx, h-3);
		shield.curveTo(cx-6, h-5, 7, h-13, 7, 9);
		shield.closePath();
		solid(gr, shield, new Color(64, 110, 188), style);
		// five-pointed star
		solid(gr, star5(cx, cy-1, 8), new Color(252, 224, 120), style);
	}

	static void query(Graphics2D gr, float w, float h, Style style)
	{
		// magnifier
		float r = Math.min(w, h)*0.30f, cx = w*0.42f, cy = h*0.40f;
		Ellipse2D lens = new Ellipse2D.Float(cx-r, cy-r, 2*r, 2*r);
		gr.setColor(new Color(60, 74, 90));
		gr.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(cx + r*0.7f, cy + r*0.7f, w-5, h-5));
		fill(gr, lens, new Color(170, 214, 240), style);
		gr.setColor(new Color(60, 74, 90));
		gr.draw(lens);
		// question mark
		gr.setColor(new Color(40, 60, 96));
		gr.setFont(new Font("SansSerif", Font.BOLD, Math.round(r*1.7f)));
		FontMetrics fm = gr.getFontMetrics();
		String q = "?";
		gr.drawString(q, cx - fm.stringWidth(q)/2f, cy + fm.getAscent()*0.36f);
	}

	static void stadium(Graphics2D gr, float w, float h, Style style)
	{
		// oval stands
		solid(gr, new Ellipse2D.Float(3, 9, w-6, h-16), new Color(196, 200, 206), style);
		// field
		fill(gr, new Ellipse2D.Float(9, 14, w-18, h-26), new Color(86, 158, 80), style);
		gr.setColor(new Color(235, 240, 235));
		gr.setStroke(new BasicStroke(1f));
		gr.draw(new Line2D.Float(w/2, 15, w/2, h-12));
		gr.draw(new Ellipse2D.Float(w/2-4, h/2-4, 8, 8));
	}

	static void seaport(Graphics2D gr, float w, float h, Style style)
	{
		// water
		fill(gr, rr(3, h-12, w-6, 9, 3), new Color(86, 132, 206), style);
		float cx = w/2;
		Color metal = new Color(70, 86, 110);
		gr.setColor(metal);
		gr.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		// shank
		gr.draw(new Line2D.Float(cx, 10, cx, h-12));
		// ring
		gr.draw(new Ellipse2D.Float(cx-4, 5, 8, 8));
		// stock
		gr.draw(new Line2D.Float(cx-8, 15, cx+8, 15));
		// arms
		Path2D.Float arms = new Path2D.Float();
		arms.moveTo(cx-12, h-18);
		arms.curveTo(cx-12, h-11, cx-5, h-9, cx, h-9);
		arms.curveTo(cx+5, h-9, cx+12, h-11, cx+12, h-18);
		gr.draw(arms);
		gr.setColor(darken(metal, 0.3f));
		gr.fill(new Ellipse2D.Float(cx-12.5f, h-21, 5, 5));
		gr.fill(new Ellipse2D.Float(cx+7.5f, h-21, 5, 5));
	}

	static void coal(Graphics2D gr, float w, float h, Style style)
	{
		Color wall = new Color(132, 138, 146);
		solid(gr, rr(4, h-22, w-8, 18, 2), wall, style);
		// two stacks
		for (int i = 0; i < 2; i++) {
			float sx = 9 + i*16;
			solid(gr, rr(sx, 10, 8, h-26, 1), darken(wall, 0.15f), style);
			gr.setColor(new Color(40, 42, 46));
			gr.fill(rr(sx, 10, 8, 3, 1));
			// smoke
			gr.setColor(new Color(168, 168, 172, 200));
			gr.fill(new Ellipse2D.Float(sx-3, 2, 9, 9));
		}
		// stripe
		gr.setColor(new Color(220, 170, 60));
		gr.fill(new Rectangle2D.Float(5, h-13, w-10, 3));
	}

	static void nuclear(Graphics2D gr, float w, float h, Style style)
	{
		// cooling tower (hyperboloid)
		float cx = w/2;
		Path2D.Float tower = new Path2D.Float();
		tower.moveTo(cx-11, 8);
		tower.curveTo(cx-5, h/2, cx-7, h-9, cx-12, h-6);
		tower.lineTo(cx+12, h-6);
		tower.curveTo(cx+7, h-9, cx+5, h/2, cx+11, 8);
		tower.closePath();
		solid(gr, tower, new Color(212, 216, 222), style);
		gr.setColor(new Color(168, 172, 180));
		gr.fill(new Ellipse2D.Float(cx-11, 5, 22, 6));
		// radiation trefoil
		gr.setColor(new Color(214, 60, 48));
		float ty = h*0.56f;
		gr.fill(new Ellipse2D.Float(cx-2.5f, ty-2.5f, 5, 5));
		for (int i = 0; i < 3; i++) {
			double a = -Math.PI/2 + i*2*Math.PI/3;
			Arc2D.Float arc = new Arc2D.Float(cx-8, ty-8, 16, 16,
				(float)Math.toDegrees(a)-30, 60, Arc2D.PIE);
			gr.fill(arc);
		}
		gr.setColor(new Color(212, 216, 222));
		gr.fill(new Ellipse2D.Float(cx-4, ty-4, 8, 8));
		gr.setColor(new Color(214, 60, 48));
		gr.fill(new Ellipse2D.Float(cx-2, ty-2, 4, 4));
	}

	static void airport(Graphics2D gr, float w, float h, Style style)
	{
		// runway tarmac
		fill(gr, rr(4, h-13, w-8, 9, 3), ASPHALT, style);
		gr.setColor(new Color(240, 230, 120));
		gr.fill(new Rectangle2D.Float(7, h-9, w-14, 1.5f));

		// a top-down airliner modeled on the airplane sprite: white
		// fuselage, swept wings with red tips, blue cockpit, tail spine
		Color white = new Color(238, 240, 244);
		Color wing  = new Color(200, 205, 214);
		Color red   = new Color(206, 52, 44);
		Color edge  = new Color(70, 76, 86);

		AffineTransform saved = gr.getTransform();
		gr.translate(w/2f, (h-11)/2f + 1);
		gr.rotate(Math.toRadians(45));   // nose up to the right
		gr.scale(0.29f, 0.29f);
		Stroke os = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		// engine pods slung under the wings
		for (int side = -1; side <= 1; side += 2) {
			fill(gr, rr(side*30-4.5f, -8, 9, 18, 6), new Color(150, 154, 162), style);
		}
		// main wings, swept back, red wingtips
		for (int side = -1; side <= 1; side += 2) {
			Polygon wp = new Polygon(
				new int[]{ side*8, side*66, side*66, side*8 },
				new int[]{ -14, 22, 31, 18 }, 4);
			fill(gr, wp, wing, style);
			gr.setColor(edge); gr.setStroke(os); gr.draw(wp);
			gr.setColor(red);
			gr.fill(new Polygon(new int[]{ side*66, side*66, side*57 },
				new int[]{ 22, 31, 28 }, 3));
		}
		// tailplane
		for (int side = -1; side <= 1; side += 2) {
			Polygon tp = new Polygon(
				new int[]{ side*3, side*27, side*23, side*3 },
				new int[]{ 46, 62, 67, 58 }, 4);
			fill(gr, tp, wing, style);
			gr.setColor(edge); gr.setStroke(os); gr.draw(tp);
		}
		// fuselage
		Shape fus = rr(-9, -76, 18, 148, 17);
		fill(gr, fus, white, style);
		gr.setColor(edge); gr.setStroke(os); gr.draw(fus);
		// cockpit glass at the nose
		gr.setColor(new Color(46, 60, 84));
		gr.fill(new Arc2D.Float(-7, -74, 14, 16, 0, 180, Arc2D.CHORD));
		// cabin window lines
		gr.setColor(new Color(120, 140, 165));
		gr.fill(new Rectangle2D.Float(-7.5f, -52, 2, 100));
		gr.fill(new Rectangle2D.Float(5.5f, -52, 2, 100));
		// vertical stabilizer: a red-tipped spine at the tail
		gr.setColor(new Color(178, 182, 192));
		gr.fill(rr(-2.5f, 44, 5, 28, 3));
		gr.setColor(red);
		gr.fill(rr(-2.5f, 62, 5, 10, 3));
		gr.setTransform(saved);
	}
}
