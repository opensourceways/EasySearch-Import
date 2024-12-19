#!/bin/bash
SOURCE=/docs-file/source
TARGET=/docs-file/target

mkdir -p ${SOURCE}
# shellcheck disable=SC2164
cd ${SOURCE}
git clone -b ${portal_branch}   https://${gitee_user}:${gitee_pass}@gitcode.com/openUBMC/docs.git
git clone  -b  ${gitee_branch} https://${TOKEN}@github.com/opensourceways/openUBMC-portal.git

export GITHUB_TOKEN=
cd ${SOURCE}/docs/docs


mkdir -p ${TARGET}

if [ ! -d "${SOURCE}/docs" ]; then
 rm -rf ${TARGET}
 exit
fi

mkdir -p ${TARGET}/docs
mkdir -p ${TARGET}/portal
cp -r ${SOURCE}/openUBMC-portal/packages/website/config/markdown ${TARGET}/website/
cp -r ${SOURCE}/openUBMC-portal/packages/website/config/org ${TARGET}/website/
cp -r ${SOURCE}/docs/docs/zh ${TARGET}/docs/
cp -r ${SOURCE}/docs/docs/en ${TARGET}/docs/
rm -rf ${SOURCE}