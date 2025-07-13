<!-- WEASEL: AUTO-GENERATED DOCS START (do not remove) -->

# 🪐 Weasel Project: noescape.fyi

Named Entity Recognition (NER) project for [noescape.fyi](https://noescape.fyi)

## Project set up
Create virtual environment

  python -m venv .env

Install required modules

    pip install weasel spacey

## 📋 project.yml

The [`project.yml`](project.yml) defines the data assets required by the
project, as well as the available commands and workflows. For details, see the
[Weasel documentation](https://github.com/explosion/weasel).

### ⏯ Commands

The following commands are defined by the project. They
can be executed using [`weasel run [name]`](https://github.com/explosion/weasel/tree/main/docs/cli.md#rocket-run).
Commands are only re-run if their inputs have changed.

| Command | Description |
| --- | --- |
| `prepare-pretrain` |  |
| `pretrain` |  |
| `prepare-train` | Convert the data to spaCy's binary format |
| `train` | Train the NER model |
| `evaluate` | Evaluate the model and export metrics |
| `package` | Package the trained model as a pip package |

### ⏭ Workflows

The following workflows are defined by the project. They
can be executed using [`weasel run [name]`](https://github.com/explosion/weasel/tree/main/docs/cli.md#rocket-run)
and will run the specified commands in order. Commands are only re-run if their
inputs have changed.

| Workflow | Steps |
| --- | --- |
| `all` | `prepare-pretrain` &rarr; `pretrain` &rarr; `prepare-train` &rarr; `train` &rarr; `evaluate` |

<!-- WEASEL: AUTO-GENERATED DOCS END (do not remove) -->
