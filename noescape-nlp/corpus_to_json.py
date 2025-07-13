#!/usr/bin/env python3
import pathlib
import sys
import json
import re

annotated_dir = pathlib.Path('corpus/annotated')
separator = "\n=== ENTITIES ===\n"


def convert_to_json(file):
    print(file)
    text = file.read_text()
    text, entity_lines = text.split(separator)
    lines = text.splitlines()
    offsets = []
    offsets.append(0)
    offset = 0
    for line in lines:
        offset += len(line) + 1
        offsets.append(offset)
    labels = []
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
                actual_text = text[start:end]
                if ent_text != actual_text:
                    raise ValueError("Entity text does not match actual span", line, ent_text, actual_text)
                labels.append([start, end, label])
            else:
                raise ValueError(line)
    data = {
        "id": file.stem,
        "text": text,
        "Comments": [],
        "label": labels
    }
    return json.dumps(data, ensure_ascii=False)


if len(sys.argv) != 2:
    print("Usage:", sys.argv[0], "<all or 00>")
    sys.exit(1)

dir = sys.argv[1]
files = sorted(annotated_dir.rglob('*.txt') if dir == 'all' else annotated_dir.joinpath(dir).glob('*.txt'))
jsonl = ""
for file in files:
    # print(file)
    jsonl += convert_to_json(file) + '\n'
train_jsonl = pathlib.Path('train.jsonl')
train_jsonl.write_text(jsonl)
print("Created file", train_jsonl)