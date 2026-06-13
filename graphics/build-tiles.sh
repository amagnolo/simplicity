#!/bin/bash
# Regenerates the tile sheets in src/main/resources/ from the source art
# in graphics/. See docs/graphics-roadmap.adoc for the full pipeline notes.
#
# Steps:
#   1. de-dither the building/zone sheets into _16x16 source variants
#   2. derive _8x8/_32x32/_64x64 source sheets from the 16px art
#      (preferring the de-dithered _16x16 when one exists)
#   3. let GenerateTerrainArt / GenerateOverlayArt overwrite the terrain,
#      road, rail, wire and traffic sheets with procedurally drawn modern
#      art, and GenerateDeluxeArt produce the deluxe building sheets
#      (graphics/deluxe/gen/)
#   4. run MakeTiles for every tile size (3=minimap, 8, 16, 32, 64)
#   5. compose the classic skin from the unmodified base art
#   6. compose the deluxe skin from the modern sources overlaid with the
#      redrawn graphics/deluxe/ art (smaller sizes derived by halving)
#   7. keep src/main/resources/tiles.rc in sync with graphics/tiles.rc
#   8. derive _32x32/_64x64 sprite images (obj*.png)
set -euo pipefail
cd "$(dirname "$0")"

ROOT=..
CLASSES=$ROOT/target/classes
RESOURCES=$ROOT/src/main/resources

if [ ! -f "$CLASSES/micropolisj/build_tool/MakeTiles.class" ] ||
   [ ! -f "$CLASSES/micropolisj/build_tool/UpscaleArt.class" ] ||
   [ ! -f "$CLASSES/micropolisj/build_tool/HalveArt.class" ]; then
	echo "== compiling =="
	(cd $ROOT && mvn -q compile)
fi

upscale() {
	java -cp "$CLASSES" micropolisj.build_tool.UpscaleArt "$1" "$2" "$3"
}

# committed (hand-made) _NxN art is never overwritten; only gitignored
# derived files are regenerated
hand_made() {
	git ls-files --error-unmatch "$1" >/dev/null 2>&1
}

# building/zone sheets whose 80s dithering is blended away for the modern
# skin (terrain/roads/rails/wires/traffic are fully redrawn instead)
DEDITHER_SHEETS="airport coal coal_smoke_frames com_zones firestation
	ind01_pistons_frames ind03_smoke_frames ind04_smoke_frames
	ind07_smoke_frames ind08_smoke_frames ind_zones misc_animation
	nuclear police res_houses res_zones seaport stadium stadium2
	stadium_animation_gfx"

echo "== de-dithering building sheets =="
for base in $DEDITHER_SHEETS; do
	hand_made "${base}_16x16.png" ||
		java -cp "$CLASSES" micropolisj.build_tool.DeDither \
			"${base}.png" "${base}_16x16.png"
done

echo "== deriving upscaled source sheets =="
for f in *.png; do
	case "$f" in
	*_3x3.png|*_8x8.png|*_16x16.png|*_32x32.png|*_64x64.png|*_128x128.png)
		continue ;;
	esac
	base="${f%.png}"
	# upscale from the cleaned-up 16px art when the pipeline made one
	src="$f"
	[ -f "${base}_16x16.png" ] && src="${base}_16x16.png"
	hand_made "${base}_32x32.png" || upscale "$src" "${base}_32x32.png" 2
	hand_made "${base}_64x64.png" || upscale "$src" "${base}_64x64.png" 4
	if [ -f "${base}_16x16.png" ]; then
		hand_made "${base}_8x8.png" ||
			java -cp "$CLASSES" micropolisj.build_tool.HalveArt \
				"${base}_16x16.png" "${base}_8x8.png"
	fi
done

if [ -f "$CLASSES/micropolisj/build_tool/GenerateTerrainArt.class" ]; then
	echo "== generating procedural terrain art =="
	java -cp "$CLASSES" micropolisj.build_tool.GenerateTerrainArt .
fi

if [ -f "$CLASSES/micropolisj/build_tool/GenerateOverlayArt.class" ]; then
	echo "== generating procedural road/rail/wire art =="
	java -cp "$CLASSES" micropolisj.build_tool.GenerateOverlayArt .
fi

if [ -f "$CLASSES/micropolisj/build_tool/GenerateDeluxeArt.class" ]; then
	echo "== generating deluxe building art =="
	java -cp "$CLASSES" micropolisj.build_tool.GenerateDeluxeArt .
