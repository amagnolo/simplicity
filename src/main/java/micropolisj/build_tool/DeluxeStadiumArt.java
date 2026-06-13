package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static micropolisj.build_tool.ProcArt.*;
import static micropolisj.build_tool.DeluxeResArt.*;
import static micropolisj.build_tool.DeluxeCivicArt.*;

/**
 * Draws the deluxe stadium art from scratch at 64px per tile: the empty
 * stadium (4x4), the full stadium with the crowd (stadium2, 4x4) and
 * the stadium_animation_gfx sheet — 8 frames of the football game, each
 * one tile wide and two tall.
 *
 * The game frames replace tiles (2,1) and (2,2) of the full stadium
 * (MapScanner composites FOOTBALLGAME1/2 at xpos+1), so the frame
 * background is cropped from the very same full-bowl render — minus the
 * players, which are then drawn per frame — keeping the animated column
 * pixel-identical to its static neighbors. Player motion is a smooth
 * cycle so the 8-frame loop is seamless. Sun from the northwest.
 */
final class DeluxeStadiumArt
{
	// bowl geometry, shared by the block renders and the game frames.
	// Like the classic art, the bowl sits on the east side of the 4x4
	// block with the parking lot on the west — that centers the field
	// under the animated column (tiles (2,1)-(2,2), x 128-192), so the
	// game frames can show players on both sides of midfield.
	static final int CX = 160, CY = 127;     // bowl center
	static final int ORX = 88, ORY = 110;    // outer wall radii
	static final Rectangle FIELD = new Rectangle(120, 58, 80, 138);

	// the animated column: tiles (2,1)-(2,2) of the 4x4 block
	static final Rectangle ANIM = new Rectangle(2*T, T, T, 2*T);

	static final Color TURF = new Color(52, 138, 58);
	static final Color [] CROWD = {
		new Color(214, 60, 50), new Color(238, 208, 70),
		new Color(230, 230, 234), new Color(70, 110, 210),
		new Color(70, 170, 90), new Color(226, 130, 54),
	};

	private DeluxeStadiumArt()
	{
	}

	/** Preview harness: renders the stadium sheets to the temp dir. */
	public static void main(String [] args)
		throws Exception
	{
		java.io.File dir = new java.io.File(System.getProperty("java.io.tmpdir"));
		javax.imageio.ImageIO.write(renderStadium(false), "png",
			new java.io.File(dir, "stadium_preview.png"));
		javax.imageio.ImageIO.write(renderStadium(true), "png",
			new java.io.File(dir, "stadium2_preview.png"));
		javax.imageio.ImageIO.write(renderGameFrames(), "png",
			new java.io.File(dir, "stadium_animation_preview.png"));
		System.out.println("Previews in "+dir);
	}

	/**
	 * The stadium / stadium2 sheet: one 4x4 block, 256x256. The field
	 * stays empty in the static art — the game plays out in the animated
	 * column, and motionless players next to it would give it away.
	 */
	static BufferedImage renderStadium(boolean full)
	{
		return renderBowl(full);
	}

	/** The bowl, stands and field, without any players. */
	static BufferedImage renderBowl(boolean full)
	{
		int seed = full ? 5400 : 5300;
		BufferedImage img = new BufferedImage(4*T, 4*T, BufferedImage.TYPE_INT_ARGB);
		paintGround(img, PATCHY, seed);
		paintRing(img, RING_GREEN, seed);

		Graphics2D gr = gfx(img);

		// parking lot on the west side, with a walk to the west gate;
		// it only fills up while the game is on
		parkingLot(gr, full, seed);
		gr.setColor(new Color(186, 184, 178));
		gr.fill(new Rectangle2D.Float(58, CY-9, 16, 18));
		bush(gr, 14, 28, seed+13);
		bush(gr, 56, 226, seed+14);
		tree(gr, 16, 236, 11, seed+15);

		Shape outer = new Ellipse2D.Float(CX-ORX, CY-ORY, 2*ORX, 2*ORY);
		Shape inner = new RoundRectangle2D.Float(FIELD.x-4, FIELD.y-4,
			FIELD.width+8, FIELD.height+8, 28, 28);
		Area stands = new Area(outer);
		stands.subtract(new Area(inner));

		// ground shadow southeast of the bowl
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new Ellipse2D.Float(CX-ORX+7, CY-ORY+9, 2*ORX, 2*ORY));

