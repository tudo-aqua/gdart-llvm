; ModuleID = './verifier/verifier.c'
source_filename = "./verifier/verifier.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

; Function Attrs: noinline nounwind optnone uwtable
define dso_local zeroext i1 @__VERIFIER_nondet_bool() #0 {
  ret i1 false
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local signext i8 @__VERIFIER_nondet_char() #0 {
  ret i8 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @__VERIFIER_nondet_int() #0 {
  ret i32 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @__VERIFIER_nondet_uint() #0 {
  ret i32 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i64 @__VERIFIER_nondet_long() #0 {
  ret i64 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i64 @__VERIFIER_nondet_ulong() #0 {
  ret i64 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local float @__VERIFIER_nondet_float() #0 {
  ret float 0.000000e+00
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local double @__VERIFIER_nondet_double() #0 {
  ret double 0.000000e+00
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @__VERIFIER_error() #0 {
  ret void
}

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }

!llvm.module.flags = !{!0, !1, !2}
!llvm.ident = !{!3}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"uwtable", i32 1}
!2 = !{i32 7, !"frame-pointer", i32 2}
!3 = !{!"clang version 14.0.0"}
