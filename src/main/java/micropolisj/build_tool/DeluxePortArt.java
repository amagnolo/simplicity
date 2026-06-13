package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;
import static micropolisj.build_tool.DeluxeIndArt.*;
import static micropolisj.build_tool.DeluxeCivicArt.*;

/**
 * Draws the deluxe transport hubs from scratch at 64px per tile: the
 * seaport (4x4) and the airport (6x6), plus the airport's rotating
 * radar frames that GenerateDeluxeArt patches into misc_animation.
 *
 * The seaport keeps the classic blue ring and the navy anchor painted
 * on the warehouse roof; the airport keeps the yellow ring, the two
 * crossing runways with their threshold numbers, and the radar tile on
 * the north edge — tile (2,0) of the block, whose ground the radar
 * frames crop so the dish animation sits pixel-exact on the redrawn
 * airport. Sun from the northwest.
 */
final class DeluxePortArt
{
	/** The radar tile: tile (2,0) of the airport block, 64px basis. */
	static final Rectangle RADAR = new Rectangle(2*T, 0, T, T);

	private DeluxePortArt()
	{
	}

	/** Preview harness: renders the port sheets to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"));
		javax.imageio.ImageIO.write(renderSeaport(), "png",
			new java.io.File(dir, "seaport_preview.png"));
		javax.imageio.ImageIO.write(renderAirport(), "png",
			new java.io.File(dir, "airport_preview.png"));
		javax.imageio.ImageIO.write(renderRadarFrames(), "png",
			new java.io.File(dir, "radar_preview.png"));
		System.out.println("Previews in "+dir);
	}

	//
	// seaport
	//

	/**
	 * Seaport, 4x4: a paved quay with the anchor-roofed cargo hall, a
	 * container yard, the gantry crane working the east apron, and the
	 * harbor office.
	 */
	static BufferedImage renderSeaport()
	{
		int seed = 5700;
		BufferedImage img = new BufferedImage(4*T, 4*T, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, DIRT, seed);
		paintRing(img, new Color(58, 86, 196), seed);

		Graphics2D gr = gfx(img);

		// the whole interior is paved quay
		pad(gr, 6, 6, 4*T-12, 4*T-12, seed);
		// lane markings to the south gate
		gr.setColor(new Color(222, 196, 60, 180));
		for (int j = 150; j < 244; j += 16) {
			gr.fill(new Rectangle2D.Float(124, j, 4, 9));
		}

		// cargo hall with the navy anchor on the roof
		hall(gr, img, 24, 30, 122, 78, 2, new Color(168, 170, 176),
			new Color(108, 112, 120), seed);
		anchorIcon(gr, 85, 66, 26, new Color(36, 48, 96));

		// harbor office with the radio mast
		slab(gr, img, 26, 138, 56, 34, 2, new Color(140, 148, 158),
			new Color(92, 98, 108), seed+1, 1);
		gr.setColor(new Color(210, 212, 218));
		gr.fill(new Rectangle2D.Float(76, 128, 2, 14));
		statusLight(gr, 80, 126, new Color(64, 210, 84));

		// container yard southeast
		containerRow(gr, 152, 128, 4, seed);
		containerRow(gr, 152, 152, 4, seed+1);
		containerRow(gr, 170, 176, 3, seed+2);

		// gantry crane straddling the east apron
		gantryCrane(gr, 162, 36, 76, 74);

		drum(gr, 16, 196, DRUM_BLUE);
		drum(gr, 25, 200, DRUM_BLUE);
		crate(gr, 100, 150, seed);
		crate(gr, 112, 158, seed+3);
		// bollards along the quay edges
		gr.setColor(new Color(70, 72, 78));
		for (int k = 0; k < 5; k++) {
			gr.fill(new Ellipse2D.Float(16, 30 + k*40, 5, 5));
			gr.fill(new Ellipse2D.Float(236, 30 + k*40, 5, 5));
		}
		gr.dispose();
		return img;
	}

