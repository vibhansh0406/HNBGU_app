description: "Run the code-review-and-quality skill workflow for $ARGUMENTS."
agent: build
subtask: true
---
Call skill({ name: "code-review-and-quality" }) and follow its SKILL.md instructions to handle $ARGUMENTS.
