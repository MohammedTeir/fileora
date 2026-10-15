#!/usr/bin/env python3
"""Create review artifacts from actual Robolectric renders; never used by the app."""
from pathlib import Path
from PIL import Image, ImageChops, ImageStat, ImageDraw
import json
root=Path(__file__).resolve().parents[1]
out=root/'app/build/outputs/visual-parity/comparisons';out.mkdir(parents=True,exist_ok=True)
metrics={}
for render in sorted((out.parent).glob('screen_*.png')):
 ref=Image.open(root/'assets/synapse/v3/references'/render.name).convert('RGB')
 actual=Image.open(render).convert('RGB')
 # References contain mocked status/gesture bars; retain them so the difference is explicit.
 ref=ref.resize(actual.size,Image.Resampling.LANCZOS)
 w,h=actual.size
 sheet=Image.new('RGB',(w*3,h+24),'#dce2ed');sheet.paste(ref,(0,24));sheet.paste(actual,(w,24));sheet.paste(Image.blend(ref,actual,.5),(w*2,24))
 d=ImageDraw.Draw(sheet)
 for x,t in enumerate(['Approved reference','Actual Compose','50% overlay']):d.text((x*w+8,5),t,fill='#0b1530')
 sheet.save(out/render.name)
 metrics[render.stem]={'viewport':actual.size,'mean_absolute_rgb_difference':round(sum(ImageStat.Stat(ImageChops.difference(ref,actual)).mean)/3,2),'note':'Diagnostic only; not a perceptual similarity score. System bars differ.'}
(out/'metrics.json').write_text(json.dumps(metrics,indent=2)+'\n')
print(f'Wrote {len(metrics)} comparisons to {out}')
