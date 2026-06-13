package micropolisj.build_tool;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import static micropolisj.build_tool.ProcArt.*;

/**
 * Draws the mobile sprites (obj*.png) as real hi-res art for the 32px
 * and 64px tile sizes, replacing the Scale4x upscales of the original
 * 16px-basis frames. The 16px-basis obj*.png base art is untouched.
 *
 * Each vehicle is modeled once pointing north and rotated per heading
 * frame; the frame conventions follow the engine (see SpriteKind and
 * the Sprite subclasses): heading frames are 1=N, 2=NE ... 8=NW, the
 * airplane adds three takeoff frames heading east, the train and bus
 * use straight/diagonal track frames, and the monster walks 4 diagonal
 * directions in 3-step cycles plus 4 cardinal poses. Sun from the
 * northwest: drop shadows fall southeast, far for the fliers.
 *
 * Outputs objN-M_{32x32,64x64}.png into the given resources directory.
 *
 * Usage: java micropolisj.build_tool.GenerateSpriteArt &lt;resources-dir&gt;
 */
public class GenerateSpriteArt
{
	static final int SC = 4;   // px per 16px-basis source px

	final File outDir;

	GenerateSpriteArt(File outDir)
	{
		this.outDir = outDir;
	}

	public static void main(String [] args)
		throws Exception
	{
		File dir = new File(args.length > 0 ? args[0] : ".");
		new GenerateSpriteArt(dir).run();
	}

	/** Model painter: draws the sprite centered at the origin, heading north. */
	interface Painter
	{
		void paint(Graphics2D gr);
	}

	void run()
		throws Exception
	{
		// obj1: train — straight, diagonal and (empty) underwater frames
		writeFrames(1, new BufferedImage[] {
			ground(32, 0, GenerateSpriteArt::train),
			ground(32, 90, GenerateSpriteArt::train),
			ground(32, -45, GenerateSpriteArt::train),
			ground(32, 45, GenerateSpriteArt::train),
			new BufferedImage(32*SC, 32*SC, BufferedImage.TYPE_INT_ARGB),
		});

		// obj2: helicopter — 8 headings, flying high
		BufferedImage [] cop = new BufferedImage[8];
		for (int i = 0; i < 8; i++) {
			cop[i] = flying(32, i*45, GenerateSpriteArt::helicopter, 8, 10);
		}
		writeFrames(2, cop);

		// obj3: airplane — 8 headings plus 3 takeoff frames heading
		// east, played 11-10-9: the shadow closes in as it lifts off
		BufferedImage [] air = new BufferedImage[11];
		for (int i = 0; i < 8; i++) {
			air[i] = flying(48, i*45, GenerateSpriteArt::airplane, 11, 14);
		}
		air[8] = flying(48, 90, GenerateSpriteArt::airplane, 7, 9);
		air[9] = flying(48, 90, GenerateSpriteArt::airplane, 4, 5);
		air[10] = flying(48, 90, GenerateSpriteArt::airplane, 1, 2);
		writeFrames(3, air);

		// obj4: ship — 8 headings
		BufferedImage [] shi = new BufferedImage[8];
		for (int i = 0; i < 8; i++) {
			shi[i] = ground(48, i*45, GenerateSpriteArt::ship);
		}
		writeFrames(4, shi);

		// obj5: monster — NE/SE/SW/NW 3-step walks, then N/E/S/W poses
		BufferedImage [] god = new BufferedImage[16];
		final int [] diag = { 45, 135, 225, 315 };
		for (int d = 0; d < 4; d++) {
			for (int p = 0; p < 3; p++) {
				god[d*3+p] = monsterFrame(diag[d], p - 1);
			}
		}
		final int [] card = { 0, 90, 180, 270 };
		for (int d = 0; d < 4; d++) {
			god[12+d] = monsterFrame(card[d], 0);
		}
		writeFrames(5, god);

		// obj6: tornado — 3 swirl frames
		BufferedImage [] tor = new BufferedImage[3];
		for (int f = 0; f < 3; f++) {
			tor[f] = tornado(f);
		}
		writeFrames(6, tor);

		// obj7: explosion — 6-frame fireball-to-smoke sequence
		BufferedImage [] exp = new BufferedImage[6];
		for (int f = 0; f < 6; f++) {
			exp[f] = explosion(f);
		}
		writeFrames(7, exp);

		// obj8: bus — straight and diagonal frames, like the train
		writeFrames(8, new BufferedImage[] {
			ground(32, 0, GenerateSpriteArt::bus),
			ground(32, 90, GenerateSpriteArt::bus),
			ground(32, -45, GenerateSpriteArt::bus),
			ground(32, 45, GenerateSpriteArt::bus),
		});
	}

