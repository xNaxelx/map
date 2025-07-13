#!/usr/bin/env python3
import pathlib
import sys
import json
import re

annotated_dir = pathlib.Path('corpus/annotated')
separator = "\n=== ENTITIES ===\n"

if len(sys.argv) != 2:
    print("Usage:", sys.argv[0], "<file.jsonl>")
    sys.exit(1)

doccano_file = pathlib.Path(sys.argv[1])
lines = doccano_file.read_text(encoding="utf-8").splitlines()
for line in lines:
    json_obj = json.loads(line)
    case_id = json_obj["id"]
    text = json_obj["text"]
    text += separator
    print(case_id)
    for from_pos, to_pos, label in json_obj["label"]:
        index = from_pos
        line_number = text.count('\n', 0, index) + 1
        last_newline = text.rfind('\n', 0, index)
        column_number = index - (last_newline + 1) + 1
        coord = "{}:{}".format(line_number, column_number)
        label_text = text[from_pos:to_pos].rstrip()
        if label_text[0] == " ":
            label_text = label_text[1:]
            column_number += 1
        text = text + "{:<7} {:<18} {}".format(coord, label, label_text) + '\n'
    annotated_dir.joinpath(case_id + ".txt").write_text(text)
