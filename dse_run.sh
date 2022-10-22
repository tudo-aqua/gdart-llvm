#!/bin/bash

# set paths
OFFSET=$(dirname "${BASH_SOURCE[0]}")
if [[ -z "${OFFSET}" ]]; then
    OFFSET="."
fi
# echo OFFSET="${OFFSET}"

if [ "$1" == "-v" ]; then
    echo -n "gdart-llvm-0.1-"
    cat "${OFFSET}/version.txt"
    exit
fi

echo "${*}" # debug

if [[ -f "/usr/bin/clang" ]]; then
    CLANG_PATH=/usr/bin
else
    CLANG_PATH=gdart-llvm/clang+llvm/bin
fi
export CLANG_PATH
echo CLANG_PATH="${CLANG_PATH}"

if [[ -d "/usr/lib/jvm/java-11-graalvm" ]]; then
    JAVA_HOME=/usr/lib/jvm/java-11-graalvm
else
    JAVA_HOME=gdart-llvm/graalvm-ce
fi
export JAVA_HOME
echo JAVA_HOME="${JAVA_HOME}"

# compile C input files to LLVM bitcode
C_INPUT_FILES=("${@:2}")
declare -a LLVM_INPUT_FILES
for C_INPUT_FILE in "${C_INPUT_FILES[@]}"; do
    (time ( { \
        "${CLANG_PATH}"/clang -S -emit-llvm -o "${C_INPUT_FILE%.*}.ll" "${C_INPUT_FILE}"; \
        "${CLANG_PATH}"/llvm-link -S -o="${C_INPUT_FILE%.*}.ll" "${C_INPUT_FILE%.*}.ll" "${OFFSET}/verifier/verifier.ll"; \
        "${CLANG_PATH}"/llvm-as "${C_INPUT_FILE%.*}.ll"; \
    } 2>&3 ) ) 3>&2 2>"time_$(basename "${C_INPUT_FILE}").log"
    LLVM_INPUT_FILES+=("${C_INPUT_FILE%.*}.bc")
done

# run DSE
"${JAVA_HOME}"/bin/java -jar "${OFFSET}/dse/target/dse-0.0.1-SNAPSHOT-jar-with-dependencies.jar" \
    -Ddse.executor="${OFFSET}/executor.sh" \
    -Ddse.executor.args="${LLVM_INPUT_FILES[*]}" \
    -Ddse.terminate.on="assertion|error" \
    -Ddse.b64encode=false \
    -Ddse.dp=z3 \
    -Ddse.explore=BFS \
    -Ddse.witness=true \
    -Ddse.dp=multi \
    -Ddse.bounds=true \
    -Ddse.bounds.iter=6 \
    -Ddse.bounds.step=6 \
    -Djconstraints.multi=disableUnsatCoreChecking=true \
    > _gdart.log \
    2> _gdart.err

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

complete=$(cat _gdart.log | grep -a "END OF OUTPUT")
errors=$(cat _gdart.log | grep -a ERROR | grep -E -a "assertion violated|error encountered" | cut -d '.' -f 3)
buggy=$(cat _gdart.log | grep -a BUGGY | cut -d '.' -f 2)
diverged=$(cat _gdart.log | grep -a DIVERGED | cut -d '.' -f 2)
skipped=$(cat _gdart.log | grep -a SKIPPED | egrep -v "assumption violation" | cut -d '.' -f 3)

echo "complete: $complete"
echo "err: $errors"
echo "buggy: $buggy"
echo "diverged: $diverged"
echo "skipped: $skipped"

if [[ -n "$errors" ]]; then
  echo "Errors:"
  echo "$errors"
  err=$(echo $errors | wc -l)
fi

if [[ ! "$err" -eq "0" ]]; then
    echo "== ERROR"
else
    if [[ -z $buggy ]] && [[ -z $skipped ]] && [[ ! -z $complete ]] && [[ -z $diverged ]]; then
        echo "== OK"
    else
        echo "== DONT-KNOW"
    fi
fi