		// outer wall ring, lit from the northwest
		gr.setColor(new Color(96, 96, 102));
		gr.fill(outer);
		gr.setColor(new Color(146, 146, 152));
		gr.fill(new Arc2D.Float(CX-ORX+1, CY-ORY+1, 2*ORX-2, 2*ORY-2, 70, 130, Arc2D.PIE));
		gr.setColor(new Color(70, 70, 76));
		gr.fill(new Arc2D.Float(CX-ORX+1, CY-ORY+1, 2*ORX-2, 2*ORY-2, 250, 130, Arc2D.PIE));

		// stand terraces step down toward the field
		Shape clip = gr.getClip();
		gr.clip(stands);
		for (int k = 0; k < 5; k++) {
			float s = 0.96f - 0.05f*k;
			float rx = ORX*s, ry = ORY*s;
			gr.setColor(new Color(124+k*7, 124+k*7, 130+k*7));
			gr.fill(new Ellipse2D.Float(CX-rx, CY-ry, 2*rx, 2*ry));
		}
		// seat rows: concentric seams
		gr.setColor(new Color(0, 0, 0, 40));
		for (int k = 0; k < 10; k++) {
			float s = 0.94f - 0.052f*k;
			float rx = ORX*s, ry = ORY*s;
			gr.draw(new Ellipse2D.Float(CX-rx, CY-ry, 2*rx, 2*ry));
		}
		// radial aisles
		gr.setColor(new Color(208, 208, 212, 120));
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(2.5f));
		for (int k = 0; k < 12; k++) {
			double a = Math.PI*2*k/12 + Math.PI/12;
			gr.draw(new Line2D.Float(
				CX + (float)Math.cos(a)*ORX*0.45f, CY + (float)Math.sin(a)*ORY*0.45f,
				CX + (float)Math.cos(a)*ORX*0.97f, CY + (float)Math.sin(a)*ORY*0.97f));
		}
		gr.setStroke(saved);

		if (full) {
			// the crowd: speckles seated along the terrace rings
			for (int row = 0; row < 14; row++) {
				float s = 0.945f - 0.036f*row;
				float rx = ORX*s, ry = ORY*s;
				int n = Math.round(rx*0.9f);
				for (int k = 0; k < n; k++) {
					if (hash2(row, k, seed) > 0.78f) continue;
					double a = Math.PI*2*(k + 0.6f*hash2(k, row, seed+1))/n;
					float px = CX + (float)Math.cos(a)*rx;
					float py = CY + (float)Math.sin(a)*ry;
					gr.setColor(CROWD[(int)(hash2(k*3, row*7, seed+2)*CROWD.length)]);
					gr.fill(new Rectangle2D.Float(px-1.1f, py-1.1f, 2.2f, 2.2f));
				}
			}
		}
		else {
			// a few early fans in the empty bowl
			for (int k = 0; k < 30; k++) {
				double a = Math.PI*2*hash1(k, 81);
				float s = 0.62f + 0.3f*hash1(k, 82);
				gr.setColor(CROWD[k % CROWD.length]);
				gr.fill(new Rectangle2D.Float(
					CX + (float)Math.cos(a)*ORX*s - 1, CY + (float)Math.sin(a)*ORY*s - 1,
					2.2f, 2.2f));
			}
		}
		gr.setClip(clip);

		// inner wall edge around the field
		gr.setColor(new Color(56, 56, 62));
		gr.setStroke(new BasicStroke(3f));
		gr.draw(inner);
		gr.setStroke(saved);

		// gates: dark openings through the bowl at the four compass points
		gr.setColor(new Color(34, 34, 38));
		gr.fill(new RoundRectangle2D.Float(CX-11, CY-ORY-1, 22, 12, 6, 6));
		gr.fill(new RoundRectangle2D.Float(CX-11, CY+ORY-11, 22, 12, 6, 6));
		gr.fill(new RoundRectangle2D.Float(CX-ORX-1, CY-11, 12, 22, 6, 6));
		gr.fill(new RoundRectangle2D.Float(CX+ORX-11, CY-11, 12, 22, 6, 6));

		paintField(gr, seed);
		gr.dispose();
		return img;
	}

	/**
	 * The west parking lot: marked stalls that stand empty until the
	 * game is on, then fill with fans' cars (with the odd free stall).
	 */
	static void parkingLot(Graphics2D gr, boolean full, int seed)
	{
		gr.setColor(new Color(92, 92, 96));
		gr.fill(new Rectangle2D.Float(10, 36, 52, 186));
		gr.setColor(new Color(206, 206, 210, 190));
		for (int bay = 0; bay < 4; bay++) {
			float y = 42 + bay*45;
			for (int k = 0; k <= 4; k++) {
				gr.fill(new Rectangle2D.Float(12 + k*12, y, 1, 14));
			}
		}
		if (!full) return;
		for (int bay = 0; bay < 4; bay++) {
			int y = 42 + bay*45;
			for (int k = 0; k < 4; k++) {
				if (hash2(bay, k, seed) > 0.92f) continue;
				car(gr, 15 + k*12, y+1, 8, 12, carColor(seed + bay*9 + k));
			}
		}
	}

	/** The pitch: striped turf, touchlines, yard lines and end zones. */
	static void paintField(Graphics2D gr, int seed)
	{
		int x = FIELD.x, y = FIELD.y, w = FIELD.width, h = FIELD.height;
		// mowed turf stripes
		for (int j = 0; j < h; j += 14) {
			gr.setColor((j/14) % 2 == 0 ? TURF : mixc(TURF, Color.BLACK, 0.10f));
			gr.fill(new Rectangle2D.Float(x, y+j, w, Math.min(14, h-j)));
		}
		// end zones
		gr.setColor(new Color(36, 104, 44));
		gr.fill(new Rectangle2D.Float(x, y, w, 16));
		gr.fill(new Rectangle2D.Float(x, y+h-16, w, 16));
		// touchline
		gr.setColor(new Color(235, 240, 235, 230));
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(2f));
		gr.draw(new Rectangle2D.Float(x+3, y+3, w-6, h-6));
		gr.setStroke(saved);
		// yard lines, the midfield line strongest
		for (int j = 16; j <= h-16; j += 13) {
			boolean mid = Math.abs(y+j - (y+h/2)) < 7;
			gr.setColor(new Color(235, 240, 235, mid ? 220 : 120));
			gr.fill(new Rectangle2D.Float(x+4, y+j, w-8, mid ? 2 : 1));
		}
		// goals
		gr.setColor(new Color(238, 214, 90));
		gr.fill(new Rectangle2D.Float(x+w/2f-9, y+5, 18, 2));
		gr.fill(new Rectangle2D.Float(x+w/2f-9, y+h-7, 18, 2));
	}

	/**
	 * The stadium_animation_gfx sheet: 8 frames side by side, each one
	 * tile wide and two tall — the full bowl's east field column with
	 * the game in progress. Player motion is sinusoidal in the frame
	 * counter, so frame 8 meets frame 0 seamlessly.
	 */
	static BufferedImage renderGameFrames()
	{
		BufferedImage bowl = renderBowl(true);
		BufferedImage column = bowl.getSubimage(ANIM.x, ANIM.y, ANIM.width, ANIM.height);
		BufferedImage sheet = new BufferedImage(8*T, 2*T, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(sheet);
		for (int f = 0; f < 8; f++) {
			gr.drawImage(column, f*T, 0, null);
			Shape clip = gr.getClip();
			// keep the players on the pitch, inside this frame's cell
			gr.clip(new Rectangle(f*T, FIELD.y+3 - ANIM.y,
				Math.min(FIELD.x+FIELD.width-3 - ANIM.x, T), FIELD.height-6));

			float t = f / 8f;
			// the play drives down the field and resets, both teams
			// tracking the ball carrier; the pack straddles midfield
			float driveY = CY + 38f*(float)Math.sin(2*Math.PI*t);
			float sway = 6f*(float)Math.cos(2*Math.PI*t);
			for (int k = 0; k < 8; k++) {
				boolean red = k % 2 == 0;
				float ox = CX - 24 + hash1(k, 91)*44 + sway*(red ? 1 : -1);
				float oy = driveY + (red ? -7 : 7)
					+ 10f*(float)Math.sin(2*Math.PI*(t + hash1(k, 92)));
				player(gr, f*T + ox - ANIM.x, oy - ANIM.y, red);
			}
			// the ball at the scrimmage line
			gr.setColor(new Color(120, 74, 38));
			gr.fill(new Ellipse2D.Float(f*T + (CX+4) - ANIM.x + sway, driveY - ANIM.y, 4, 3));
			gr.setClip(clip);
		}
		gr.dispose();
		return sheet;
	}

	/** One player: team-colored jersey dot with a shadow and helmet glint. */
	static void player(Graphics2D gr, float x, float y, boolean red)
	{
		gr.setColor(new Color(0, 0, 0, 70));
		gr.fill(new Ellipse2D.Float(x-2, y-1, 5, 4));
		gr.setColor(red ? new Color(206, 44, 38) : new Color(238, 208, 70));
		gr.fill(new Ellipse2D.Float(x-2.5f, y-3, 5, 5));
		gr.setColor(new Color(255, 255, 255, 160));
		gr.fill(new Ellipse2D.Float(x-1.5f, y-2.5f, 2, 2));
	}
}
