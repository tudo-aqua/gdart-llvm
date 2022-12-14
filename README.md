# GDart-LLVM

The repository contains the GDart-LLVM symbolic tracer (which is implemented as a Truffle instrument), support files,
and the packaging scripts the combine it with the [DSE](https://github.com/tudo-aqua/dse) engine and dependencies to
obtain a standalone, [SV-COMP](sv-comp.sosy-lab.org/)-compliant tool.

### Tracer

The tracer is contained in the `tracer` directory and should be compiled by Maven into a single "fat-jar".

### Packaging Scripts

CI and packaging is fully automated by the `.gitlab-ci.yml`. Since CI steps require additional software not readily
available via GitHub and may take multiple hours of CPU time, building is performed on a private GitLab instance at the
moment.
