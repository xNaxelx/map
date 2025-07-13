"""Convert texts to .spacy format for pretraining. Annotations are skipped."""
import re
from pathlib import Path

import spacy
import typer
from spacy.tokens import DocBin
from spacy.util import compile_infix_regex


def convert_plain_text(input_dir: Path, output_file: Path):
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
        # print(file)
        text = file.read_text(encoding="UTF-8")
        text, _ = text.split("\n=== ENTITIES ===\n")
        doc = nlp(text)
        db.add(doc)
    db.to_disk(output_file)


if __name__ == "__main__":
    typer.run(convert_plain_text)
