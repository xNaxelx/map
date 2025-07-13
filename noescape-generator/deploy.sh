#!/bin/bash

# Cleanup and update repository
git -C ../noescape-data stash --include-untracked
git -C ../noescape-data pull --rebase

# Build the app
mvn clean package -DskipTests

# Create ssh tunnel to the server
ssh -f -N -L 32000:localhost:32000 noescape.fyi

# Build and push docker image
mvn jib:build

# Remove the tunnel
pkill -f "ssh -f -N -L 32000"

# Generate site files
mvn exec:java -Dexec.mainClass=noescape.Main

# Zip all texts
zip -r target/text.zip ../noescape-text/text

# Upload data and texts to server
scp -r target/data noescape.fyi:/var/noescape/
scp -r target/text.zip noescape.fyi:/var/noescape/data/

# Update data repository
cp data-geocoded.tsv ../noescape-data/data.tsv
cd ../noescape-data || exit 1
git commit -a -m "Update data"
git push
cd -
