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
        x=float(v.get('x',0));y=float(v.get('y',0));w=float(v['width']);h=float(v['height'])
        rx=min(float(v.get('rx',v.get('ry',0))),w/2)
        ry=min(float(v.get('ry',v.get('rx',0))),h/2)
        if rx==0 or ry==0:
            return f'M{x},{y} H{x+w} V{y+h} H{x} Z'
        # SVG rounded rectangles use elliptical arcs, not quadratic corner approximations.
        return (f'M{x+rx},{y} H{x+w-rx} A{rx},{ry} 0 0 1 {x+w},{y+ry} '
                f'V{y+h-ry} A{rx},{ry} 0 0 1 {x+w-rx},{y+h} '
                f'H{x+rx} A{rx},{ry} 0 0 1 {x},{y+h-ry} '
                f'V{y+ry} A{rx},{ry} 0 0 1 {x+rx},{y} Z')
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
