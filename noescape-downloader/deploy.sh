#!/bin/bash

# Build the app
mvn clean package

# Create ssh tunnel to the server
ssh -f -N -L 32000:localhost:32000 noescape.fyi

# Build and push docker image
mvn jib:build

# Remove the tunnel
pkill -f "ssh -f -N -L 32000"
