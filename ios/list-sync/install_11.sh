#!/usr/bin/env bash
# This downloads swift5.1 compatible version of CBL. Use with Xcode 11.4
cd Frameworks
rm -rf iOS
rm -rf macOS
rm -rf tvOS
curl https://packages.couchbase.com/releases/couchbase-lite-ios/2.8.0/couchbase-lite-swift_enterprise_2.8.0.zip > cbl.zip
unzip -n cbl.zip
rm -rf cbl.zip
rm -rf cbl
