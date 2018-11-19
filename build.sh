#!/bin/sh

# We do want to fail if any command fails!
set -e

# Remake out folder
rm -rf out
mkdir out

# Clone this repo into the out folder
git clone . out/repo
cd out/repo

# Remove things that aren't for the zip file
rm -rf .git
rm Config

# Build docs folder
cd docs
mkdir _static
make html
mv _build/html ../../to_upload
cd ..
mkdir ../to_upload/files

# Create zip file
zip -r ../to_upload/files/student-files.zip .

# Create dummy null lambda for API
cd null_lambda
./gradlew shadowJar
cp build/libs/*.jar ../../to_upload/files

cd ..
cd sqs_order_forwarder
./gradlew shadowJar
cp build/libs/*.jar ../../to_upload/files
