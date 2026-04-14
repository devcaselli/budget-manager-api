---
name: User profile
description: Senior-minded Java/Spring developer who communicates in Portuguese and prioritizes clean architecture, SOLID, Big-O analysis, and code simplification
type: user
---

The user is the author/maintainer of the budget-manager-api project. Observed characteristics:

- **Communicates in Portuguese (pt-BR).** Respond in Portuguese unless they explicitly switch.
- **Strongly values clean architecture and SOLID.** Expects reviews to flag layering violations and anti-patterns explicitly.
- **Wants Big-O analysis** as a first-class part of code review output — both time and space complexity.
- **Prefers fewer lines when it doesn't hurt readability.** When asked to "diminuir o código," look for idiomatic reductions (replace filter-in-loop with groupingBy, inline trivial helpers, remove dead imports/statics).
- **Handles their own builds.** Explicitly said "não precise rodar mvn clean... eu rodo depois" — do NOT run Maven/build commands after edits. They verify manually.
- **Accepts review-driven iteration.** Commit history shows multiple rounds titled "Fixing code review points 01–10" — they act on reviewer feedback rather than pushing back defensively.

**How to apply:** Deliver reviews in the standard format (Summary / Critical / Major / Performance / Minor), always include Big-O, lean toward concise refactored examples, and leave build verification to them.
