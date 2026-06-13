package micropolisj.build_tool;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import static micropolisj.build_tool.ProcArt.*;

/**
 * Generates the "deluxe" building art consumed by the deluxe skin. All
 * the building/zone sheets are drawn from scratch at 64px per tile by
 * the Deluxe*Art classes; only misc_animation still goes through the
 * legacy enhanced upscale, as the base sheet DeluxeMiscArt patches its
 * redrawn tiles into (the unused tiles keep the upscaled art).
 *
 * That upscaler is an edge-aware mean shift: each output pixel starts
 * from a bilinear estimate of its source neighborhood and is pulled
 * toward the locally dominant color mode with a joint spatial/range
 * kernel, after a pre-blend pass melts surviving dither checkerboards;
 * a finishing pass adds a saturation lift and a subtle material grain.
 *
 * Outputs name_{8x8,16x16,32x32,64x64}.png into graphics/deluxe/gen/;
 * build-tiles.sh overlays them (under any committed hand-made art in
 * graphics/deluxe/) when composing src/main/resources/deluxe/.
 *
 * The fountain frames inside misc_animation are pasted back from the
 * procedural misc_animation_64x64.png so the deluxe skin keeps the
 * full-color fountain drawn by GenerateTerrainArt.
 *
 * Usage: java micropolisj.build_tool.GenerateDeluxeArt &lt;graphics-dir&gt;
 */
public class GenerateDeluxeArt
{
	// sheets that still get the enhanced upscale: only misc_animation,
	// as the base its redrawn tiles are patched into (DeluxeMiscArt);
	// every building sheet is redrawn from scratch (DeluxeResArt,
	// DeluxeComArt, DeluxeIndArt, DeluxeCivicArt, DeluxeStadiumArt,
	// DeluxePowerArt, DeluxePortArt) and rendered separately
	static final String [] SHEETS = {
		"misc_animation",
	};

	static final int F = 4;             // upscale factor (16px -> 64px basis)
	static final float SIGMA_S = 0.55f; // spatial kernel width, in source px
	static final float SIGMA_R = 34f;   // range kernel width, in channel units
	static final int ITERATIONS = 3;    // mean-shift refinement steps

	// fountain animation frames in misc_animation.png (16px basis),
	// procedurally drawn by GenerateTerrainArt
	static final int FOUNTAIN_Y = 208;
	static final int FOUNTAIN_FRAMES = 4;

	final File graphicsDir;
	final File outDir;

	GenerateDeluxeArt(File graphicsDir)
	{
		this.graphicsDir = graphicsDir;
		this.outDir = new File(new File(graphicsDir, "deluxe"), "gen");
	}

	public static void main(String [] args)
		throws Exception
	{
		File dir = new File(args.length > 0 ? args[0] : ".");
		new GenerateDeluxeArt(dir).run();
	}