fi

echo "== composing tile sheets =="
java -Dtile_size=3  -cp "$CLASSES" micropolisj.build_tool.MakeTiles tiles.rc "$RESOURCES/sm"
for size in 8 16 32 64; do
	java -Dtile_size=$size -cp "$CLASSES" micropolisj.build_tool.MakeTiles tiles.rc "$RESOURCES/${size}x${size}"
done

# the classic skin (Options > Graphics > Classic) is the original art:
# compose it from the base 16px sources only, by staging them in a
# directory without any _NxN variants
echo "== composing classic tile sheets =="
CLASSES_ABS=$(cd "$CLASSES" && pwd)
RESOURCES_ABS=$(cd "$RESOURCES" && pwd)
STAGE=$(mktemp -d)
DSTAGE=$(mktemp -d)
trap 'rm -rf "$STAGE" "$DSTAGE"' EXIT
for f in *.png; do
	case "$f" in
	*_3x3.png|*_8x8.png|*_16x16.png|*_32x32.png|*_64x64.png|*_128x128.png)
		continue ;;
	esac
	cp "$f" "$STAGE/"
done
cp tiles.rc *.ani *.xml "$STAGE/"
for size in 8 16 32 64; do
	(cd "$STAGE" && java -Dtile_size=$size -cp "$CLASSES_ABS" micropolisj.build_tool.MakeTiles tiles.rc "$RESOURCES_ABS/classic/${size}x${size}")
done

# the deluxe skin (Options > Graphics > Deluxe) is the fully redrawn art:
# stage the modern sources (base art plus all _NxN variants), overlay the
# graphics/deluxe/ overrides, and compose from that — sheets without a
# deluxe redraw fall back to the modern art automatically
echo "== composing deluxe tile sheets =="
cp *.png tiles.rc *.ani *.xml "$DSTAGE/"
if compgen -G "deluxe/gen/*.png" >/dev/null; then
	cp deluxe/gen/*.png "$DSTAGE/"
fi
if compgen -G "deluxe/*.png" >/dev/null; then
	cp deluxe/*.png "$DSTAGE/"
	# a single 64px redraw covers every zoom level: derive the smaller
	# sizes by repeated halving unless the artist provided them
	for f in deluxe/*_64x64.png; do
		base=$(basename "${f%_64x64.png}")
		for sizes in "64 32" "32 16" "16 8"; do
			set -- $sizes
			[ -f "deluxe/${base}_$2x$2.png" ] ||
				java -cp "$CLASSES" micropolisj.build_tool.HalveArt \
					"$DSTAGE/${base}_$1x$1.png" "$DSTAGE/${base}_$2x$2.png"
		done
	done
fi
for size in 8 16 32 64; do
	(cd "$DSTAGE" && java -Dtile_size=$size -cp "$CLASSES_ABS" micropolisj.build_tool.MakeTiles tiles.rc "$RESOURCES_ABS/deluxe/${size}x${size}")
done

cp tiles.rc "$RESOURCES/tiles.rc"

echo "== deriving upscaled sprites =="
for f in "$RESOURCES"/obj*.png; do
	case "$f" in
	*_32x32.png|*_64x64.png)
		continue ;;
	esac
	base="${f%.png}"
	upscale "$f" "${base}_32x32.png" 2
	upscale "$f" "${base}_64x64.png" 4
done

# the classic skin keeps the pixel-art upscales of the original
# sprites (loaded from classic/ by TileImages); the shared variants
# are then replaced by the redrawn hi-res art. The 16px base
# obj*.png stays the original art everywhere.
if [ -f "$CLASSES/micropolisj/build_tool/GenerateSpriteArt.class" ]; then
	cp "$RESOURCES"/obj*_32x32.png "$RESOURCES"/obj*_64x64.png "$RESOURCES/classic/"
	echo "== generating sprite art =="
	java -cp "$CLASSES" micropolisj.build_tool.GenerateSpriteArt "$RESOURCES"
fi

# the toolbar tool icons follow the graphics skin: draw the modern and
# deluxe icon sets (the classic skin keeps the original root ic*.png art)
if [ -f "$CLASSES/micropolisj/build_tool/GenerateToolIcons.class" ]; then
	echo "== generating tool icons =="
	java -cp "$CLASSES" micropolisj.build_tool.GenerateToolIcons "$RESOURCES"
fi

echo "== done =="
