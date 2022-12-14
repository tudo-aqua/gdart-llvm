# The GDart-LLVM Tool

GDart-LLVM is a tool ensemble for the dynamic symbolic execution of C verification tasks. This tool archive ships all
five components required for running the tool:

- [DSE](https://github.com/tudo-aqua/dse) is a generic dynamic symbolic execution engine that handle the "logistics" of
  DSE. It is shipped in the `dse` directory.
- [GDart-LLVM](https://github.com/tudo-aqua/gdart-llvm) is a Truffle instrument that performs symbolic path recording by
  montoring the program's execution. It is shipped in the `tracer` directory.
- [JConstraints](https://github.com/tudo-aqua/jconstraints) is a meta solving library for SMT problems that also handles
  SMT-Lib encoding and decoding. It is repackaged into the DSE and GDart-LLVM tools.
- [GraalVM](https://www.graalvm.org/) is a generic virtual machine based on the HotSpot JVM. Execution support for
  arbitrary languages can be added via its Truffle API that also offers instrumentation. Sulong adds support for LLVM
  Bitcode. It is shipped in the `graalvm-ce` directory.
- [LLVM](https://llvm.org/) is a compiler toolkit using LLVM Bitcode as an intermediate representation.
  [Clang](https://clang.llvm.org/) is a LLVM compiler frontend for C and C++. It is shipped in the `clang+llvm`
  directory.

To verify a a SV-COMP benchmark example, you can call the provided `run-gdart.sh` script:
```sh
./run-gdart.sh /path/to/property.prp /path/to/benchmark/file.c
```
At the moment, the property is ignored and `Reach-Safety` is assumed.

GDart-LLVM was tested on Ubuntu 20.04. and the AMD64 architecture. Support for other OS hinges on the native components
(GraalVM, Sulong, LLVM, and Clang).