	void run()
		throws Exception
	{
		outDir.mkdirs();
		for (String base : SHEETS) {
			File src16 = new File(graphicsDir, base+"_16x16.png");
			File src = src16.exists() ? src16 : new File(graphicsDir, base+".png");
			BufferedImage img = UpscaleArt.toArgb(ImageIO.read(src));

			preBlend(img);
			BufferedImage out64 = upscale(img);
			enhance(out64);
			if (base.equals("misc_animation")) {
				restoreFountain(out64);
				DeluxeMiscArt.patch(out64);
				// the redrawn tiles graduate at the small sizes too,
				// pasted over the modern de-dithered sheet
				BufferedImage base16 = UpscaleArt.toArgb(ImageIO.read(src16));
				BufferedImage out16 = DeluxeMiscArt.patchSmall(base16, out64);
				write(out16, base+"_16x16.png");
				write(halve(out16), base+"_8x8.png");
			}
			write(out64, base+"_64x64.png");
			write(halve(out64), base+"_32x32.png");
		}

		// the building sheets are redrawn from scratch; all four sizes
		// come from the 64px render, like GenerateTerrainArt
		writeAllSizes(DeluxeResArt.renderResZones(), "res_zones");
		writeAllSizes(DeluxeResArt.renderResHouses(), "res_houses");
		writeAllSizes(DeluxeComArt.renderComZones(), "com_zones");
		writeAllSizes(DeluxeIndArt.renderIndZones(), "ind_zones");
		writeAllSizes(DeluxeIndArt.renderPistonFrames(), "ind01_pistons_frames");
		writeAllSizes(DeluxeIndArt.renderSmokeFrames(3), "ind03_smoke_frames");
		writeAllSizes(DeluxeIndArt.renderSmokeFrames(4), "ind04_smoke_frames");
		writeAllSizes(DeluxeIndArt.renderSmokeFrames(7), "ind07_smoke_frames");
		writeAllSizes(DeluxeIndArt.renderSmokeFrames(8), "ind08_smoke_frames");
		writeAllSizes(DeluxeCivicArt.renderFireStation(), "firestation");
		writeAllSizes(DeluxeCivicArt.renderPoliceStation(), "police");
		// the station light animations are new art with no classic
		// original: their 16px base sheets go to graphics/ so the
		// classic and modern skins can compose the frames too
		BufferedImage fireLights = DeluxeCivicArt.renderFireLightFrames();
		writeAllSizes(fireLights, "firestation_light_frames");
		writeBase16(fireLights, "firestation_light_frames.png");
		BufferedImage policeLights = DeluxeCivicArt.renderPoliceLightFrames();
		writeAllSizes(policeLights, "police_lights_frames");
		writeBase16(policeLights, "police_lights_frames.png");
		writeAllSizes(DeluxeStadiumArt.renderStadium(false), "stadium");
		writeAllSizes(DeluxeStadiumArt.renderStadium(true), "stadium2");
		writeAllSizes(DeluxeStadiumArt.renderGameFrames(), "stadium_animation_gfx");
		writeAllSizes(DeluxePowerArt.renderCoal(), "coal");
		writeAllSizes(DeluxePowerArt.renderCoalSmoke(), "coal_smoke_frames");
		writeAllSizes(DeluxePowerArt.renderNuclear(), "nuclear");
		writeAllSizes(DeluxePortArt.renderSeaport(), "seaport");
		writeAllSizes(DeluxePortArt.renderAirport(), "airport");

		patchModernDrawbridge();
	}

	/**
	 * The modern skin shares the procedural road bridges from
	 * GenerateOverlayArt, so its misc_animation sheets get the
	 * matching redrawn drawbridge tiles too — everything else in
	 * them keeps the de-dithered art. Must run after the sheets
	 * were derived and after restoreFountain read the modern 64px
	 * sheet.
	 */
	void patchModernDrawbridge()
		throws Exception
	{
		BufferedImage [] tiles = new BufferedImage[8];
		for (int k = 0; k < 4; k++) {
			tiles[k] = DeluxeMiscArt.renderBridgeTile(true, k);
			tiles[4+k] = DeluxeMiscArt.renderBridgeTile(false, k);
		}
		final int [] rows = { DeluxeMiscArt.ROW_HBRDG, DeluxeMiscArt.ROW_VBRDG };
		for (int size = 64; size >= 8; size /= 2) {
			File f = new File(graphicsDir, "misc_animation_"+size+"x"+size+".png");
			if (f.exists()) {
				BufferedImage sheet = UpscaleArt.toArgb(ImageIO.read(f));
				for (int r = 0; r < 2; r++) {
					for (int k = 0; k < 4; k++) {
						paste(sheet, tiles[r*4+k], 0, (rows[r] + k*16) * size / 16);
					}
				}
				System.out.println("Patching drawbridge: "+f);
				ImageIO.write(sheet, "png", f);
			}
			if (size > 8) {
				for (int i = 0; i < tiles.length; i++) {
					tiles[i] = halve(tiles[i]);
				}
			}
		}
	}

	void writeAllSizes(BufferedImage img64, String base)
		throws Exception
	{
		write(img64, base+"_64x64.png");
		BufferedImage img32 = halve(img64);
		write(img32, base+"_32x32.png");
		BufferedImage img16 = halve(img32);
		write(img16, base+"_16x16.png");
		write(halve(img16), base+"_8x8.png");
	}

	void write(BufferedImage img, String fileName)
		throws Exception
	{
		File f = new File(outDir, fileName);
		System.out.println("Generating art: "+f);
		ImageIO.write(img, "png", f);
	}

	/** Writes a 16px-basis base sheet (halved from 64px) to graphics/. */
	void writeBase16(BufferedImage img64, String fileName)
		throws Exception
	{
		File f = new File(graphicsDir, fileName);
		System.out.println("Generating art: "+f);
		ImageIO.write(halve(halve(img64)), "png", f);
	}

