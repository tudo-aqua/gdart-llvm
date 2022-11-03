#!/bin/bash

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
