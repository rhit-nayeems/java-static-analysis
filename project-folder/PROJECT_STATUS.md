# Project Status Report

Last validated: 2026-04-03  
Project: `LinterProject` (`rhit.csse.csse374:LinterProject:1.0-rc3`)

## 1. Executive Summary

This repository is a Java static-analysis project that combines two analysis modes:

- source-based linting for `.java` files
- bytecode-based linting for compiled `.class` files using ASM

The project is functional, builds successfully, has both CLI and Swing GUI entry points, and has a meaningful automated test suite. Its strongest engineering feature is the hybrid architecture: simple textual/style checks are performed on source files, while structural checks such as SRP/cohesion and pattern detection are implemented against bytecode.

As of 2026-04-03, the project is in a solid "demo and interview ready" state, with these qualifications:

- Build and tests are passing.
- The CLI flow works end to end.
- The GUI code is present and structurally sound.
- The rule set is broad for a student/static-analysis project.
- Some detectors are intentionally heuristic and produce false positives or false negatives.
- Test coverage is uneven across implemented linters.
- Some documentation artifacts are stale relative to the current implementation.

Overall assessment: the project is technically credible, modular, and strong enough to discuss in an SWE internship interview, especially as an example of static analysis, modular design, configuration-driven behavior, and bytecode inspection with ASM.

## 2. Validation Snapshot

### Verified during this review

- Repository inspected directly
- Core source files read across `presentation`, `domain`, and `datastorage`
- Representative example files read
- Test suite executed
- CLI run executed against repository examples

### Commands verified successfully

```bash
mvn test
java -jar target/LinterProject-1.0-rc3-jar-with-dependencies.jar
```

### Test result at validation time

- Command: `mvn test`
- Result: `BUILD SUCCESS`
- Total tests run: `15`
- Failures: `0`
- Errors: `0`
- Skipped: `0`

### CLI sanity-check result at validation time

The built jar was run against:

- `src/main/java/example`
- `target/classes/example`

Observed output included:

- `3` SRP violations
- `1` facade candidate
- `0` strategy candidates
- `1` singleton candidate
- `1` decorator candidate
- `2` adapter candidates
- `0` boolean-flag issues
- `1` public non-final field issue
- `0` too-many-parameters issues
- multiple design-risk findings
- multiple source-level style and least-knowledge findings

This confirms that the end-to-end execution path is working and that both source and bytecode rule groups are being invoked.

## 3. Repository Inventory

### Production code

- `49` Java files under `src/main/java`
- Main packages:
  - `presentation`
  - `domain`
  - `datastorage`
  - `example`
  - `inlineexamples`

### Test code

- `11` Java files under `src/test/java`
- `9` test classes ending in `*Test.java`

### Build artifacts present

- `target/LinterProject-1.0-rc3.jar`
- `target/LinterProject-1.0-rc3-jar-with-dependencies.jar`

### Key non-code files

- `pom.xml`
- `README.md`
- `SRP_LINTER_README.md`
- `linter.properties`
- `current.puml`
- `plan.puml`
- `project_profile.json`

## 4. Intended Purpose

The project was built as a Java linter for a software design course. The explicit goal is not only to catch low-level style issues, but also to detect:

- design-principle violations
- design smells
- pattern candidates
- architectural risk indicators

That purpose explains why the repository goes beyond regex/text scanning and uses ASM to inspect compiled bytecode.

## 5. Current Architecture

## 5.1 Layering

The project follows a simple 3-layer design:

### `presentation`

Responsibilities:

- CLI entry point
- GUI entry point
- linter construction
- linter execution orchestration
- user interaction and output formatting

Key classes:

- `presentation.LinterMain`
- `presentation.LinterGuiMain`
- `presentation.LinterGuiFrame`
- `presentation.LinterFactory`
- `presentation.LinterRunner`

### `domain`

Responsibilities:

- core `Linter` contract
- all concrete lint rules
- configuration model
- LCOM/cohesion metric computation
- UML generation utility

Key classes:

- `domain.Linter`
- `domain.LinterConfig`
- `domain.AbstractSourceLinter`
- `domain.AbstractASMLinter`
- `domain.LCOMCalculator`

### `datastorage`

Responsibilities:

- config loading from properties file
- path expansion and recursive file discovery
- ASM-based `.class` loading

Key classes:

