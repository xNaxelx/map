"""Convert annotated texts in custom format to .spacy format."""
import re
from pathlib import Path

import spacy
import typer
from spacy.tokens import DocBin
from spacy.util import compile_infix_regex



def process(file, nlp):
    text = file.read_text(encoding="UTF-8")
    text, entity_lines = text.split("\n=== ENTITIES ===\n")
    lines = text.splitlines()
    offsets = []
    offsets.append(0)
    offset = 0
    for line in lines:
        offset += len(line) + 1
        offsets.append(offset)

    doc = nlp(text)
    ents = []
    pattern = r"^(\d+):(\d+)\s+([A-Z_]+)\s+(.+)$"
    for line in entity_lines.splitlines():
        if line:
            match = re.match(pattern, line)
            if match:
                line = int(match.group(1))
                column = int(match.group(2))
                label = match.group(3)
                ent_text = match.group(4)
                start = offsets[line - 1] + column - 1
                end = start + len(ent_text)
                span = doc.char_span(start, end, label=label)
                if span is None:
                    print(file)
                    print("start", start)
                    print("end", end)
                    print("label", label)
                    print("label_text", text[start - 5:start] + "|" + text[start:end] + "|" + text[end:end + 5])
                elif (span.text != ent_text):
                    raise ValueError(line, ent_text, span.text)
                else:
                    ents.append(span)
            else:
                raise ValueError(line)
    doc.ents = ents
    return doc


def convert(input_dir: Path, output_file: Path):
    nlp = spacy.blank("uk")
    custom_infixes = nlp.Defaults.infixes + [
        r'(?<=[А-ЯҐЄІЇа-яґєії])(?=\d)',  # Split between Ukrainian letter and digit
        r'(?<=\d)(?=[А-ЯҐЄІЇа-яґєії])'  # Split between digit and Ukrainian letter
    ]
    infix_re = compile_infix_regex(custom_infixes)
    nlp.tokenizer.infix_finditer = infix_re.finditer

    db = DocBin()
    files = sorted(input_dir.rglob('*.txt'))
    for file in files:
        print(file)
        doc = process(file, nlp)
        db.add(doc)
    db.to_disk(output_file)


if __name__ == "__main__":
    typer.run(convert)
