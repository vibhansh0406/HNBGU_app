description: "Run the planning-and-task-breakdown skill workflow for $ARGUMENTS."
agent: build
subtask: true
---
Call skill({ name: "planning-and-task-breakdown" }) and follow its SKILL.md instructions to handle $ARGUMENTS.
