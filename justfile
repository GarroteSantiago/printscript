# Display the list of available commands by just typing 'just'
deafult:
    @just --list

# --- DEVELOPMENT & EXECUTION ---

# Compile the entire project quickly (skipping tests and analysis)
build:
    ./gradlew assemble

# Run the CLI application (e.g., just run --args="main.ps")
run args:
    ./gradlew :cli:run --args="{{args}}"

# Run the interactive REPL (e.g., just repl, or just repl 1.1 for v1.1)
repl version="":
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -z "{{version}}" ]; then
        ./gradlew :repl:run --console=plain -q
    else
        ./gradlew :repl:run --console=plain -q --args="{{version}}"
    fi

# Usage: Send a file path relative to the repo root to interpret with the cli app
pisp-interpret file:
    just run "execute --source=/home/garro/dev/faculty/ingsis/printscript/{{ file }} --version=1.0"

# Usage: Send a file path relative to the repo root to analyze with the cli app
pisp-analyze file config:
    just run "analyze --version=1.0 --source=/home/garro/dev/faculty/ingsis/printscript/{{ file }} --config=/home/garro/dev/faculty/ingsis/printscript/{{ config }}"

# Usage: Send a file path relative to the repo root to format with the cli app
pisp-format file:
    just run "format --version=1.0 --source=/home/garro/dev/faculty/ingsis/printscript/{{ file }}"

# Compile main and test sources across every module (warms the Gradle build cache ahead of
# checkstyle/pmd/test, which each need compiled classes for their own runs)
compile:
    ./gradlew compileJava compileTestJava

# --- CODE QUALITY (LINT & FORMAT) ---

# Automatically format all code using Google Java Format
format:
    ./gradlew spotlessApply

# Run Checkstyle, PMD, and Spotless without running tests (Fast)
lint:
    ./gradlew spotlessCheck checkstyleMain pmdMain

# Check formatting without modifying files
format-check:
    ./gradlew spotlessCheck

# Run Checkstyle on main and test sources
checkstyle:
    ./gradlew checkstyleMain checkstyleTest

# Run PMD on main and test sources
pmd:
    ./gradlew pmdMain pmdTest

# --- TESTING & COVERAGE ---

# Run all tests cleanly from scratch (Bypasses caching)
test:
    ./gradlew clean test

# Run tests, generate the visual coverage report (JaCoCo), and fail if coverage is under 80%
coverage:
    ./gradlew test jacocoTestReport jacocoTestCoverageVerification
    @echo "Coverage report generated inside each module's build directory."
    # For Linux: xdg-open cli/build/reports/jacoco/test/html/index.html
    # For macOS: open cli/build/reports/jacoco/test/html/index.html

# --- CONTINUOUS INTEGRATION / VERIFICATION ---

# Run every CI quality tool (formatting, style, static analysis, tests) without a clean rebuild
# Each check runs as its own gradlew invocation so CI can run them as separate, fail-fast steps.
validate: format-check checkstyle pmd coverage

# Run EVERYTHING (Compiles, tests, verifies style, and checks quality rules)
# Run this command right before pushing your code to the faculty repository!
check:
    ./gradlew clean check jacocoTestReport

# --- GIT HOOKS ---

# Point Git at the versioned hooks in .githooks/ (run once per clone)
install-hooks:
    git config core.hooksPath .githooks
    chmod +x .githooks/pre-commit .githooks/pre-push
    @echo "Git hooks installed: pre-commit (format+checkstyle), pre-push (full validate)."

# --- MAINTENANCE ---

# Delete all generated build/ directories
clean:
    ./gradlew clean
