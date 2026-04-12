#!/bin/bash
# deploy.sh — build and publish a themidibus release to S3

S3_BUCKET="s3://smallbutdigital.com/releases/themidibus"
AWS_PROFILE="${MIDIBUS_SBD_AWS_PROFILE:?ERROR: MIDIBUS_SBD_AWS_PROFILE is not set}"

# Extract version from library.properties
VERSION=$(grep '^version' library.properties | sed 's/[^0-9]//g')
PADDED=$(printf "%03d" "$VERSION")

# Check S3 for existing release
if aws s3 ls --profile "$AWS_PROFILE" "$S3_BUCKET/themidibus-${PADDED}.zip" 2>/dev/null | grep -q .; then
    echo "ERROR: Release $PADDED already exists on S3. Bump the version in library.properties first."
    exit 1
fi

# Build
ant clean && ant zip
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed."
    exit 1
fi

# Upload versioned files
aws s3 cp --profile "$AWS_PROFILE" output/themidibus.zip "$S3_BUCKET/themidibus-${PADDED}.zip"
aws s3 cp --profile "$AWS_PROFILE" library.properties    "$S3_BUCKET/themidibus-${PADDED}.txt"

# Update latest
aws s3 cp --profile "$AWS_PROFILE" "$S3_BUCKET/themidibus-${PADDED}.zip" "$S3_BUCKET/themidibus-latest.zip"
aws s3 cp --profile "$AWS_PROFILE" "$S3_BUCKET/themidibus-${PADDED}.txt" "$S3_BUCKET/themidibus-latest.txt"

echo "Deployed themidibus-${PADDED} and updated -latest."
