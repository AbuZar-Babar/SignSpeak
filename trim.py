import sys

path = r'c:\Users\AbuZar\Desktop\Fyp\signspeak\signspeak-pakistansignlanguage-dataset-model-mobileapp\kotlin app\app\src\main\java\com\example\kotlinfrontend\MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

print('Total lines:', len(lines))
# Keep only up to line 1186 (0-indexed: 0..1185)
kept = lines[:1186]

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(kept)

print('Done. Wrote', len(kept), 'lines.')
