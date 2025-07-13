#!/usr/bin/env python3
import pathlib
import sys
import spacy

if len(sys.argv) != 2:
    print("usage: python", sys.argv[0], "<file>")
    sys.exit(1)

nlp = spacy.load("output/model-best")
text = pathlib.Path(sys.argv[1]).read_text()
doc = nlp(text)
for ent in doc.ents:
    print(ent.label_, ent.text)