	void writeFrames(int objId, BufferedImage [] frames64)
		throws Exception
	{
		for (int i = 0; i < frames64.length; i++) {
			write(frames64[i], "obj"+objId+"-"+i+"_64x64.png");
			write(halve(frames64[i]), "obj"+objId+"-"+i+"_32x32.png");
		}
	}

	void write(BufferedImage img, String fileName)
		throws Exception
	{
		File f = new File(outDir, fileName);
		System.out.println("Generating art: "+f);
		ImageIO.write(img, "png", f);
	}

	//
	// frame assembly
	//

	static Graphics2D gfx(BufferedImage img)
	{
		Graphics2D gr = img.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return gr;
	}

	/** Renders the model rotated to the given heading (degrees cw from north). */
	static BufferedImage render(int base, double headingDeg, Painter p)
	{
		int s = base*SC;
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(img);
		gr.translate(s/2.0, s/2.0);
		gr.rotate(Math.toRadians(headingDeg));
		p.paint(gr);
		gr.dispose();
		return img;
	}

	/** A ground/water vehicle: tight drop shadow under the model. */
	static BufferedImage ground(int base, double headingDeg, Painter p)
	{
		return withShadow(render(base, headingDeg, p), 3, 4, 0.32f);
	}

	/** A flier: the shadow falls well southeast of the model. */
	static BufferedImage flying(int base, double headingDeg, Painter p, int dx, int dy)
	{
		return withShadow(render(base, headingDeg, p), dx, dy, 0.30f);
	}