	/**
	 * Blends the dense two-color dithering that survived DeDither's
	 * conservative pass (narrow checkered bands such as awnings and roof
	 * trim) — a checkerboard can't be upscaled meaningfully, the blended
	 * shade is the full-color reading of it. Same alternation-candidate
	 * detection as DeDither, but blending wherever the 3x3 candidate
	 * density reaches 6: a true checkerboard scores 9 there while sparse
	 * window grids stay at 5 or below and remain crisp.
	 */
	static void preBlend(BufferedImage img)
	{
		final int w = img.getWidth();
		final int h = img.getHeight();
		int [] in = img.getRGB(0, 0, w, h, null, 0, w);

		boolean [] alt = new boolean[w*h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = in[y*w+x];
				if ((p >>> 24) < 128) continue;

				int differing = 0, agreeing = 0, b = 0;
				boolean consistent = true;
				for (int d = 0; d < 4; d++) {
					int nx = x + (d==0?1:d==1?-1:0);
					int ny = y + (d==2?1:d==3?-1:0);
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int q = in[ny*w+nx];
					if ((q >>> 24) < 128) continue;
					if (q == p) continue;
					differing++;
					if (agreeing == 0) {
						b = q;
						agreeing = 1;
					}
					else if (q != b) {
						consistent = false;
					}
				}
				alt[y*w+x] = differing >= 3 && consistent;
			}
		}

		int [] out = in.clone();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = in[y*w+x];
				if ((p >>> 24) < 128) continue;

