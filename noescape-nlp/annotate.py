#!/usr/bin/env python3
import pathlib
import spacy
import sys
from spacy.util import compile_infix_regex

text_dir = pathlib.Path('../noescape-text/text/')
corpus_dir = pathlib.Path('corpus/')
separator = "\n=== ENTITIES ===\n"
nlp = spacy.load("output/model-best")

custom_infixes = nlp.Defaults.infixes + [
    r'(?<=[А-ЯҐЄІЇа-яґєії])(?=\d)',  # Split between Ukrainian letter and digit
    r'(?<=\d)(?=[А-ЯҐЄІЇа-яґєії])'   # Split between digit and Ukrainian letter
]
infix_re = compile_infix_regex(custom_infixes)
nlp.tokenizer.infix_finditer = infix_re.finditer


def annotate_text(text):
    doc = nlp(text)
    text = text + separator
    for ent in doc.ents:
        index = ent.start_char
        line_number = text.count('\n', 0, index) + 1
        last_newline = text.rfind('\n', 0, index)
        column_number = index - (last_newline + 1) + 1
        coord = "{}:{}".format(line_number, column_number)
        text = text + "{:<7} {:<18} {}".format(coord, ent.label_, ent.text) + '\n'
    return text

if len(sys.argv) != 2:
    print("Usage:", sys.argv[0], "<'all', 'new' or dir>")
    sys.exit(1)

dir = sys.argv[1]
files = []
if dir == 'corpus':
    print('annotating files in', corpus_dir)
    files = corpus_dir.rglob('*.txt')
elif dir == 'all' or dir == 'new':
    print('annotating', dir, 'files in', text_dir)
    files = text_dir.rglob('*.txt')
else:
    print('annotating files in', text_dir.joinpath(dir))
    files = text_dir.joinpath(dir).glob('*.txt')

for file in sorted(files):
    text = file.read_text()
    parts = text.split(separator)
    if dir != 'new' or len(parts) == 1:
        print(file)
        annotated_text = annotate_text(parts[0])
        file.write_text(annotated_text)