- `datastorage.ConfigLoader`
- `datastorage.FileLoader`
- `datastorage.ASMReader`

## 5.2 Core Contract

All lint-capable components share one main contract:

```java
public interface Linter {
    String lint(List<File> files);
}
```

This is the main extension point. Adding a new linter requires:

1. implementing `Linter` directly or extending one of the abstract base classes
2. registering the implementation in `LinterFactory`
3. optionally adding configuration and tests

## 5.3 Execution Model

### CLI path

1. `presentation.LinterMain` loads `linter.properties`
2. `LinterFactory` creates enabled linter instances
3. user enters file or directory paths
4. `FileLoader` expands and deduplicates input paths
5. `LinterRunner` splits inputs into:
   - `.class` files/directories
   - non-`.class` files/directories
6. matching linters run against each bucket
7. results are concatenated and printed

### GUI path

1. `presentation.LinterGuiMain` launches Swing UI
2. GUI discovers all registered linters
3. user selects one linter and compatible files
4. user may load/save config and adjust thresholds
5. selected linter runs asynchronously via `SwingWorker`
6. output is displayed in a text area with status updates

## 6. Design Patterns and Engineering Ideas Present

The codebase visibly uses or approximates the following design ideas:

- Template Method:
  - `AbstractSourceLinter`
  - `AbstractASMLinter`
- Factory-style construction:
  - `LinterFactory`
- Dependency injection:
  - shared services such as `ASMReader` and `LCOMCalculator` are passed into some linters
- Layered architecture:
  - presentation -> domain -> datastorage
- Config-driven behavior:
  - `LinterConfig` + `ConfigLoader`

This is one of the stronger discussion points for interviews because the project is not just a pile of rule classes; it has a reusable execution framework.

## 7. Rule Inventory

The repository currently contains `15` concrete lint-capable implementations:

- `SnakeLinter`
- `LeastKnowledgePrincipleLinter`
- `TrailingWhitespaceLinter`
- `UnusedImportLinter`
- `PublicNonFinalFieldLinter`
- `SRPLinter`
- `FacadePatternLinter`
- `StrategyPatternLinter`
- `SingletonPatternLinter`
- `DecoratorPatternLinter`
- `AdapterPatternLinter`
- `BooleanFlagMethodLinter`
- `TooManyParametersLinter`
- `DesignRiskLinter`
- `PlantUMLGenerator`

## 7.1 Source-based rules

| Rule | Input | Technique | Purpose | Config | Dedicated Test Present |
|---|---|---|---|---|---|
| `SnakeLinter` | `.java` | regex over source text | flags names not matching `snake_case` | none | No |
| `LeastKnowledgePrincipleLinter` | `.java` | regex/statement heuristics | flags chained calls/member access as Law of Demeter issues | none | Yes |
| `TrailingWhitespaceLinter` | text files in non-class bucket | line scanning | flags trailing spaces/tabs | none | No |
| `UnusedImportLinter` | `.java` | import collection + token usage regex | flags unused explicit non-static imports | none | No |

### Notes

- `SnakeLinter` is enforcing a custom naming rule that is not standard Java naming convention.
- `LeastKnowledgePrincipleLinter` is one of the noisiest detectors and currently over-flags common chained access patterns.
- `TrailingWhitespaceLinter` is simple and low-risk.
- `UnusedImportLinter` is a reasonable heuristic for simple files but does not perform full parsing.

## 7.2 Bytecode/ASM-based rules

| Rule | Input | Technique | Purpose | Config | Dedicated Test Present |
|---|---|---|---|---|---|
| `PublicNonFinalFieldLinter` | `.class` | ASM field access flags | flags public mutable fields | none | No |
| `SRPLinter` | `.class` | ASM + LCOM4 | flags low-cohesion classes as SRP candidates | `srp_lcom_threshold` | No |
| `FacadePatternLinter` | `.class` | delegation and subsystem heuristics | finds likely facade classes | none | Yes |
| `StrategyPatternLinter` | `.class` | interface/implementor heuristics | finds likely strategy usage | none | Yes |
| `SingletonPatternLinter` | `.class` | constructor/field/accessor heuristics | finds singleton candidates | none | Yes |
| `DecoratorPatternLinter` | `.class` | inheritance + same-type composition heuristic | finds decorator candidates | none | No |
| `AdapterPatternLinter` | `.class` | target/adaptee/delegation heuristic | finds adapter candidates | none | Yes |
| `BooleanFlagMethodLinter` | `.class` | ASM method descriptor analysis | flags boolean/Boolean parameters | none | Yes |
| `TooManyParametersLinter` | `.class` | ASM descriptor parameter count | flags large parameter lists | `too_many_parameters_limit` | Yes |
| `DesignRiskLinter` | `.class` | weighted scoring over size/coupling/state/cohesion | scores architectural risk | none | No |
| `PlantUMLGenerator` | `.class` | bytecode model extraction | generates PlantUML text | none | No |

