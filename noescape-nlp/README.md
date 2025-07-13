Text annotator
=============

Use ../noescape-weasel to train model.
Put the model to output/model-best

Annotate new non-annotated texts in ../noescape-text/text:

    ./annotate.py new

Re-annotate all texts in ../noescape-text/text:

    ./annotate.py all

Re-annotate corpus (selected texts for training a new model):

    ./annotate.py corpus

After re-annotating corpus review and fix annotations manually.