	/** A row of shipping containers, stacked two high here and there. */
	static void containerRow(Graphics2D gr, float x, float y, int n, int seed)
	{
		Color [] colors = {
			new Color(186, 68, 50), new Color(62, 110, 180),
			new Color(70, 140, 84), new Color(214, 130, 48),
			new Color(150, 152, 158),
		};
		for (int k = 0; k < n; k++) {
			float cx = x + k*20;
			Color c = colors[(int)(hash2(k, (int)y, seed)*colors.length)];
			gr.setColor(new Color(0, 0, 0, 60));
			gr.fill(new Rectangle2D.Float(cx+2, y+3, 18, 10));
			gr.setColor(c);
			gr.fill(new Rectangle2D.Float(cx, y, 18, 10));
			gr.setColor(mixc(c, Color.WHITE, 0.30f));
			gr.fill(new Rectangle2D.Float(cx, y, 18, 2));
			gr.setColor(mixc(c, Color.BLACK, 0.35f));
			for (float i = cx+3; i < cx+16; i += 3) {
				gr.fill(new Rectangle2D.Float(i, y+2, 1, 8));
			}
			if (hash2(k*3, (int)y, seed+1) < 0.4f) {
				Color c2 = colors[(int)(hash2(k*7, (int)y, seed+2)*colors.length)];
				gr.setColor(c2);
				gr.fill(new Rectangle2D.Float(cx+2, y-4, 14, 8));
				gr.setColor(mixc(c2, Color.WHITE, 0.30f));
				gr.fill(new Rectangle2D.Float(cx+2, y-4, 14, 2));
			}
		}
	}

	/** A portal gantry crane: two trucks, the portal beam, the trolley. */
	static void gantryCrane(Graphics2D gr, float x, float y, float w, float h)
	{
		// rails
		gr.setColor(new Color(90, 92, 96));
		gr.fill(new Rectangle2D.Float(x, y, 3, h));
		gr.fill(new Rectangle2D.Float(x+w-3, y, 3, h));
		// shadow of the beam
		gr.setColor(new Color(0, 0, 0, 50));
		gr.fill(new Rectangle2D.Float(x+4, y+h*0.45f+6, w-2, 7));
		// end trucks
		gr.setColor(new Color(150, 60, 48));
		gr.fill(new Rectangle2D.Float(x-2, y+h*0.45f-8, 7, 26));
		gr.fill(new Rectangle2D.Float(x+w-5, y+h*0.45f-8, 7, 26));
		// portal beam across
		gr.setColor(new Color(196, 84, 60));
		gr.fill(new Rectangle2D.Float(x, y+h*0.45f-3, w, 9));
		gr.setColor(new Color(232, 130, 92));
		gr.fill(new Rectangle2D.Float(x, y+h*0.45f-3, w, 3));
		gr.setColor(new Color(0, 0, 0, 40));
		for (float i = x+8; i < x+w-6; i += 9) {
			gr.fill(new Rectangle2D.Float(i, y+h*0.45f-2, 1, 7));
		}
		// trolley with a container on the hook
		gr.setColor(new Color(60, 62, 68));
		gr.fill(new Rectangle2D.Float(x+w*0.55f, y+h*0.45f-5, 12, 13));
		gr.setColor(new Color(62, 110, 180));
		gr.fill(new Rectangle2D.Float(x+w*0.55f-2, y+h*0.45f+10, 16, 9));
		gr.setColor(new Color(120, 160, 220));
		gr.fill(new Rectangle2D.Float(x+w*0.55f-2, y+h*0.45f+10, 16, 2));
	}

