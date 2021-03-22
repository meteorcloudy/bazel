package com.google.devtools.build.lib.starlarkbuildapi.repository;

import com.google.devtools.build.docgen.annot.DocCategory;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
    name = "override",
    category = DocCategory.BUILTIN,
    doc = "Overrides some information about a Bazel module dependency.")
public interface StarlarkOverrideApi extends StarlarkValue {
  // This space intentionally left blank
}
