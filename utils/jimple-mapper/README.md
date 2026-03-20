# Jimple Mapper

Tracks the mapping between line numbers in generated Jimple files and their original locations in Java source code.

## License

This module is licensed under the **MIT License**, the same as the parent project.

It depends on [SootUp](https://github.com/soot-oss/SootUp) (LGPL-2.1+) as an external library for source position information (`sootup.core.model.Position`).

## Purpose

When Java bytecode is converted to Jimple, the resulting `.jimple` files have different line numbers than the original `.java` source files. This module records the correspondence between the two, enabling tools (e.g. static analysis, SonarQube plugins) to map Jimple findings back to the original source location.

## Data Model

```
LineNumberMapper         — collects mappings for one class during Jimple printing
    └── LineMappingCollection  — immutable snapshot; can be serialized to JSON
            └── LineMapping[]  — one entry per recorded element
                    ├── jimpleLine       : int          — line in the .jimple file
                    ├── elementType      : ElementType  — CLASS | METHOD | FIELD | STATEMENT
                    ├── elementSignature : String       — fully-qualified signature
                    └── sourcePosition   : SourcePosition
                            ├── firstLine / lastLine   — source line range
                            └── firstCol  / lastCol    — source column range
```

The source file name (e.g. `TestClass.java`) is stored once on `LineMappingCollection`, not repeated on every entry.

`SourcePosition` and `LineMapping` use a flat structure to keep protobuf serialization straightforward in the future.

## Usage

### Recording mappings (during Jimple printing)

```java
LineNumberMapper mapper = new LineNumberMapper("com.example.MyClass");
mapper.setSourceFileName("MyClass.java");   // just the filename, no path

// called by the Jimple printer as it emits each element:
mapper.recordClassPosition(1,  "com.example.MyClass",                       position);
mapper.recordFieldPosition(3,  "<com.example.MyClass: int value>",           position);
mapper.recordMethodPosition(7, "<com.example.MyClass: void doWork()>",       position);
mapper.recordStmtPosition(10,  "r0 := @this: com.example.MyClass",           position);
```

### Serializing to JSON

```java
LineMappingCollection collection = mapper.getCollection();

// as a String
String json = collection.toJson();

// directly to a Writer / FileWriter
try (Writer w = new FileWriter("MyClass.jimple.map.json")) {
    collection.writeJson(w);
}
```

### Example JSON output

```json
{
  "className" : "com.example.MyClass",
  "sourceFileName" : "MyClass.java",
  "mappings" : [ {
    "jimpleLine" : 1,
    "elementType" : "CLASS",
    "elementSignature" : "com.example.MyClass",
    "sourcePosition" : { "firstLine" : 10, "lastLine" : 50, "firstCol" : 1, "lastCol" : 1 }
  }, {
    "jimpleLine" : 7,
    "elementType" : "METHOD",
    "elementSignature" : "<com.example.MyClass: void doWork()>",
    "sourcePosition" : { "firstLine" : 20, "lastLine" : 30, "firstCol" : 3, "lastCol" : 4 }
  } ]
}
```

## API Reference

| Class | Responsibility |
|---|---|
| `LineNumberMapper` | Collects mappings during Jimple generation; call `getCollection()` when done |
| `LineMappingCollection` | Immutable result; provides `toJson()` / `writeJson(Writer)` |
| `LineMapping` | A single mapping entry (jimple line → source position + element type) |
| `SourcePosition` | Source location (line/column range); flat structure for protobuf readiness |
| `ElementType` | Enum: `CLASS`, `METHOD`, `FIELD`, `STATEMENT` |

## Future: Protobuf Support

The data model is intentionally flat and free of inheritance to make a future `.proto` definition straightforward:

```proto
// planned — not yet implemented
message LineMapping { ... }
message LineMappingCollection { ... }
```

## Dependencies

| Dependency | Version | License |
|---|---|---|
| `sootup.core` | 2.0.0 | LGPL-2.1+ |
| `sootup.java.core` | 2.0.0 | LGPL-2.1+ |
| `jackson-databind` | 2.18.2 | Apache-2.0 |

