#!/bin/bash
SOURCE=/docs-file/source
TARGET=/docs-file/target

mkdir -p ${SOURCE}
# shellcheck disable=SC2164
cd ${SOURCE}
git clone https://${TOKEN}@github.com/openmerlin/docs.git
export GITHUB_TOKEN=
cd ${SOURCE}/docs/docs
rm -rf .vitepress
rm index.md
rm inner_search.md
rm search.md

mkdir -p ${SOURCE}
mkdir -p ${TARGET}

if [ ! -d "${SOURCE}/docs" ]; then
 rm -rf ${TARGET}
 exit
fi

mkdir -p ${TARGET}/docs
cp -r ${SOURCE}/docs/docs/zh ${TARGET}/docs/
cp -r ${SOURCE}/docs/docs/en ${TARGET}/docs/