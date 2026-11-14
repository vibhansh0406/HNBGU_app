description: "Run the shipping-and-launch skill workflow for $ARGUMENTS."
agent: build
subtask: true
---
Call skill({ name: "shipping-and-launch" }) and follow its SKILL.md instructions to handle $ARGUMENTS.
