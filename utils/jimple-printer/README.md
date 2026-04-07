# Jimple Printer

Prints SootUp `SootClass` and `Body` objects as Jimple text, and optionally records a line number mapping via the `jimple-mapper` module.

## License

This module is licensed under the **GNU Lesser General Public License v2.1 or later (LGPL-2.1+)**.

The source code is derived from [SootUp](https://github.com/soot-oss/SootUp) (originally the Soot framework), which is also LGPL-2.1+. All original copyright notices are preserved in the source file headers.

### Using this module from MIT-licensed code

The LGPL explicitly permits dynamic linking from differently-licensed software (including MIT). Your MIT-licensed modules remain MIT — only modifications *to this module itself* must stay LGPL. As a standard Maven dependency this module is always dynamically linked and users can replace it if desired.

## Purpose

SootUp converts Java bytecode into Jimple, an intermediate representation. This module writes Jimple as human-readable text and — when a `LineNumberMapper` is provided — records which line in the output file corresponds to which element in the original Java source.

## Class Overview

```
JimplePrinter                       — entry point; prints a SootClass or Body to a PrintWriter
    │   options: UseAbbreviations | OmitLocalsDeclaration | UseImports | LegacyMode | Deterministic
    │   optional: LineNumberMapper  — if set, records line mappings while printing
    │
    └── LabeledStmtPrinter (abstract)  — manages branch labels and trap lists
            ├── NormalStmtPrinter      — full Jimple output (default)
            ├── BriefStmtPrinter       — abbreviated output (UseAbbreviations option)
            └── LegacyJimplePrinter    — Soot ≤ v4 compatibility (LegacyMode option)

AbstractStmtPrinter                 — base class; handles indentation, imports, type printing
BlockGraphIteratorAndTrapAggregator — traverses the StmtGraph in a stable linear order
```

## Usage

### Print a class without mapping

```java
JimplePrinter printer = new JimplePrinter(JimplePrinter.Option.Deterministic);

try (PrintWriter out = new PrintWriter(new FileWriter("MyClass.jimple"))) {
    printer.printTo(sootClass, out);
}
```

### Print a class and collect a line number mapping

```java
LineNumberMapper mapper = new LineNumberMapper("com.example.MyClass");

JimplePrinter printer = new JimplePrinter(mapper, JimplePrinter.Option.Deterministic);

try (PrintWriter out = new PrintWriter(new FileWriter("MyClass.jimple"))) {
    printer.printTo(sootClass, out);
}

// mapper now contains the full mapping — hand it off to jimple-mapper for serialization
LineMappingCollection mapping = mapper.getCollection();
try (Writer w = new FileWriter("MyClass.jimple.map.json")) {
    mapping.writeJson(w);
}
```

### Printer options

| Option | Effect |
|---|---|
| `UseAbbreviations` | Uses `BriefStmtPrinter`; omits types and signatures for readability |
| `OmitLocalsDeclaration` | Skips the local variable declaration block at the start of each method |
| `UseImports` | Emits Java-style import statements to shorten fully-qualified type names |
| `LegacyMode` | Uses `LegacyJimplePrinter`; output compatible with old Soot (≤ v4) |
| `Deterministic` | Sorts interfaces, fields, and methods for stable output across runs |

## Dependencies

| Dependency | Version | License |
|---|---|---|
| `jimple-mapper` | `${project.version}` | MIT |
| `sootup.core` | 2.0.0 | LGPL-2.1+ |
| `sootup.java.core` | 2.0.0 | LGPL-2.1+ |

