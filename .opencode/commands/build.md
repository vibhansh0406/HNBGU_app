description: "Run the incremental-implementation skill workflow for $ARGUMENTS."
agent: build
subtask: true
---
Call skill({ name: "incremental-implementation" }) and follow its SKILL.md instructions to handle $ARGUMENTS.