				int density = 0;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						int nx = x+i, ny = y+j;
						if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
						if (alt[ny*w+nx]) density++;
					}
				}
				if (density < 6) continue;

				int r=0, g=0, b=0, cnt=0;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						int nx = x+i, ny = y+j;
						if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
						int q = in[ny*w+nx];
						if ((q >>> 24) < 128) continue;
						r += (q>>16)&0xff; g += (q>>8)&0xff; b += q&0xff;
						cnt++;
					}
				}
				out[y*w+x] = (p & 0xff000000) | ((r/cnt)<<16) | ((g/cnt)<<8) | (b/cnt);
			}
		}

		img.setRGB(0, 0, w, h, out, 0, w);
	}

	/**
	 * Edge-aware mean-shift 4x upscale. Works in premultiplied-alpha
	 * space so transparent sheet areas (smoke frames) don't bleed color.
	 */
	static BufferedImage upscale(BufferedImage src)
	{
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int W = w*F;
		final int H = h*F;
		int [] in = src.getRGB(0, 0, w, h, null, 0, w);

		// premultiplied a,r,g,b channels
		float [][] ch = new float[4][w*h];
		for (int i = 0; i < w*h; i++) {
			int p = in[i];
			float a = p >>> 24;
			ch[0][i] = a;
			ch[1][i] = ((p >> 16) & 0xff) * a / 255f;
			ch[2][i] = ((p >> 8) & 0xff) * a / 255f;
			ch[3][i] = (p & 0xff) * a / 255f;
		}

		int [] out = new int[W*H];
		final float invS = 1f / (2*SIGMA_S*SIGMA_S);
		final float invR = 1f / (2*SIGMA_R*SIGMA_R);
		float [] ref = new float[4];
		for (int y = 0; y < H; y++) {
			float sy = (y + 0.5f)/F - 0.5f;
			int cy = Math.round(sy);
			for (int x = 0; x < W; x++) {
				float sx = (x + 0.5f)/F - 0.5f;
				int cx = Math.round(sx);

				bilinearAt(ch, w, h, sx, sy, ref);
				for (int iter = 0; iter < ITERATIONS; iter++) {
					float ws = 0, va = 0, vr = 0, vg = 0, vb = 0;
					for (int j = -2; j <= 2; j++) {
						int yy = Math.max(0, Math.min(h-1, cy+j));
						float dy = sy - (cy+j);
						for (int i = -2; i <= 2; i++) {
							int xx = Math.max(0, Math.min(w-1, cx+i));
							float dx = sx - (cx+i);
							int idx = yy*w + xx;
							float da = ch[0][idx] - ref[0];
							float dr = ch[1][idx] - ref[1];
							float dg = ch[2][idx] - ref[2];
							float db = ch[3][idx] - ref[3];
							float wgt = (float)Math.exp(
								-(dx*dx + dy*dy)*invS
								- (da*da + dr*dr + dg*dg + db*db)*invR);
							ws += wgt;
							va += wgt*ch[0][idx];
							vr += wgt*ch[1][idx];
							vg += wgt*ch[2][idx];
							vb += wgt*ch[3][idx];
						}
					}
					ref[0] = va/ws;
					ref[1] = vr/ws;
					ref[2] = vg/ws;
					ref[3] = vb/ws;
				}

				float a = ref[0];
				if (a < 1f) {
					out[y*W+x] = 0;
				}
				else {
					int ia = Math.min(255, Math.round(a));
					int ir = Math.min(255, Math.round(ref[1]*255f/a));
					int ig = Math.min(255, Math.round(ref[2]*255f/a));
					int ib = Math.min(255, Math.round(ref[3]*255f/a));
					out[y*W+x] = (ia<<24) | (ir<<16) | (ig<<8) | ib;
				}
			}
		}

		BufferedImage dst = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		dst.setRGB(0, 0, W, H, out, 0, W);
		return dst;
	}

	/** Bilinear sample of the premultiplied channels, edge-clamped. */
	static void bilinearAt(float [][] ch, int w, int h, float sx, float sy, float [] dst)
	{
		int x0 = (int)Math.floor(sx);
		int y0 = (int)Math.floor(sy);
		float fx = sx - x0;
		float fy = sy - y0;
		int xa = Math.max(0, Math.min(w-1, x0));
		int xb = Math.max(0, Math.min(w-1, x0+1));
		int ya = Math.max(0, Math.min(h-1, y0));
		int yb = Math.max(0, Math.min(h-1, y0+1));
		for (int c = 0; c < 4; c++) {
			float v00 = ch[c][ya*w+xa];
			float v10 = ch[c][ya*w+xb];
			float v01 = ch[c][yb*w+xa];
			float v11 = ch[c][yb*w+xb];
			dst[c] = (v00*(1-fx)+v10*fx)*(1-fy) + (v01*(1-fx)+v11*fx)*fy;
		}
	}

	/**
	 * Finishing pass: a gentle saturation lift so the art reads as full
	 * color, and a subtle material grain (periodic per 64px tile so map
	 * neighbors join seamlessly).
	 */
	static void enhance(BufferedImage img)
	{
		final int w = img.getWidth();
		final int h = img.getHeight();
		for (int y = 0; y < h; y++) {
			float v = (y % N) / (float)N;
			for (int x = 0; x < w; x++) {
				int p = img.getRGB(x, y);
				int a = p >>> 24;
				if (a == 0) continue;

				float r = (p >> 16) & 0xff;
				float g = (p >> 8) & 0xff;
				float b = p & 0xff;

				float gray = 0.299f*r + 0.587f*g + 0.114f*b;
				r = gray + (r - gray)*1.12f;
				g = gray + (g - gray)*1.12f;
				b = gray + (b - gray)*1.12f;

				float u = (x % N) / (float)N;
				float grain = 1f + 0.06f*(noise(u, v, 16, 5) - 0.5f);
				r *= grain;
				g *= grain;
				b *= grain;

				int ir = Math.max(0, Math.min(255, Math.round(r)));
				int ig = Math.max(0, Math.min(255, Math.round(g)));
				int ib = Math.max(0, Math.min(255, Math.round(b)));
				img.setRGB(x, y, (a<<24) | (ir<<16) | (ig<<8) | ib);
			}
		}
	}

	/**
	 * The fountain frames inside misc_animation are procedural art from
	 * GenerateTerrainArt; copy them back over the upscaled sheet so the
	 * deluxe skin keeps them.
	 */
	void restoreFountain(BufferedImage out64)
		throws Exception
	{
		File f = new File(graphicsDir, "misc_animation_64x64.png");
		if (!f.exists()) return;
		BufferedImage modern = UpscaleArt.toArgb(ImageIO.read(f));
		int y0 = FOUNTAIN_Y * F;
		int hh = FOUNTAIN_FRAMES * SRC * F;
		paste(out64, modern.getSubimage(0, y0, modern.getWidth(), hh), 0, y0);
	}
}