	/**
	 * Composites the model over its own drop shadow: the silhouette's
	 * (blurred) alpha, painted black and offset to the southeast.
	 */
	static BufferedImage withShadow(BufferedImage model, int dx, int dy, float strength)
	{
		int w = model.getWidth(), h = model.getHeight();
		float [] a = new float[w*h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				a[y*w+x] = (model.getRGB(x, y) >>> 24) / 255f;
			}
		}
		// two soft blur passes over the silhouette
		for (int pass = 0; pass < 2; pass++) {
			float [] b = new float[w*h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					float sum = 0;
					int cnt = 0;
					for (int j = -2; j <= 2; j += 2) {
						for (int i = -2; i <= 2; i += 2) {
							int xx = x+i, yy = y+j;
							if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue;
							sum += a[yy*w+xx];
							cnt++;
						}
					}
					b[y*w+x] = sum/cnt;
				}
			}
			a = b;
		}

		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int sx = x - dx, sy = y - dy;
				if (sx < 0 || sy < 0 || sx >= w || sy >= h) continue;
				int al = Math.round(255*strength*a[sy*w+sx]);
				if (al > 0) {
					out.setRGB(x, y, (al<<24));
				}
			}
		}
		Graphics2D gr = out.createGraphics();
		gr.drawImage(model, 0, 0, null);
		gr.dispose();
		return out;
	}

	static Color mixc(Color a, Color b, float t)
	{
		return new Color(
			Math.round(a.getRed() + (b.getRed()-a.getRed())*t),
			Math.round(a.getGreen() + (b.getGreen()-a.getGreen())*t),
			Math.round(a.getBlue() + (b.getBlue()-a.getBlue())*t));
	}

	//
	// vehicles (all drawn centered at the origin, heading north)
	//

	/** The commuter train: a double-cab loco, dark body with gray roof. */
	static void train(Graphics2D gr)
	{
		Color body = new Color(64, 68, 78);
		Color roof = new Color(134, 136, 142);

		gr.setColor(body);
		gr.fill(new RoundRectangle2D.Float(-13, -48, 26, 96, 10, 10));
		// cab windshields at both ends
		gr.setColor(new Color(34, 42, 58));
		gr.fill(new RoundRectangle2D.Float(-10, -46, 20, 7, 5, 5));
		gr.fill(new RoundRectangle2D.Float(-10, 39, 20, 7, 5, 5));
		// roof panel with a lit center ridge and vents
		gr.setColor(roof);
		gr.fill(new RoundRectangle2D.Float(-10, -36, 20, 72, 6, 6));
		gr.setColor(mixc(roof, Color.WHITE, 0.30f));
		gr.fill(new Rectangle2D.Float(-2, -34, 4, 68));
		gr.setColor(new Color(92, 94, 100));
		for (int k = 0; k < 4; k++) {
			gr.fill(new Rectangle2D.Float(-6, -28 + k*17, 12, 5));
		}
		// car coupling seam at the middle
		gr.setColor(new Color(30, 32, 38));
		gr.fill(new Rectangle2D.Float(-13, -1.5f, 26, 3));
		// warm yellow side trim
		gr.setColor(new Color(222, 178, 60));
		gr.fill(new Rectangle2D.Float(-13, -44, 2, 88));
		gr.fill(new Rectangle2D.Float(11, -44, 2, 88));
		// marker lights at both noses
		gr.setColor(new Color(238, 70, 56));
		gr.fill(new Ellipse2D.Float(-8, -49, 4, 4));
		gr.fill(new Ellipse2D.Float(4, -49, 4, 4));
		gr.fill(new Ellipse2D.Float(-8, 45, 4, 4));
		gr.fill(new Ellipse2D.Float(4, 45, 4, 4));
	}

	/** The city bus: red coachwork, silver roof, rooftop AC pod. */
	static void bus(Graphics2D gr)
	{
		Color coach = new Color(178, 42, 38);
		Color roof = new Color(212, 214, 218);

		gr.setColor(coach);
		gr.fill(new RoundRectangle2D.Float(-13, -38, 26, 76, 9, 9));
		// windshield up front
		gr.setColor(new Color(36, 44, 60));
		gr.fill(new RoundRectangle2D.Float(-11, -36, 22, 7, 6, 6));
		// roof panel
		gr.setColor(roof);
		gr.fill(new RoundRectangle2D.Float(-10, -27, 20, 60, 7, 7));
		gr.setColor(mixc(roof, Color.WHITE, 0.35f));
		gr.fill(new Rectangle2D.Float(-10, -27, 4, 60));
		// skylight strip and the AC pod
		gr.setColor(new Color(150, 158, 172));
		gr.fill(new RoundRectangle2D.Float(-3, -22, 6, 38, 4, 4));
		gr.setColor(new Color(168, 170, 176));
		gr.fill(new RoundRectangle2D.Float(-7, 18, 14, 11, 4, 4));
		gr.setColor(new Color(120, 122, 128));
		gr.draw(new RoundRectangle2D.Float(-7, 18, 14, 11, 4, 4));
		// roof number so it reads as transit
		gr.setColor(new Color(64, 66, 72));
		gr.fill(new Rectangle2D.Float(-1.5f, -17, 3, 12));
	}

	/** The helicopter: white fuselage, glass nose, spinning rotor. */
	static void helicopter(Graphics2D gr)
	{
		// skids
		gr.setColor(new Color(70, 72, 78));
		gr.fill(new RoundRectangle2D.Float(-13, -18, 3.5f, 38, 3, 3));
		gr.fill(new RoundRectangle2D.Float(9.5f, -18, 3.5f, 38, 3, 3));
		// tail boom, fin and tail rotor
		gr.setColor(new Color(190, 192, 198));
		gr.fill(new RoundRectangle2D.Float(-2.5f, 12, 5, 40, 4, 4));
		gr.setColor(new Color(150, 152, 158));
		gr.fill(new RoundRectangle2D.Float(-7, 46, 14, 4, 3, 3));
		gr.setColor(new Color(90, 92, 98, 150));
		gr.fill(new Ellipse2D.Float(6, 42, 12, 12));
		gr.setColor(new Color(238, 70, 56));
		gr.fill(new Ellipse2D.Float(-2, 50, 4, 4));
		// fuselage with the red mission stripe
		gr.setColor(new Color(228, 230, 234));
		gr.fill(new Ellipse2D.Float(-11, -24, 22, 42));
		gr.setColor(new Color(196, 50, 42));
		gr.fill(new Rectangle2D.Float(-11, -5, 22, 7));
		// glass nose
		gr.setColor(new Color(46, 60, 84));
		gr.fill(new Arc2D.Float(-9, -23, 18, 24, 0, 180, Arc2D.CHORD));
		gr.setColor(new Color(150, 180, 215));
		gr.fill(new Arc2D.Float(-6, -21, 9, 12, 40, 110, Arc2D.CHORD));
		// engine housing
		gr.setColor(new Color(120, 122, 130));
		gr.fill(new RoundRectangle2D.Float(-6, 0, 12, 12, 5, 5));
		// main rotor: motion-blur disc, three blades, hub
		gr.setColor(new Color(40, 40, 46, 36));
		gr.fill(new Ellipse2D.Float(-30, -32, 60, 60));
		gr.setColor(new Color(52, 54, 60, 210));
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int k = 0; k < 3; k++) {
			double a = Math.toRadians(25 + k*120);
			gr.draw(new Line2D.Float(0, -2,
				(float)(Math.cos(a)*29), (float)(-2 + Math.sin(a)*29)));
		}
		gr.setStroke(saved);
		gr.setColor(new Color(36, 38, 44));
		gr.fill(new Ellipse2D.Float(-3.5f, -5.5f, 7, 7));
	}

	/** The airliner: white fuselage, swept wings, red trim. */
	static void airplane(Graphics2D gr)
	{
		Color white = new Color(234, 236, 240);
		Color wing = new Color(214, 216, 224);
		Color red = new Color(198, 46, 40);

		// tailplane
		gr.setColor(wing);
		gr.fill(new Polygon(
			new int [] { -3, -28, -24, -3 },
			new int [] { 48, 66, 70, 62 }, 4));
		gr.fill(new Polygon(
			new int [] { 3, 28, 24, 3 },
			new int [] { 48, 66, 70, 62 }, 4));
		// engine pods first: they hang under the wings, only their
		// nacelle fronts poke out ahead of the leading edge
		for (int side = -1; side <= 1; side += 2) {
			gr.setColor(new Color(140, 144, 152));
			gr.fill(new RoundRectangle2D.Float(side*30 - 4.5f, -8, 9, 18, 6, 6));
			gr.setColor(new Color(52, 56, 64));
			gr.fill(new Ellipse2D.Float(side*30 - 4.5f, -9, 9, 6));
		}
		// main wings, swept back, drawn over the engines
		for (int side = -1; side <= 1; side += 2) {
			gr.setColor(wing);
			gr.fill(new Polygon(
				new int [] { side*8, side*65, side*65, side*8 },
				new int [] { -12, 24, 32, 18 }, 4));
			gr.setColor(mixc(wing, Color.BLACK, 0.18f));
			gr.fill(new Polygon(
				new int [] { side*8, side*65, side*65, side*8 },
				new int [] { 14, 29, 32, 18 }, 4));
			// red wingtip
			gr.setColor(red);
			gr.fill(new Polygon(
				new int [] { side*65, side*65, side*56 },
				new int [] { 24, 32, 29 }, 3));
		}
		// fuselage
		gr.setColor(white);
		gr.fill(new RoundRectangle2D.Float(-9, -74, 18, 144, 17, 17));
		gr.setColor(mixc(white, Color.WHITE, 0.5f));
		gr.fill(new RoundRectangle2D.Float(-7, -70, 6, 134, 6, 6));
		// cockpit
		gr.setColor(new Color(40, 52, 74));
		gr.fill(new Arc2D.Float(-7, -71, 14, 16, 0, 180, Arc2D.CHORD));
		// cabin window line
		gr.setColor(new Color(120, 136, 160));
		gr.fill(new Rectangle2D.Float(5.5f, -52, 2, 96));
		gr.fill(new Rectangle2D.Float(-7.5f, -52, 2, 96));
		// vertical stabilizer from above: a red-tipped spine at the tail
		gr.setColor(new Color(170, 174, 184));
		gr.fill(new RoundRectangle2D.Float(-2, 46, 4, 26, 3, 3));
		gr.setColor(red);
		gr.fill(new RoundRectangle2D.Float(-2, 64, 4, 8, 3, 3));
	}

	/** The freighter: dark hull, container deck, white sterncastle. */
	static void ship(Graphics2D gr)
	{
		Color hull = new Color(40, 48, 66);

		// bow wake and stern foam on the water
		gr.setColor(new Color(235, 242, 248, 90));
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(-4, -70, -26, -28));
		gr.draw(new Line2D.Float(4, -70, 26, -28));
		gr.setStroke(saved);
		gr.setColor(new Color(235, 242, 248, 70));
		gr.fill(new Ellipse2D.Float(-13, 62, 26, 12));

		// hull, pointed bow and rounded stern
		Path2D.Float p = new Path2D.Float();
		p.moveTo(0, -74);
		p.curveTo(13, -62, 22, -42, 22, -22);
		p.lineTo(22, 50);
		p.quadTo(22, 64, 0, 64);
		p.quadTo(-22, 64, -22, 50);
		p.lineTo(-22, -22);
		p.curveTo(-22, -42, -13, -62, 0, -74);
		p.closePath();
		gr.setColor(hull);
		gr.fill(p);
		gr.setColor(mixc(hull, Color.WHITE, 0.25f));
		gr.setStroke(new BasicStroke(2f));
		gr.draw(p);
		gr.setStroke(saved);

		// deck
		gr.setColor(new Color(116, 122, 132));
		gr.fill(new RoundRectangle2D.Float(-17, -36, 34, 92, 14, 14));
		// fo'c'sle with anchor gear
		gr.setColor(new Color(150, 156, 166));
		gr.fill(new Polygon(
			new int [] { 0, 13, -13 },
			new int [] { -64, -38, -38 }, 3));
		gr.setColor(new Color(80, 86, 96));
		gr.fill(new Ellipse2D.Float(-3, -52, 6, 6));
		// container bays
		Color [] boxes = {
			new Color(192, 92, 40), new Color(64, 110, 170),
			new Color(108, 64, 56), new Color(76, 132, 78),
			new Color(170, 150, 60), new Color(140, 70, 64),
		};
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 2; col++) {
				gr.setColor(boxes[(row*2+col) % boxes.length]);
				gr.fill(new Rectangle2D.Float(-14 + col*15, -30 + row*13, 13, 11));
				gr.setColor(new Color(0, 0, 0, 50));
				gr.fill(new Rectangle2D.Float(-14 + col*15, -30 + row*13 + 9, 13, 2));
			}
		}
		// sterncastle: white bridge block with wings and the funnel
		gr.setColor(new Color(226, 228, 232));
		gr.fill(new Rectangle2D.Float(-19, 26, 38, 9));
		gr.fill(new RoundRectangle2D.Float(-14, 26, 28, 24, 6, 6));
		gr.setColor(new Color(60, 74, 96));
		gr.fill(new Rectangle2D.Float(-12, 28, 24, 4));
		gr.setColor(new Color(196, 50, 42));
		gr.fill(new Ellipse2D.Float(-6, 40, 12, 9));
		gr.setColor(new Color(40, 42, 48));
		gr.fill(new Ellipse2D.Float(-3.5f, 42, 7, 5));
	}

	//
	// monster
	//

	static BufferedImage monsterFrame(double headingDeg, int stride)
	{
		return withShadow(render(48, headingDeg, gr -> monster(gr, stride)), 4, 5, 0.34f);
	}

	/**
	 * The monster: a green kaiju seen from above, tail swinging
	 * against the stride; {@code stride} -1/0/+1 swings the limbs.
	 */
	static void monster(Graphics2D gr, int stride)
	{
		Color skin = new Color(74, 128, 56);
		Color skinDark = new Color(52, 96, 42);
		Color belly = new Color(150, 168, 96);
		Color claw = new Color(204, 52, 40);

		// tail, curling opposite the stride
		Stroke saved = gr.getStroke();
		gr.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.setColor(skinDark);
		Path2D.Float tail = new Path2D.Float();
		tail.moveTo(0, 28);
		tail.quadTo(-stride*16, 52, -stride*22, 68);
		gr.draw(tail);
		gr.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		gr.draw(new Line2D.Float(-stride*22, 68, -stride*26, 76));
		gr.setStroke(saved);

		// legs with red claws, alternating with the stride
		for (int side = -1; side <= 1; side += 2) {
			float fwd = side*stride*9;
			float lx = side*17, ly = 26 - fwd;
			gr.setColor(skinDark);
			gr.fill(new Ellipse2D.Float(lx-8, ly-7, 16, 20));
			for (int c = 0; c < 3; c++) {
				gr.setColor(claw);
				gr.fill(new Ellipse2D.Float(lx-6 + c*4.5f, ly+10, 3.5f, 6));
			}
		}
		// arms, swinging against the legs
		for (int side = -1; side <= 1; side += 2) {
			float fwd = -side*stride*7;
			float ax = side*22, ay = -10 - fwd;
			gr.setColor(skin);
			gr.fill(new Ellipse2D.Float(ax-7, ay-6, 14, 17));
			for (int c = 0; c < 3; c++) {
				gr.setColor(claw);
				gr.fill(new Ellipse2D.Float(ax-5 + c*4f, ay+9, 3, 5));
			}
		}

		// body with the pale belly stripe and dorsal plates
		gr.setColor(skin);
		gr.fill(new Ellipse2D.Float(-21, -24, 42, 58));
		gr.setColor(belly);
		gr.fill(new Ellipse2D.Float(-8, -14, 16, 40));
		gr.setColor(new Color(0, 0, 0, 40));
		for (int k = 0; k < 4; k++) {
			gr.fill(new Ellipse2D.Float(-12 + (k%2)*16, -16 + k*11, 8, 6));
		}
		gr.setColor(skinDark);
		for (int k = 0; k < 4; k++) {
			int y = -18 + k*12;
			gr.fill(new Polygon(
				new int [] { -4, 4, 0 },
				new int [] { y+6, y+6, y-3 }, 3));
		}

		// head with yellow eyes and toothy snout
		gr.setColor(skin);
		gr.fill(new Ellipse2D.Float(-14, -52, 28, 32));
		gr.setColor(skinDark);
		gr.fill(new Ellipse2D.Float(-8, -62, 16, 18));
		gr.setColor(new Color(232, 206, 60));
		gr.fill(new Ellipse2D.Float(-10, -46, 6, 7));
		gr.fill(new Ellipse2D.Float(4, -46, 6, 7));
		gr.setColor(new Color(40, 40, 30));
		gr.fill(new Ellipse2D.Float(-8.5f, -44.5f, 3, 4));
		gr.fill(new Ellipse2D.Float(5.5f, -44.5f, 3, 4));
		gr.setColor(new Color(238, 238, 230));
		for (int c = 0; c < 3; c++) {
			gr.fill(new Polygon(
				new int [] { -6 + c*5, -2 + c*5, -4 + c*5 },
				new int [] { -60, -60, -56 }, 3));
		}
	}

	//
	// tornado
	//

	/**
	 * The tornado: a leaning funnel of stacked swirl bands under a
	 * ragged cloud cap, debris circling the base; the three frames
	 * shift the swirl phase so the loop spins.
	 */
	static BufferedImage tornado(int f)
	{
		int s = 48*SC;
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(img);
		gr.translate(s/2.0, s/2.0);
		float phase = f * (float)(2*Math.PI/3);

		// dust kicked up at the ground
		gr.setColor(new Color(150, 122, 92, 110));
		gr.fill(new Ellipse2D.Float(2 - 26, 66, 52, 18));
		gr.setColor(new Color(170, 144, 110, 90));
		gr.fill(new Ellipse2D.Float(2 - 16 + 8*(float)Math.sin(phase), 62, 36, 14));

		// the funnel, drawn bottom-up so upper bands overlap lower ones
		final int bands = 14;
		for (int k = bands-1; k >= 0; k--) {
			float t = k/(float)(bands-1);              // 0 top .. 1 tip
			float rx = 6 + 58*(float)Math.pow(1-t, 1.35f);
			float ry = rx*0.34f;
			float cy = -56 + 128*t;
			float cx = -4 + 10*t + (6 - 4*t)*(float)Math.sin(phase + t*7f);
			float lum = 0.86f + 0.14f*(float)Math.sin(phase*2 + t*11f);
			int g = Math.round((205 - 70*t) * lum);
			gr.setColor(new Color(g, g, Math.min(255, g+6), 235));
			gr.fill(new Ellipse2D.Float(cx-rx, cy-ry, 2*rx, 2*ry));
			// swirl shading hugging one side of the band
			gr.setColor(new Color(Math.round(g*0.72f), Math.round(g*0.72f),
				Math.round(g*0.76f), 180));
			gr.fill(new Arc2D.Float(cx-rx, cy-ry, 2*rx, 2*ry,
				200 + (float)Math.toDegrees(phase) + t*160, 120, Arc2D.CHORD));
		}

		// ragged cloud cap
		for (int k = 0; k < 6; k++) {
			float a = phase + k*(float)(Math.PI/3);
			float cx = -4 + 30*(float)Math.cos(a);
			float cy = -62 + 9*(float)Math.sin(a);
			int g = 188 + (k%2)*18;
			gr.setColor(new Color(g, g, g+4, 235));
			gr.fill(new Ellipse2D.Float(cx-22, cy-12, 44, 24));
		}

		// debris circling the funnel base
		for (int k = 0; k < 9; k++) {
			float a = phase*1.5f + k*(float)(2*Math.PI/9);
			float t = 0.55f + 0.42f*hash1(k, 7);
			float cy = -56 + 128*t;
			float rx = (6 + 58*(float)Math.pow(1-t, 1.35f)) + 7;
			float cx = -4 + 10*t + rx*(float)Math.cos(a);
			gr.setColor(hash1(k, 8) < 0.5f
				? new Color(80, 66, 52) : new Color(120, 112, 100));
			gr.fill(new Ellipse2D.Float(cx-2, cy + 4*(float)Math.sin(a) - 2, 4, 4));
		}
		gr.dispose();
		return img;
	}

	//
	// explosion
	//

	/**
	 * One frame of the six-frame explosion: flash, swelling fireball
	 * with flying debris, then collapsing into smoke that thins out.
	 */
	static BufferedImage explosion(int f)
	{
		int s = 48*SC;
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = gfx(img);
		gr.translate(s/2.0, s/2.0);

		if (f < 3) {
			// flash growing into the full fireball
			float r = 22 + f*22;
			gr.setColor(new Color(150, 32, 16, 200));
			gr.fill(DeluxeMiscArt.blob(0, 0, r+6, f*47));
			gr.setColor(new Color(232, 96, 26));
			gr.fill(DeluxeMiscArt.blob(0, 0, r, f*47+1));
			gr.setColor(new Color(252, 178, 50));
			gr.fill(DeluxeMiscArt.blob(-r*0.10f, -r*0.12f, r*0.64f, f*47+2));
			gr.setColor(new Color(255, 244, 200));
			gr.fill(DeluxeMiscArt.blob(-r*0.16f, -r*0.18f, r*0.34f, f*47+3));
			// debris flung outward
			for (int k = 0; k < 5 + f*5; k++) {
				double a = Math.PI*2*hash2(k, f, 5);
				float d = (r+6) * (0.8f + 0.7f*hash2(k, f, 6));
				gr.setColor(hash2(k, f, 7) < 0.5f
					? new Color(66, 54, 46) : new Color(248, 156, 62));
				gr.fill(new Ellipse2D.Float(
					(float)Math.cos(a)*d - 2.5f,
					(float)Math.sin(a)*d*0.92f - 2.5f, 5, 5));
			}
		}
		else {
			// fire dying under churning smoke, thinning away
			float age = (f-3) / 2f;            // 0 .. 1
			float r = 64 + 14*age;
			int al = Math.round(215*(1f - age*0.80f));
			gr.setColor(new Color(46, 44, 44, al));
			gr.fill(DeluxeMiscArt.blob(0, -8 - 14*age, r, f*47));
			gr.setColor(new Color(88, 84, 82, Math.round(al*0.9f)));
			gr.fill(DeluxeMiscArt.blob(-14, -22 - 18*age, r*0.62f, f*47+1));
			gr.setColor(new Color(134, 128, 124, Math.round(al*0.7f)));
			gr.fill(DeluxeMiscArt.blob(16, -28 - 22*age, r*0.46f, f*47+2));
			if (f == 3) {
				// embers still glowing under the smoke
				gr.setColor(new Color(244, 128, 42, 220));
				gr.fill(DeluxeMiscArt.blob(-10, 22, 16, 9));
				gr.fill(DeluxeMiscArt.blob(20, 12, 11, 10));
			}
		}
		gr.dispose();
		return img;
	}
}
