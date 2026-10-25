"""Build TV JPEG copies and photo manifests without changing originals.
Run with optimize/run/photo-tools on PYTHONPATH for HEIC support.
"""
from pathlib import Path
import hashlib, json
from PIL import Image, ImageOps, ImageDraw
import pillow_heif
pillow_heif.register_heif_opener()
ROOT = Path(__file__).resolve().parents[2]
photos = ROOT / 'photos'
out = photos / 'tv'
out.mkdir(exist_ok=True)
old = json.loads((photos / 'index.json').read_text(encoding='utf-8-sig'))
credits = {p.get('source', p['f']): p for p in old}
entries, thumbs = [], []
for source in sorted(photos.iterdir(), key=lambda p:p.name.casefold()):
    if not source.is_file() or source.suffix.lower() not in ('.jpg','.jpeg','.png','.heic','.heif','.webp'): continue
    digest = hashlib.sha256(source.read_bytes()).hexdigest()[:16]
    name = 'tv_' + digest + '.jpg'
    with Image.open(source) as original:
        image = ImageOps.exif_transpose(original).convert('RGB')
        original_size = image.size
        image.thumbnail((1920,1080), Image.Resampling.LANCZOS)
        image.save(out / name, 'JPEG', quality=87, optimize=True)
        thumb = ImageOps.contain(image, (240,135))
        tile = Image.new('RGB',(260,175),'#10201a')
        tile.paste(thumb, ((260-thumb.width)//2,0))
        ImageDraw.Draw(tile).text((6,140),str(len(entries)+1)+'. '+source.name[:28],fill='white')
        thumbs.append(tile)
    prior = credits.get(source.name, {})
    entries.append({'f':'tv/'+name,'source':source.name,'c':prior.get('c',''), 'd':prior.get('d',0.95), 'width':original_size[0], 'height':original_size[1]})
# Remove only obsolete generated JPEG copies; never touch source photos.
keep = {Path(p['f']).name for p in entries}
for generated in out.glob('tv_*.jpg'):
    if generated.name not in keep: generated.unlink()
(photos/'index.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
columns=6; rows=(len(thumbs)+columns-1)//columns
sheet=Image.new('RGB',(columns*260,rows*175),'#07120d')
for n,tile in enumerate(thumbs): sheet.paste(tile,((n%columns)*260,(n//columns)*175))
sheet.save(ROOT/'optimize/run/shots/updated-photo-contact-sheet.jpg',quality=85)
print(json.dumps({'photos':len(entries),'original_bytes':sum(p.stat().st_size for p in photos.iterdir() if p.is_file() and p.suffix.lower() != '.json'),'tv_bytes':sum((photos/p['f']).stat().st_size for p in entries),'portrait':sum(p['height']>p['width'] for p in entries)},indent=2))
