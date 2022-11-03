#!/bin/bash

# set paths
OFFSET=$(dirname "${BASH_SOURCE[0]}")
if [[ -z "${OFFSET}" ]]; then
    OFFSET="."
fi

if [ "$1" == "-v" ]; then
    echo -n "gdart-llvm-0.1-"
    cat "${OFFSET}/version.txt"
    exit
fi

set -x

echo OFFSET="${OFFSET}"
echo "args: ${*}"

# determine tool paths
CLANG_PATH="${OFFSET}/clang+llvm/bin"

CLANG="${CLANG_PATH}"/clang-14 # the clang symlink is broken by SV-COMP infra
LLVM_LINK="${CLANG_PATH}"/llvm-link
LLVM_AS="${CLANG_PATH}"/llvm-as

"${CLANG}" --version
"${LLVM_LINK}" --version
"${LLVM_AS}" --version

JAVA_HOME="${OFFSET}/graalvm-ce"
JAVA="${JAVA_HOME}"/bin/java
"${JAVA}" -version

# compile C input files to LLVM bitcode
C_INPUT_FILES=("${@:2}")
declare -a LLVM_INPUT_FILES
for C_INPUT_FILE in "${C_INPUT_FILES[@]}"; do
    C_FILENAME="${C_INPUT_FILE##*/}"
    BASE_FILENAME="${C_FILENAME%.*}"

    "${CLANG}" -S -emit-llvm -o "${BASE_FILENAME}.ll" "${C_INPUT_FILE}" || exit
    "${LLVM_LINK}" -S -o="${BASE_FILENAME}.ll" "${BASE_FILENAME}.ll" "${OFFSET}/verifier/verifier.ll" || exit
    "${LLVM_AS}" "${BASE_FILENAME}.ll" || exit

    LLVM_INPUT_FILES+=("${BASE_FILENAME}.bc")
done

# run DSE
"${JAVA}" -jar "${OFFSET}/dse/target/dse-0.0.1-SNAPSHOT-jar-with-dependencies.jar" \
    -Ddse.executor="${OFFSET}/executor.sh" \
    -Ddse.executor.args="${LLVM_INPUT_FILES[*]}" \
    -Ddse.terminate.on="assertion|error" \
    -Ddse.b64encode=false \
    -Ddse.dp=z3 \
    -Ddse.explore=BFS \
    -Ddse.witness=true \
    -Ddse.bounds=true \
    -Ddse.bounds.iter=6 \
    -Ddse.bounds.step=6 \
    -Djconstraints.multi=disableUnsatCoreChecking=true \
    > >(tee _gdart.log) 2> >(tee _gdart.err >&2)

# output (adapted from run-gdart.sh)
# remove non-printable characters from log
sed 's/[^[:print:]]//' _gdart.log > _gdart.processed
mv _gdart.processed _gdart.log

# print output
for LOGFILE in *.{log,err,graphml}; do
    echo "# # # # # # # ${LOGFILE}"
    cat "${LOGFILE}"
done
echo "# # # # # # #"

complete=$(grep -a "END OF OUTPUT" _gdart.log)
errors=$(grep -a ERROR _gdart.log | grep -E -a "assertion violated|error encountered" | cut -d '.' -f 3)
buggy=$(grep -a BUGGY _gdart.log | cut -d '.' -f 2)
diverged=$(grep -a DIVERGED _gdart.log | cut -d '.' -f 2)
skipped=$(grep -a SKIPPED _gdart.log | grep -E -v "assumption violation" | cut -d '.' -f 3)

echo "complete: $complete"
echo "err: $errors"
echo "buggy: $buggy"
echo "diverged: $diverged"
echo "skipped: $skipped"

if [[ -n "$errors" ]]; then
  echo "Errors:"
  echo "$errors"
  err=$(echo "$errors" | wc -l)
fi

if [[ ! "$err" -eq "0" ]]; then
    echo "== ERROR"
else
    if [[ -z $buggy ]] && [[ -z $skipped ]] && [[ -n $complete ]] && [[ -z $diverged ]]; then
        echo "== OK"
    else
        echo "== DONT-KNOW"
    fi
fi
