#!/usr/bin/env python3
"""Extract ONLY isolated artwork, with user authorization; no screen/card/control is a runtime image.

Coordinates describe the full approved display normalized to 412 x 746, not the app crop.
Source reference bytes are unchanged. No generative reconstruction, recoloring, or upscaling.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import hashlib, json, shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/synapse/v3/artwork'
RUNTIME=ROOT/'app/src/main/res/drawable-nodpi'
CROPS={
 'welcome_phones': (1,(216,80,404,252)),
 'device_galaxy_a04s': (14,(35,227,91,349)),
 'video_1': (8,(30,350,78,381)),
 'video_2': (8,(30,398,78,429)),
 'video_3': (8,(30,446,78,477)),
 'provider_google': (22,(27,224,54,250)),
 'provider_openai': (22,(26,283,56,312)),
 'provider_anthropic': (22,(25,341,57,374)),
 'app_whatsapp': (16,(25,294,49,318)),
 'app_youtube': (16,(26,328,49,351)),
 'app_chrome': (16,(26,361,49,384)),
 'app_termux': (16,(25,394,49,419)),
 'app_drive': (16,(25,463,49,487)),
 'app_gallery': (16,(25,499,49,523)),
 'app_camera': (16,(25,535,49,559)),
 'app_settings': (16,(25,571,49,595)),
 'package_python': (19,(22,276,42,297)),
 'package_nodejs': (19,(22,307,42,329)),
 'package_git': (19,(22,337,42,358)),
 'package_curl': (19,(22,368,42,389)),
 'package_ffmpeg': (19,(22,398,42,419)),
 'package_neofetch': (19,(22,427,42,449)),
 'package_htop': (19,(22,457,42,479)),
 'package_vim': (19,(22,487,42,508)),
 'package_wget': (19,(22,517,42,539)),
 'package_build': (19,(22,547,42,570)),
 'execution_capture_1': (6,(244,496,314,546)),
 'execution_capture_2': (6,(319,496,392,546)),
 'execution_capture_3': (6,(244,551,314,601)),
 'execution_capture_4': (6,(319,551,392,601)),
}
OUT.mkdir(exist_ok=True);RUNTIME.mkdir(exist_ok=True)
records=[]
for name,(page,bounds) in CROPS.items():
 source=ROOT/f'assets/synapse/v3/references/screen_{page:02}.png'
 im=Image.open(source)
 box=tuple(round(c*im.size[i%2]/(412 if i%2==0 else 746)) for i,c in enumerate(bounds))
 crop=im.crop(box)
 dest=OUT/f'{name}.png';crop.save(dest)
 shutil.copy2(dest,RUNTIME/f'synapse_art_{name}.png')
 records.append({'name':name,'source':str(source.relative_to(ROOT)),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'bounds_source_pixels':box,'bounds_normalized_display':bounds,'dimensions':crop.size,'runtime':f'drawable-nodpi/synapse_art_{name}.png','sha256':hashlib.sha256(dest.read_bytes()).hexdigest()})
(OUT/'manifest.json').write_text(json.dumps({'provenance':'User-authorized isolated pixel extraction from approved v3 raster artwork. These are not original vector masters. No full screen, card, or interactive control is extracted. Execution captures are the sample screenshot thumbnails depicted as image content in screen 06.','reference_display':[412,746],'assets':records},indent=2)+'\n')
# Inspection sheet, never packaged.
sheet=Image.new('RGB',(800,((len(records)+4)//5)*150),'#edf3fd');draw=ImageDraw.Draw(sheet)
for i,r in enumerate(records):
 im=Image.open(OUT/(r['name']+'.png'));im.thumbnail((145,120))
 x=(i%5)*160;y=(i//5)*150
 sheet.paste(im,(x+(160-im.width)//2,y));draw.text((x+3,y+125),r['name'],fill='#112240')
catalog=ROOT/'app/build/outputs/visual-parity/artwork-catalog.png'
catalog.parent.mkdir(parents=True, exist_ok=True)
sheet.save(catalog)
print(f'Extracted {len(records)} isolated assets with pixel coordinates and SHA-256 provenance.')
