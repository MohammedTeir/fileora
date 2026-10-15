#!/usr/bin/env python3
"""Convert the approved v3 SVG primitives to Android vectors without raster tracing."""
from pathlib import Path
import xml.etree.ElementTree as E
ROOT = Path(__file__).resolve().parents[1]
NS = 'http://schemas.android.com/apk/res/android'
E.register_namespace('android', NS)
def a(k): return '{'+NS+'}'+k
def shape(e):
    t=e.tag.split('}')[-1]; v=e.attrib
    if t=='path': return v['d']
    if t in ('circle','ellipse'):
        x=float(v['cx']); y=float(v['cy']); rx=float(v.get('r',v.get('rx','0'))); ry=float(v.get('r',v.get('ry','0')))
        return f'M{x-rx},{y} a{rx},{ry} 0,1 0,{2*rx},0 a{rx},{ry} 0,1 0,{-2*rx},0 Z'
    if t=='rect':
        x=float(v['x']);y=float(v['y']);w=float(v['width']);h=float(v['height']);r=float(v.get('rx','0'))
        return f'M{x+r},{y} H{x+w-r} Q{x+w},{y} {x+w},{y+r} V{y+h-r} Q{x+w},{y+h} {x+w-r},{y+h} H{x+r} Q{x},{y+h} {x},{y+h-r} V{y+r} Q{x},{y} {x+r},{y} Z'
    raise ValueError(f'Unsupported SVG element: {t}')
for p in sorted((ROOT/'assets/synapse/v3/icons/svg').glob('*.svg')):
    svg=E.parse(p).getroot()
    assert svg.get('viewBox')=='0 0 24 24'
    vector=E.Element('vector',{a('width'):'24dp',a('height'):'24dp',a('viewportWidth'):'24',a('viewportHeight'):'24'})
    for e in svg:
        E.SubElement(vector,'path',{a('pathData'):shape(e),a('fillColor'):'#00000000',a('strokeColor'):svg.get('stroke'),a('strokeWidth'):svg.get('stroke-width'),a('strokeLineCap'):svg.get('stroke-linecap'),a('strokeLineJoin'):svg.get('stroke-linejoin')})
    E.indent(vector)
    E.ElementTree(vector).write(ROOT/f'app/src/main/res/drawable/synapse_icon_{p.stem}.xml',encoding='utf-8',xml_declaration=True)
print('Converted 38 SVG concepts; original SVGs preserved.')
