#!/bin/bash
SOURCE=/docs-file/source
TARGET=/docs-file/target

mkdir -p ${SOURCE}
# shellcheck disable=SC2164
cd ${SOURCE}
echo "git clone -b  ${gitee_branch} https://${gitee_user}:${gitee_pass}@gitee.com/modelers/merlin-docs.git"
git clone -b  ${gitee_branch} https://${gitee_user}:${gitee_pass}@gitee.com/modelers/merlin-docs.git
export GITHUB_TOKEN=
cd ${SOURCE}/merlin-docs/docs
rm -rf .vitepress
rm index.md
rm inner_search.md
rm search.md

mkdir -p ${SOURCE}
mkdir -p ${TARGET}

if [ ! -d "${SOURCE}/merlin-docs" ]; then
 rm -rf ${TARGET}
 exit
fi

mkdir -p ${TARGET}/docs
cp -r ${SOURCE}/merlin-docs/docs/zh ${TARGET}/docs/
cp -r ${SOURCE}/merlin-docs/docs/en ${TARGET}/docs/