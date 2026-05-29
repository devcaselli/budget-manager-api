---
name: feedback-delegation-strategy
description: When to delegate to java-minor-worker vs. write inline — token efficiency rule
metadata:
  type: feedback
---

Prefer writing files inline (Write/Edit tools) over delegating to java-minor-worker when the required template files have already been read in the current context.

**Why:** java-minor-worker starts cold — it re-reads every template from scratch, duplicating reads already done in main context. T1 (4 files) cost 123k tokens; T2 (1 file, 20 lines) cost 104k tokens. Both were wasteful because templates were already available.

**How to apply:**
- Already read the reference templates? → Write inline with Write/Edit.
- Templates not yet read AND scope is 1-2 files? → Still consider reading them here first, then writing inline.
- Delegate to java-minor-worker only when: scope is genuinely ambiguous, risk of cross-file side effects is high, or you have NOT read the relevant context and don't want to load it into main context.
