import zipfile
import xml.etree.ElementTree as ET

# Path to the excel file
xlsx_file = r"D:\nav web\Master_Plan.xlsx"

try:
    with zipfile.ZipFile(xlsx_file, 'r') as z:
        # 1. Read all shared strings
        strings = []
        if 'xl/sharedStrings.xml' in z.namelist():
            tree = ET.fromstring(z.read('xl/sharedStrings.xml'))
            for elem in tree.findall('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}si'):
                text_nodes = elem.findall('.//{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t')
                strings.append("".join([t.text for t in text_nodes if t.text]))

        # 2. Read sheet rows
        sheet_tree = ET.fromstring(z.read('xl/worksheets/sheet1.xml'))
        rows = []
        for row in sheet_tree.findall('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}sheetData/{http://schemas.openxmlformats.org/spreadsheetml/2006/main}row'):
            cols = []
            for c in row.findall('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}c'):
                v = c.find('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}v')
                if v is not None and v.text:
                    if c.attrib.get('t') == 's':
                        idx = int(v.text)
                        cols.append(strings[idx] if idx < len(strings) else '')
                    else:
                        cols.append(v.text)
            if cols:
                rows.append(" | ".join(cols))

    output_path = r"D:\nav web\all_master_rows.txt"
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(rows))
    print(f"Success: Extracted {len(rows)} rows into {output_path}")

except Exception as e:
    print(f"Error: {e}")