### Notes

- `SRPLinter` and `DesignRiskLinter` share `LCOMCalculator`.
- `PlantUMLGenerator` acts more like a modeling utility than a linter, but it uses the same contract and execution framework.
- Pattern detectors are best described as "candidate detectors" rather than guaranteed recognizers.

## 8. Deep-Dive: SRP and LCOM

The technically strongest subsystem is the SRP/cohesion analysis.

### Implementation summary

`domain.LCOMCalculator` computes an LCOM4-style metric by:

1. filtering to instance methods and instance fields
2. analyzing ASM bytecode instructions to determine which methods access which fields
3. building an implicit graph where methods are connected if they share field access
4. computing connected components with a union-find structure
5. using component count as the LCOM score

### Interpretation in this project

- `LCOM = 1`: cohesive class
- `LCOM >= threshold`: potential SRP violation

### Consumers

- `SRPLinter`
- `DesignRiskLinter`

### Why this matters

This is a good interview talking point because it demonstrates:

- bytecode analysis with ASM
- graph modeling
- union-find usage
- turning a software design principle into a measurable heuristic

## 9. Configuration Status

The main configuration file is `linter.properties`.

Current checked-in values:

```properties
enabled_linters=all
too_many_parameters_limit=5
srp_lcom_threshold=2
```

### Config behavior

- Missing or invalid config file falls back to defaults.
- `enabled_linters=all` means all registered linters are enabled.
- A comma-separated list can enable specific linters by simple class name.
- Threshold values are validated as positive integers.

### Current implementation status

- Config loading is implemented and tested.
- CLI uses config for enabled linters and thresholds.
- GUI allows load/save/edit of config values.

## 10. Input Handling Status

### `FileLoader`

Current behavior:

- accepts comma-separated paths
- ignores empty tokens
- skips missing paths
- recursively expands directories
- deduplicates by absolute path
- preserves a deterministic traversal order by sorting directory contents

### `LinterRunner`

Current behavior:

- separates input into `.class` and non-`.class` buckets
- runs only class-file linters on `.class` inputs
- runs source/text linters on non-`.class` inputs

### Practical implication

The project can analyze:

- single files
- multiple files
- whole source trees
- whole compiled-output trees such as `target/classes`

## 11. GUI Status

The GUI is implemented in `presentation.LinterGuiFrame`.

### Features currently present

- linter selection dropdown
- compatible file suggestion dropdown
- manual file/folder browsing
- config path selection
- config load/save
- threshold editing
- asynchronous execution via `SwingWorker`
- output console area
- status label and elapsed-time display

### Current assessment

- Code structure is solid.
- The UI reuses the same domain-layer logic instead of duplicating behavior.
- This review did not manually launch the GUI; status is based on source inspection, not a live GUI smoke test.

Status: `Implemented, not manually re-verified in this review`

## 12. Automated Testing Status

## 12.1 Testing framework

- JUnit 5
- Maven Surefire

## 12.2 Notable testing strategy

The test suite includes `testsupport.JavaClassFixtureCompiler`, which uses the JDK compiler API to:

1. write miniature Java source fixtures to a temporary directory
2. compile them during the test run
3. feed the resulting `.class` files into ASM-based linters

This is a strong design choice because it tests the bytecode analyzers against realistic compiled input rather than mocked ASM structures.

## 12.3 Test classes currently present

- `datastorage.ConfigLoaderTest`
- `domain.AdapterPatternLinterTest`
- `domain.BooleanFlagMethodLinterTest`
- `domain.FacadePatternLinterTest`
- `domain.LeastKnowledgePrincipleLinterTest`
- `domain.SingletonPatternLinterTest`
- `domain.StrategyPatternLinterTest`
- `domain.TooManyParametersLinterTest`
- `test.LeastKnowledgePrincipleLinterTest`

