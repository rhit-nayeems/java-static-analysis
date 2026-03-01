# Java Linter Project

A Java linter that combines source-based and bytecode-based checks (ASM) to detect style issues, principle violations, and design pattern signals.

## Requirements
- JDK 11+
- Maven 3+

## Build
```bash
mvn clean package
```

## Run (CLI)
```bash
java -jar target/LinterProject-1.0-rc3-jar-with-dependencies.jar
```

## Run (GUI)
```bash
java -jar target/LinterProject-1.0-rc3-jar-with-dependencies.jar --gui
```

You can also use:
```bash
java -cp target/classes presentation.LinterGuiMain
```

## Configuration keys (`linter.properties`)
Default config file: `linter.properties`

- `enabled_linters=all` or comma-separated linter class names
- `too_many_parameters_limit=5`
- `srp_lcom_threshold=2`

## GUI workflow
1. Pick a linter in `Linter to run`.
2. Pick compatible files (the file dropdown is filtered by the selected linter type).
3. Optionally change config values in the config panel:
   - `enabled_linters=all` checkbox
   - `enabled_linters` comma-separated text field
   - `too_many_parameters_limit`
   - `srp_lcom_threshold`
4. Use `Load` and `Save` to import/export `linter.properties`.
5. Click `Run Selected Linter`.

Notes:
- The linter dropdown always shows all registered linters.
- `enabled_linters` is still editable and saved to config for CLI/consistency.
- Class-file linters need `.class` files (for example `target/classes`).
- Source linters need `.java` files.