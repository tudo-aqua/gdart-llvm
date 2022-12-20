#!/bin/bash

# Copyright (C) 2021, Automated Quality Assurance Group,
# TU Dortmund University, Germany. All rights reserved.
#
# executor.sh is licensed under the Apache License,
# Version 2.0 (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software distributed
# under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
# CONDITIONS OF ANY KIND, either express or implied. See the License for the
# specific language governing permissions and limitations under the License.

# set paths
OFFSET=$(dirname "${BASH_SOURCE[0]}")
if [[ -z "${OFFSET}" ]]; then
    OFFSET="."
fi

JAVA_HOME="${OFFSET}/graalvm-ce"

# prepare arguments
declare -a ARGUMENTS
for ARGUMENT in "${@}"; do
    if [[ ${ARGUMENT} == -D* ]]; then
        ARGUMENTS+=("--symbolictracer.${ARGUMENT/-D/}")
    elif [[ -n ${ARGUMENT} ]]; then
        ARGUMENTS+=("${ARGUMENT}")
    fi
done

"${JAVA_HOME}/languages/llvm/bin/lli" \
    --jvm \
    --experimental-options \
    --llvm.llDebug \
    --llvm.verifyBitcode=false \
    --log.llvm.LLDebug.level=OFF \
    --log.llvm.BitcodeVerifier.level=OFF \
    --vm.Dtruffle.class.path.append="${OFFSET}"/tracer/target/tracer-1.0.jar \
    --log.symbolictracer.level=OFF \
    --symbolictracer.run \
    "${ARGUMENTS[@]}"
