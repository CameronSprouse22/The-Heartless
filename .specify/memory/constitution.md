<!--
SYNC IMPACT REPORT - 2026-03-13

VERSION CHANGE: NONE → 1.0.0
BUMP RATIONALE: Initial constitution establishment for The Heartless project

PRINCIPLES DEFINED:
  + I. Testability First - Every method must be independently testable
  + II. Method Simplicity - Single responsibility, clear inputs/outputs
  + III. Separation of Concerns - Game logic separated from presentation
  + IV. Explicit Over Implicit - Clear method contracts and naming

SECTIONS ADDED:
  + Technology Stack - Java, HTML, JUnit requirements
  + Development Standards - Code quality, documentation, testing gates

TEMPLATES REQUIRING UPDATES:
  ✅ plan-template.md - Constitution Check section aligns with new principles
  ✅ spec-template.md - Requirements structure supports testable scenarios
  ✅ tasks-template.md - Task categorization supports principle-driven organization

FOLLOW-UP TODOS: None - all placeholders resolved
-->

# The Heartless Constitution

## Core Principles

### I. Testability First (NON-NEGOTIABLE)

Every method MUST be independently testable without complex setup or mocking.

- Methods MUST have clear, predictable inputs and outputs
- Avoid static state and hidden dependencies
- Use dependency injection for external collaborators
- Each method MUST have corresponding unit tests with ≥80% code coverage
- Tests MUST be written before implementation (Test-Driven Development)

**Rationale**: Testability ensures code reliability, enables confident refactoring, and serves
as executable documentation. For a game, testable methods make it easy to verify game logic,
rules, scoring, and state transitions independently from the UI.

### II. Method Simplicity

Every method MUST do one thing and do it well (Single Responsibility Principle).

- Methods SHOULD be ≤20 lines of code; >30 lines requires justification
- Maximum cyclomatic complexity of 5 per method
- No nested conditionals deeper than 2 levels
- Extract complex logic into smaller, named helper methods
- Avoid method chaining that obscures readability

**Rationale**: Simple methods are easier to test, understand, debug, and maintain. In game
development, simple building blocks combine to create complex emergent behavior without
creating a maintenance nightmare.

### III. Separation of Concerns

Game logic MUST be completely independent from presentation (HTML/UI) layer.

- Core game engine classes contain NO HTML, DOM manipulation, or UI references
- UI components contain NO game logic—they only render state and forward user actions
- Game state represented as pure Java objects (POJOs)
- Communication between layers via clear interfaces/contracts
- Game logic MUST be runnable and testable without any UI present

**Rationale**: Separating logic from presentation enables independent testing of game rules,
allows UI changes without touching logic, and makes the codebase portable (e.g., could add
different frontends without rewriting the engine).

### IV. Explicit Over Implicit

Method contracts, behaviors, and assumptions MUST be clear and explicit.

- Use descriptive method names that reveal intent (e.g., `calculateDamageAfterArmor()` not
  `process()`)
- Document preconditions, postconditions, and side effects with Javadoc
- Make expected exceptions explicit in method signatures or documentation
- Avoid "magic numbers"—use named constants
- Return types SHOULD be specific (avoid returning `Object` or overly generic types)

**Rationale**: Explicit code reduces cognitive load, prevents subtle bugs, and makes the
codebase accessible to new contributors. In games, where rules can be complex, clarity
prevents misunderstandings about how the game actually works.

## Technology Stack

**Language**: Java 11+ (LTS version)

**Build Tool**: Maven or Gradle (specify in project setup)

**Testing Framework**: JUnit 5 (Jupiter) for unit tests

**HTML/Frontend**: Pure HTML5/CSS3/JavaScript (no frameworks unless justified)

**Version Control**: Git with conventional commits (e.g., `feat:`, `fix:`, `test:`)

**Code Quality**: Checkstyle or SpotBugs for static analysis (enforced in CI/CD if applicable)

**Documentation**: Javadoc for all public APIs; README for project overview and setup

**Constraints**:
- No runtime dependencies beyond standard Java library unless justified
- Frontend MUST work in modern browsers (Chrome, Firefox, Edge, Safari) without polyfills
- Game MUST be playable offline (no required external API calls)

## Development Standards

### Code Quality Gates

Before any code is considered complete, it MUST pass:

1. **Compilation**: No compiler errors or warnings
2. **Tests**: All unit tests pass with ≥80% code coverage
3. **Static Analysis**: No critical issues from Checkstyle/SpotBugs
4. **Manual Review**: Code reviewed for adherence to principles I-IV

### Documentation Requirements

- Every public class and method MUST have Javadoc comments
- Complex algorithms or business rules MUST include inline comments explaining "why"
- README MUST be updated if new features change setup or gameplay
- Test cases SHOULD serve as usage examples

### Testing Standards

- Unit tests MUST be fast (entire suite runs in <10 seconds)
- Test names SHOULD describe the scenario (e.g., `playerTakesDamage_healthReducesCorrectly`)
- Use Arrange-Act-Assert pattern for test structure
- Mock external dependencies; verify game logic in isolation
- Edge cases MUST be tested: boundary values, null inputs, invalid states

## Governance

This constitution supersedes all other development practices. All design decisions, code
reviews, and feature implementations MUST align with the core principles defined herein.

**Amendment Procedure**:
- Proposed changes MUST be documented with rationale and impact analysis
- Version increments follow semantic versioning:
  - **MAJOR**: Principle removal, redefinition, or backward-incompatible governance changes
  - **MINOR**: New principle added or section materially expanded
  - **PATCH**: Clarifications, wording improvements, typo fixes
- Amendments require updating this document and propagating changes to all dependent templates
  in `.specify/templates/`

**Compliance**:
- All feature specifications MUST reference applicable principles
- Implementation plans MUST include a "Constitution Check" section
- Code that violates principles without documented justification will be rejected
- Complexity that contradicts Method Simplicity (Principle II) MUST be justified in
  implementation plans

**Runtime Guidance**: For day-to-day development decisions not covered by this constitution,
refer to standard Java best practices and the Clean Code principles.

**Version**: 1.0.0 | **Ratified**: 2026-03-13 | **Last Amended**: 2026-03-13