## 12.4 Areas directly covered

- config loading
- adapter detection
- boolean-flag detection
- facade detection
- least-knowledge detection
- singleton detection
- strategy detection
- too-many-parameters threshold behavior

## 12.5 Coverage gaps

No dedicated test classes were found for:

- `SnakeLinter`
- `TrailingWhitespaceLinter`
- `UnusedImportLinter`
- `PublicNonFinalFieldLinter`
- `SRPLinter`
- `DecoratorPatternLinter`
- `DesignRiskLinter`
- `PlantUMLGenerator`

## 12.6 Redundancy

There are two `LeastKnowledgePrincipleLinterTest` classes in different packages:

- `domain.LeastKnowledgePrincipleLinterTest`
- `test.LeastKnowledgePrincipleLinterTest`

This duplication is not breaking anything, but it is redundant.

## 13. Example and Demo Assets

The repository includes two kinds of examples:

### `src/main/java/example`

Larger example/demo classes, including:

- `SRPExample`
- `RiskExample`
- `SingletonExample`
- `AdapterExample`
- `DecoratorExample`
- `FacadeExample`
- `UnusedImportExample`
- `LinterMainExample`

### `src/main/java/inlineexamples`

Smaller focused examples, including:

- `BooleanFlagService`
- `CheckoutFacade`
- `GatewayAdapter`
- `LegacyGateway`
- `PaymentTarget`
- `BillingSubsystem`
- `NotificationSubsystem`

These examples improve the project's demo value and make it easier to explain what each detector is looking for.

## 14. Observed Strengths

### 14.1 Hybrid analysis model

Combining source scanning and ASM analysis is the defining strength of this project. It shows a practical understanding that different kinds of code quality problems need different analysis techniques.

### 14.2 Clean extension model

The single `Linter` interface plus the two abstract base classes give the repository a clear path for adding new rules without rewriting orchestration logic.

### 14.3 Good test technique for bytecode analysis

Using the JDK compiler API in tests is a strong engineering choice and one of the best implementation details in the repo.

### 14.4 Good interview breadth

The project gives you solid talking points in:

- Java
- Maven
- ASM
- Swing
- static analysis
- design patterns
- layered architecture
- testing
- configuration management

### 14.5 Runnable artifact

The project packages a `jar-with-dependencies`, which makes it easy to demo without dependency setup friction.

## 15. Known Limitations and Technical Debt

This section is intentionally precise and critical.

## 15.1 Heuristic false positives in `LeastKnowledgePrincipleLinter`

Current behavior is noisy. During test execution, the linter flagged:

- `System.out.println(...)`

as a Law of Demeter violation in the sample fixture. The test still passes because the expected total count was written around current behavior, but the result indicates the heuristic is broader than the comments suggest.

Status: `Real issue`

Impact:

- lowers confidence in reported least-knowledge violations
- may make output look overly noisy in demos

## 15.2 Overlap between adapter and decorator heuristics

During the CLI example run, `CoffeeDecorator` was reported as both:

- a decorator candidate
- an adapter candidate

This indicates `AdapterPatternLinter` is broad enough to classify some decorators as adapters when the class both implements a target type and delegates to a wrapped instance of that type.

Status: `Real issue`

Impact:

- pattern detector results should be described as heuristic candidates, not authoritative classifications

## 15.3 Strategy detector is conservative

`StrategyPatternLinter` only reports candidates when:

- an interface method call returns a non-void value
- the return value appears to be used
- multiple concrete implementors are observed

This reduces false positives, but it also misses classic "void execute()" strategy designs.

Status: `Intentional tradeoff`

Impact:

- lower recall
- safer but incomplete detection

## 15.4 `DesignRiskLinter` scoring scale is inconsistent

The linter labels results as `x/10`, but the implemented weights can sum to `12`.

Observed example:

- `RiskExample` reported `Total Score = 12/10`

Status: `Real issue`

Impact:

- output scale is misleading

## 15.5 Encoding/formatting artifacts in SRP output

The CLI output showed garbled separator characters in the SRP report blocks.

Status: `Real issue`

Likely cause:

- non-ASCII box-drawing characters rendered under a mismatched console encoding

Impact:

- cosmetic
- makes polished demos weaker

## 15.6 Documentation drift in `SRP_LINTER_README.md`

