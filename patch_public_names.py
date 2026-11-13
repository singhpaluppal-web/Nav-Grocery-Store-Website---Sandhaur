import os, re
from update_names import names, fb
q, sq = chr(34), chr(39)
qt, nqt = '['+q+sq+']', '[^'+q+sq+']*'
for r, ds, fs in os.walk('.'):
    ds[:] = [d for d in ds if d not in ('.git','node_modules','saved_scripts','31_PDF_Batches')]
    for fn in fs:
        if fn.endswith(('.html','.ts','.tsx','.js')):
            p = os.path.join(r, fn)
            try:
                txt = open(p, encoding='utf-8').read()
                orig = txt
                if 'NAV001' in txt:
                    for i in range(308):
                        s = f'NAV{i+1:03d}'
                        t = names[i].replace(q,'\\'+q).replace(sq,'\\'+sq)
                        b = fb[s]['brand'].replace(q,'\\'+q).replace(sq,'\\'+sq)
                        txt = re.sub('('+qt+s+qt+'[^}]{0,500}?(?:name|title|productName)'+qt+r'?\s*:\s*'+qt+')'+nqt+'('+qt+')', r'\g<1>'+t+r'\g<2>', txt)
                        txt = re.sub('('+qt+s+qt+'[^}]{0,500}?brand'+qt+r'?\s*:\s*'+qt+')'+nqt+'('+qt+')', r'\g<1>'+b+r'\g<2>', txt)
                if txt != orig:
                    open(p, 'w', encoding='utf-8').write(txt)
                    print('Patched:', p)
            except Exception: pass
print('ALL_PATCHED_OK')
