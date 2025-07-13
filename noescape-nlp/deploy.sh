#!/bin/bash

# Remove the old model
#ssh root@noescape.fyi "rm -rf /var/noescape-nlp/output/model-best ; mkdir -p /var/noescape-nlp/output/model-best"

# Upload model to the server
#scp -r output/model-best root@noescape.fyi:/var/noescape-nlp/output

# Build doker image
docker build . -t noescape-nlp -t localhost:32000/noescape-nlp

# Create ssh tunnel to the server
ssh -f -N -L 32000:localhost:32000 noescape.fyi

# Push image to the server through the tunnel
docker push localhost:32000/noescape-nlp:latest

# Remove the tunnel
pkill -f "ssh -f -N -L 32000"