That file says the threshold should be changed by editing `LinterMain.java` directly. The current implementation actually uses:

- `linter.properties`
- `LinterConfig`
- `LinterFactory`

Status: `Outdated documentation`

Impact:

- confusing for new readers

## 15.7 `current.puml` appears stale/inaccurate

The checked-in UML text includes references that do not match current code accurately, such as:

- outdated class details
- naming mismatches
- an `ExampleLinter2` entry

Status: `Stale artifact`

Impact:

- should not be treated as the current source of truth

## 15.8 Uneven test coverage

Some important rules do not yet have dedicated tests, especially:

- `SRPLinter`
- `DesignRiskLinter`
- `DecoratorPatternLinter`
- `PublicNonFinalFieldLinter`
- `PlantUMLGenerator`

Status: `Coverage gap`

Impact:

- changes to these linters carry more regression risk

## 15.9 `SnakeLinter` enforces non-idiomatic Java naming

The project intentionally flags names that are not `snake_case`, including standard Java-style class names like `SingletonExample` and `ComputerFacade`.

Status: `Intentional but unconventional`

Impact:

- this should be framed in interviews as a custom style rule, not a universal Java best practice

## 16. Current Readiness Assessment

| Area | Status | Notes |
|---|---|---|
| Build | Green | Maven build and tests pass |
| CLI execution | Green | Verified end to end |
| GUI implementation | Yellow | Implemented, not manually re-run in this review |
| Core architecture | Green | Clean modular structure |
| Configuration | Green | Implemented and tested |
| Bytecode analysis | Green | ASM integration works |
| Rule accuracy | Yellow | Several heuristics are noisy |
| Automated tests | Yellow/Green | Good foundation, incomplete coverage |
| Documentation | Yellow | Some docs are stale |
| Demo readiness | Green | Good for walkthroughs and interviews |

## 17. Recommended Next Steps

## 17.1 High-priority improvements

1. Tighten `LeastKnowledgePrincipleLinter` to reduce obvious false positives such as `System.out.println(...)`.
2. Refine `AdapterPatternLinter` so decorators are less likely to be classified as adapters.
3. Fix `DesignRiskLinter` output scale so the maximum score matches the advertised range.
4. Replace SRP report separator characters with ASCII-safe formatting.

## 17.2 Testing improvements

1. Add dedicated tests for `SRPLinter`.
2. Add dedicated tests for `DesignRiskLinter`.
3. Add dedicated tests for `DecoratorPatternLinter`.
4. Add dedicated tests for `PublicNonFinalFieldLinter`.
5. Add dedicated tests for `UnusedImportLinter`, `SnakeLinter`, and `TrailingWhitespaceLinter`.
6. Remove or consolidate the duplicate least-knowledge test.

## 17.3 Documentation improvements

1. Update `SRP_LINTER_README.md` to reflect config-driven thresholds.
2. Regenerate or remove stale UML files.
3. Add a short architecture diagram that matches the current layering.
4. Add examples for running individual linters against `target/classes`.

## 17.4 Productization improvements

1. Add CI to run `mvn test` automatically.
2. Add a machine-readable report mode, such as JSON output.
3. Add severity levels for findings.
4. Add per-rule enablement and richer thresholding in the GUI.

## 18. Best Interview Positioning

If this project is discussed in an interview, the most defensible framing is:

- This is a modular Java static-analysis tool.
- It supports both source-level and bytecode-level analysis.
- The most technically interesting part is ASM-based structural analysis and LCOM4/SRP detection.
- The design is extensible because all rules plug into a shared `Linter` contract and are created through a factory.
- The strongest engineering detail is the test harness that compiles Java fixtures dynamically to test bytecode analyzers realistically.

Avoid overselling pattern detection as perfect recognition. The code is better described as:

- "candidate detection based on heuristics"

rather than:

- "formal design-pattern proof"

## 19. Final Status Statement

Current status: `Functional, modular, and credible student static-analysis project with passing tests and strong interview value`

Confidence level in current repository state:

- core architecture: high
- build stability: high
- CLI functionality: high
- GUI existence/structure: medium-high
- rule precision across all detectors: medium
- overall maintainability: medium-high

The project is in good shape to discuss, demo, and extend, with the main caveat that several detectors are heuristic and some documentation/test coverage still need cleanup.
