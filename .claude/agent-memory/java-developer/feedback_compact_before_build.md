---
name: compact-before-build
description: Before starting each new build/implementation, analyze context cost and warn user if compact is advisable — show what would be retained and discarded.
metadata:
  type: feedback
---

Before each new construction task, analyze context load and proactively warn if a `/compact` would save tokens. Show the user:
- What turns/files are bloating context (re-read files, large tool results)
- What would be retained in the compact summary
- What would be discarded
- Estimated token savings

**Why:** User wants to control context size proactively, not reactively. They asked for this after needing to request compact multiple times in the same session.

**How to apply:** At the start of every new C-*, N-*, or P-* security item implementation, or any medium/large task, surface the compact recommendation with data before writing any code.