	/** The classic navy anchor, painted flat on a roof. */
	static void anchorIcon(Graphics2D gr, float cx, float cy, float r, Color c)
	{
		Stroke saved = gr.getStroke();
		gr.setColor(c);
		gr.setStroke(new BasicStroke(r*0.18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		// shank and crossbar
		gr.draw(new Line2D.Float(cx, cy-r*0.75f, cx, cy+r*0.55f));
		gr.draw(new Line2D.Float(cx-r*0.45f, cy-r*0.30f, cx+r*0.45f, cy-r*0.30f));
		// flukes
		gr.draw(new Arc2D.Float(cx-r*0.65f, cy-r*0.25f, r*1.3f, r*1.1f, 200, 140, Arc2D.OPEN));
		// the ring on top
		gr.setStroke(new BasicStroke(r*0.13f));
		gr.draw(new Ellipse2D.Float(cx-r*0.16f, cy-r*1.05f, r*0.32f, r*0.32f));
		gr.setStroke(saved);
	}

	//
	// airport
	//

	/**
	 * Airport, 6x6: the horizontal runway 9/27 crossing the vertical
	 * runway 18/36, taxiways to the terminal with its jetway gates and
	 * parked airliners, the control tower, the hangar, a car park, the
	 * fuel farm and the windsock. The radar pad on the north edge stays
	 * clear: the radar animation tiles land there.
	 */
	static BufferedImage renderAirport()
	{
		int seed = 5800;
		BufferedImage img = new BufferedImage(6*T, 6*T, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, LAWN, seed);
		paintBorder(img, seed);

		Graphics2D gr = gfx(img);

		// runways: horizontal 9/27 along the north, vertical 18/36 east
		runway(gr, 14, 76, 358, 44, true, "9", "27", seed);
		runway(gr, 286, 14, 44, 356, false, "18", "36", seed+1);

		// taxiways: apron to the horizontal runway and to the vertical
		taxiway(gr, 96, 120, 18, 78, false);
		taxiway(gr, 96, 182, 196, 18, true);
		gr.setColor(new Color(238, 206, 60, 200));
		gr.fill(new Rectangle2D.Float(104, 124, 2, 72));
		gr.fill(new Rectangle2D.Float(104, 189, 184, 2));

		// terminal with jetway gates on its north face
		pad(gr, 28, 200, 230, 100, seed);
		slab(gr, img, 36, 240, 150, 48, 2, new Color(196, 196, 202),
			new Color(140, 142, 150), seed, 3);
		for (int k = 0; k < 2; k++) {
			float gx = 64 + k*72;
			gr.setColor(new Color(120, 122, 130));
			gr.fill(new Rectangle2D.Float(gx, 222, 8, 20));
			airliner(gr, gx-18, 204, seed+k);
		}

		// control tower east of the terminal
		controlTower(gr, 216, 236);

		// hangar by the vertical runway
		hall(gr, img, 196, 308, 78, 40, 2, new Color(150, 154, 162),
			new Color(96, 100, 108), seed+2);
		gr.setColor(new Color(238, 206, 60, 180));
		gr.fill(new Rectangle2D.Float(232, 352, 2, 16));

		// car park southwest
		parking(gr, 32, 314, 96, 48, seed);

		// fuel farm tucked south of the hangar
		silo(gr, 144, 320, 22, 22, new Color(225, 225, 230), seed);
		silo(gr, 168, 328, 18, 18, new Color(225, 225, 230), seed+1);

		// windsock on the infield grass
		windsock(gr, 248, 130);

		gr.dispose();
		return img;
	}

	/**
	 * A runway strip with edge lines, displaced threshold stripes, the
	 * dashed centerline and painted numbers at both ends.
	 */
	static void runway(Graphics2D gr, float x, float y, float w, float h,
		boolean horizontal, String n0, String n1, int seed)
	{
		// asphalt
		gr.setColor(new Color(78, 78, 84));
		gr.fill(new Rectangle2D.Float(x, y, w, h));
		for (int k = 0; k < 220; k++) {
			gr.setColor(new Color(255, 255, 255, 8 + (int)(10*hash1(k, seed))));
			gr.fill(new Rectangle2D.Float(
				x + hash1(k, seed+1)*(w-3), y + hash1(k, seed+2)*(h-3), 3, 2));
		}
		float len = horizontal ? w : h;
		float wid = horizontal ? h : w;

		// edge lines
		gr.setColor(new Color(230, 230, 226, 220));
		if (horizontal) {
			gr.fill(new Rectangle2D.Float(x, y+2, w, 2));
			gr.fill(new Rectangle2D.Float(x, y+h-4, w, 2));
		}
		else {
			gr.fill(new Rectangle2D.Float(x+2, y, 2, h));
			gr.fill(new Rectangle2D.Float(x+w-4, y, 2, h));
		}
		// dashed centerline
		for (float t = 34; t < len-34; t += 16) {
			if (horizontal) {
				gr.fill(new Rectangle2D.Float(x+t, y+h/2-1, 9, 2));
			}
			else {
				gr.fill(new Rectangle2D.Float(x+w/2-1, y+t, 2, 9));
			}
		}
		// threshold zebra stripes
		for (int end = 0; end < 2; end++) {
			for (int k = 0; k < 4; k++) {
				float c = wid/2 + (k - 1.5f)*wid/5;
				if (horizontal) {
					float tx = end == 0 ? x+4 : x+w-12;
					gr.fill(new Rectangle2D.Float(tx, y + c - 2, 8, 4));
				}
				else {
					float ty = end == 0 ? y+4 : y+h-12;
					gr.fill(new Rectangle2D.Float(x + c - 2, ty, 4, 8));
				}
			}
		}
		// numbers
		paintNumber(gr, n0, horizontal ? x+24 : x+w/2, horizontal ? y+h/2 : y+24);
		paintNumber(gr, n1, horizontal ? x+w-24 : x+w/2, horizontal ? y+h/2 : y+h-24);
	}

	static void paintNumber(Graphics2D gr, String n, float cx, float cy)
	{
		gr.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
		FontMetrics fm = gr.getFontMetrics();
		gr.setColor(new Color(234, 234, 230, 230));
		gr.drawString(n, cx - fm.stringWidth(n)/2f, cy + fm.getAscent()/2f - 1);
	}

	/** A plain taxiway strip in lighter concrete. */
	static void taxiway(Graphics2D gr, float x, float y, float w, float h, boolean horizontal)
	{
		gr.setColor(new Color(118, 118, 124));
		if (horizontal) {
			gr.fill(new Rectangle2D.Float(x, y, w, h));
		}
		else {
			gr.fill(new Rectangle2D.Float(x, y, w, h == 0 ? 1 : h));
		}
	}

	/** A parked airliner from above, nose north toward its gate. */
	static void airliner(Graphics2D gr, float x, float y, int seed)
	{
		AffineTransform old = gr.getTransform();
		gr.translate(x, y);
		// shadow
		gr.setColor(new Color(0, 0, 0, 55));
		gr.fill(new Ellipse2D.Float(14, 26, 14, 32));
		// wings, swept back
		gr.setColor(new Color(206, 208, 214));
		Path2D.Float wings = new Path2D.Float();
		wings.moveTo(20, 30);
		wings.lineTo(0, 46);
		wings.lineTo(0, 52);
		wings.lineTo(20, 42);
		wings.lineTo(40, 52);
		wings.lineTo(40, 46);
		wings.closePath();
		gr.fill(wings);
		// tailplane
		Path2D.Float tail = new Path2D.Float();
		tail.moveTo(20, 52);
		tail.lineTo(10, 60);
		tail.lineTo(10, 63);
		tail.lineTo(20, 58);
		tail.lineTo(30, 63);
		tail.lineTo(30, 60);
		tail.closePath();
		gr.fill(tail);
		// fuselage
		gr.setColor(new Color(232, 234, 240));
		gr.fill(new RoundRectangle2D.Float(16.5f, 18, 7, 44, 7, 7));
		gr.setColor(new Color(160, 164, 174));
		gr.fill(new Ellipse2D.Float(17.5f, 18, 5, 7));
		// tail fin stripe
		gr.setColor(hash1(seed, 7) < 0.5f ? new Color(196, 50, 44) : new Color(58, 96, 196));
		gr.fill(new Rectangle2D.Float(18, 58, 4, 4));
		gr.setTransform(old);
	}

	/** The control tower: square shaft, octagonal glass cab, beacon. */
	static void controlTower(Graphics2D gr, float x, float y)
	{
		// shaft with its SE shadow
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new Rectangle2D.Float(x+5, y+7, 22, 30));
		gr.setColor(new Color(190, 190, 196));
		gr.fill(new Rectangle2D.Float(x, y+10, 20, 24));
		gr.setColor(new Color(140, 142, 150));
		gr.fill(new Rectangle2D.Float(x+13, y+10, 7, 24));
		// glass cab overhanging the shaft
		gr.setColor(new Color(60, 64, 74));
		gr.fill(new Ellipse2D.Float(x-4, y-2, 28, 18));
		GradientPaint gp = new GradientPaint(x-2, y, new Color(150, 196, 230),
			x+24, y+14, new Color(60, 100, 150));
		Paint saved = gr.getPaint();
		gr.setPaint(gp);
		gr.fill(new Ellipse2D.Float(x-2, y, 24, 14));
		gr.setPaint(saved);
		gr.setColor(new Color(255, 255, 255, 120));
		gr.fill(new Arc2D.Float(x, y+1, 20, 11, 70, 120, Arc2D.PIE));
		// beacon
		gr.setColor(new Color(220, 60, 50));
		gr.fill(new Ellipse2D.Float(x+8, y+3, 4, 4));
	}

	/** The orange windsock on its mast, blowing southeast. */
	static void windsock(Graphics2D gr, float x, float y)
	{
		gr.setColor(new Color(0, 0, 0, 60));
		gr.fill(new Rectangle2D.Float(x+1, y+1, 1.5f, 14));
		gr.setColor(new Color(206, 208, 214));
		gr.fill(new Rectangle2D.Float(x, y-4, 2, 18));
		Path2D.Float sock = new Path2D.Float();
		sock.moveTo(x+2, y-4);
		sock.lineTo(x+16, y);
		sock.lineTo(x+16, y+3);
		sock.lineTo(x+2, y+2);
		sock.closePath();
		gr.setColor(new Color(232, 120, 40));
		gr.fill(sock);
		gr.setColor(new Color(255, 255, 255, 150));
		gr.fill(new Rectangle2D.Float(x+6, y-3, 3, 6));
		gr.fill(new Rectangle2D.Float(x+12, y-2, 3, 5));
	}

	/**
	 * The radar frames for misc_animation: 8 tiles, the airport's radar
	 * pad with the dish turning 45 degrees per frame — a full sweep over
	 * the 8-frame loop. The ground is cropped from the airport block so
	 * the animated tile sits seamlessly in the redrawn airport.
	 */
	static BufferedImage renderRadarFrames()
	{
		BufferedImage airport = renderAirport();
		BufferedImage sheet = new BufferedImage(T, 8*T, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 8; f++) {
			gr.drawImage(airport.getSubimage(RADAR.x, RADAR.y, T, T), 0, f*T, null);
			float cx = T/2f, cy = f*T + T/2f + 4;
			// concrete footing and pedestal
			gr.setColor(new Color(172, 170, 164));
			gr.fill(new Ellipse2D.Float(cx-13, cy-10, 26, 22));
			gr.setColor(new Color(0, 0, 0, 60));
			gr.fill(new Ellipse2D.Float(cx-7, cy-2, 18, 11));
			gr.setColor(new Color(110, 112, 118));
			gr.fill(new Ellipse2D.Float(cx-9, cy-7, 18, 12));
			gr.setColor(new Color(70, 72, 78));
			gr.fill(new Ellipse2D.Float(cx-6, cy-5, 12, 8));
			// the dish, swept around by the frame counter
			double a = Math.toRadians(f*45);
			AffineTransform old = gr.getTransform();
			gr.rotate(a, cx, cy-2);
			gr.setColor(new Color(0, 0, 0, 60));
			gr.fill(new Ellipse2D.Float(cx-20, cy-6, 42, 14));
			gr.setColor(new Color(34, 36, 42));
			gr.fill(new Ellipse2D.Float(cx-22, cy-10.5f, 44, 15));
			gr.setColor(new Color(226, 228, 234));
			gr.fill(new Ellipse2D.Float(cx-21, cy-9.5f, 42, 13));
			gr.setColor(new Color(166, 168, 176));
			gr.fill(new Ellipse2D.Float(cx-17, cy-7.5f, 34, 9));
			gr.setColor(new Color(248, 250, 252));
			gr.fill(new Ellipse2D.Float(cx-21, cy-9.5f, 18, 6.5f));
			// feed horn out front
			gr.setColor(new Color(206, 52, 44));
			gr.fill(new Ellipse2D.Float(cx+16, cy-6, 6, 6));
			gr.setTransform(old);
		}
		gr.dispose();
		return sheet;
	}
}
