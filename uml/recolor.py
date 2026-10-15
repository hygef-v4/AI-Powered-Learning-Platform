"""Post-process a package .drawio (layout untouched):
- give each folder its own fill/stroke color
- shrink the tab to a small empty tab and center the label inside the folder body."""
import re, sys
PAL = [('#DAE8FC', '#6C8EBF'), ('#D5E8D4', '#82B366'), ('#FFE6CC', '#D79B00'), ('#E1D5E7', '#9673A6'),
       ('#F8CECC', '#B85450'), ('#FFF2CC', '#D6B656'), ('#D0F0EF', '#3A9A96'), ('#FCE4EC', '#C2185B'),
       ('#E8EAF6', '#3F51B5'), ('#F1F8E9', '#689F38'), ('#FBE9E7', '#E64A19'), ('#E0F7FA', '#0097A7'),
       ('#F3E5F5', '#8E24AA'), ('#FFFDE7', '#F9A825'), ('#ECEFF1', '#546E7A'), ('#EFEBE9', '#6D4C41')]
TAB_W, TAB_H = 50, 14
p = sys.argv[1]
x = open(p, encoding='utf8').read()
n = 0
def fix(m):
    global n
    cell = m.group(0)
    if 'shape=folder' not in cell or 'fillColor=' in cell: return cell
    c = PAL[n % len(PAL)]; n += 1
    cell = re.sub(r'tabWidth=\d+', 'tabWidth=%d' % TAB_W, cell)
    cell = re.sub(r'tabHeight=\d+', 'tabHeight=%d' % TAB_H, cell)
    cell = cell.replace('verticalAlign=top;', 'verticalAlign=middle;').replace('align=left;', 'align=center;')
    cell = re.sub(r'spacingLeft=\d+;', '', cell)
    cell = re.sub(r'spacingTop=\d+;', 'spacingTop=%d;' % TAB_H, cell)
    return cell.replace('style="', 'style="fillColor=%s;strokeColor=%s;' % c, 1)
x = re.sub(r'<mxCell [^>]*>', fix, x)
# arrows entering a folder from the top end at the body, not at the (now empty) tab height
x = x.replace('entryY=0;entryDx=0;entryDy=0;', 'entryY=0;entryDx=0;entryDy=%d;' % TAB_H)
open(p, 'w', encoding='utf8').write(x)
print('folders styled:', n)
