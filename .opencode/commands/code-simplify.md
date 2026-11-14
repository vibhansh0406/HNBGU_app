description: "Run the code-simplification skill workflow for $ARGUMENTS."
agent: build
subtask: true
---
Call skill({ name: "code-simplification" }) and follow its SKILL.md instructions to handle $ARGUMENTS.
