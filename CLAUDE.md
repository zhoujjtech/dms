# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an OpenSpec project - a spec-driven development workflow that uses structured artifacts (proposal, specs, design, tasks) to guide implementation. The project is initialized with the `spec-driven` schema, which follows this artifact sequence:

```
proposal → specs → design → tasks → implementation → archive
```

## OpenSpec Commands

### Core OpenSpec CLI Commands

```bash
# Create a new change
openspec new change <change-name>

# Check status of a specific change
openspec status --change <change-name>
openspec status --change <change-name> --json

# Get instructions for creating a specific artifact
openspec instructions <artifact-id> --change <change-name>
openspec instructions <artifact-id> --change <change-name> --json

# List all changes
openspec list --json

# Check available workflow schemas
openspec schemas --json

# Get apply instructions (for implementation)
openspec instructions apply --change <change-name> --json
```

### Available Schemas

The project uses the `spec-driven` schema by default. It defines this artifact sequence:
1. `proposal` - The "why" and high-level "what"
2. `specs` - Detailed requirements in testable form (WHEN/THEN/AND scenarios)
3. `design` - Technical decisions and approach
4. `tasks` - Checkboxed implementation tasks

All artifacts must be completed before implementation can begin.

## Custom Commands (OPSX Workflow)

The project defines `/opsx:` commands that wrap the OpenSpec CLI with AI-driven workflows:

| Command | Description |
|---------|-------------|
| `/opsx:explore <name>` | Think through problems before/during work (no code changes) |
| `/opsx:new <name>` | Start a new change, step through artifacts one at a time |
| `/opsx:ff <name>` | Fast-forward: create all artifacts needed for implementation in one go |
| `/opsx:continue <name>` | Continue an existing change, create the next artifact |
| `/opsx:apply <name>` | Implement tasks from a change |
| `/opsx:verify <name>` | Verify implementation matches change artifacts |
| `/opsx:sync <name>` | Sync delta specs from a change to main specs |
| `/opsx:archive <name>` | Archive a completed change |
| `/opsx:bulk-archive` | Archive multiple completed changes at once |
| `/opsx:onboard` | Guided walkthrough of a complete workflow cycle |

## Directory Structure

```
openspec/
├── config.yaml              # OpenSpec configuration (schema: spec-driven)
├── changes/                 # Active changes
│   ├── <change-name>/       # Change directory
│   │   ├── .openspec.yaml   # Change metadata
│   │   ├── proposal.md      # Why + high-level what
│   │   ├── design.md        # Technical decisions
│   │   ├── specs/           # Detailed requirements
│   │   │   └── <capability>/
│   │   │       └── spec.md  # Delta specs (ADDED/MODIFIED/REMOVED/RENAMED)
│   │   └── tasks.md         # Implementation checklist
│   └── archive/             # Archived changes (YYYY-MM-DD-<name>)
└── specs/                   # Main specs (target for delta spec syncs)
    └── <capability>/
        └── spec.md

.claude/
├── skills/                  # OpenSpec skill definitions
│   └── openspec-*.md
└── commands/                # OPSX command definitions
    └── opsx/
        └── *.md
```

## Key Workflow Concepts

### Delta Specs

Delta specs in `openspec/changes/<name>/specs/<capability>/spec.md` follow this format:

```markdown
## ADDED Requirements
### Requirement: <name>
<description>

#### Scenario: <name>
- **WHEN** <condition>
- **THEN** <expected outcome>

## MODIFIED Requirements
### Requirement: <name>
#### Scenario: <new scenario>
...

## REMOVED Requirements
### Requirement: <name>

## RENAMED Requirements
- FROM: `### Requirement: <old>`
- TO: `### Requirement: <new>`
```

Delta specs are synced to main specs in `openspec/specs/` using `/opsx:sync`. The sync is agent-driven - it intelligently merges changes (e.g., adding a scenario without copying entire requirement).

### Tasks Format

Tasks in `tasks.md` use checkboxes:
```markdown
- [ ] Incomplete task
- [x] Completed task
```

### Artifact Status Tracking

Use `openspec status --change <name> --json` to understand:
- Which artifacts are `done`, `ready`, or `blocked`
- The artifact dependency graph
- Schema being used
- If implementation can begin (`applyRequires` satisfied)

### Change Lifecycle

1. **New** - `openspec new change <name>` creates the change directory
2. **Artifact creation** - `/opsx:new` (step-by-step) or `/opsx:ff` (all at once)
3. **Implementation** - `/opsx:apply` to execute tasks
4. **Verification** - `/opsx:verify` (optional, recommended)
5. **Archive** - `/opsx:archive` moves to `openspec/changes/archive/YYYY-MM-DD-<name>/`

### Spec Sync Before Archive

When archiving a change with delta specs, the system prompts to sync those specs to the main specs directory. This is recommended to keep main specs updated with implemented requirements.

## Change Selection Best Practices

When multiple changes exist, use `openspec list --json` to see:
- Change names
- Schema used
- Status (task completion)
- `lastModified` timestamp

The most recently modified change is typically the one to continue. When prompting for change selection with `AskUserQuestion`, mark it as "(Recommended)".

## Explore Mode

`/opsx:explore` is a thinking partner mode for investigating problems before implementing. Key principles:
- **Curious, not prescriptive** - Ask questions naturally
- **Visual** - Use ASCII diagrams for architecture/data flows
- **Grounded** - Explore the actual codebase when relevant
- **Don't implement** - Reading/searching is fine, but never write code

Explore mode CAN create OpenSpec artifacts (capturing thinking), but cannot implement application code.

## Fluid Workflow

OpenSpec supports fluid, non-linear workflows:
- Implementation (`/opsx:apply`) can be invoked before all artifacts are complete (if tasks exist)
- Artifacts can be updated during implementation if design issues emerge
- Changes can be interleaved with exploration using `/opsx:explore <name>` mid-workflow
- Spec sync (`/opsx:sync`) can happen anytime without archiving
- Verification (`/opsx:verify`) ensures implementation matches artifacts before archive

## Important Constraints

- Always read `openspec status --json` before starting work to understand the schema and artifact graph
- The `context` and `rules` fields from `openspec instructions` are constraints for AI, NOT content for the file - never copy them into artifacts
- Use the `template` from `openspec instructions` as the structure for artifact content
- Archive directory names use `YYYY-MM-DD-<name>` format
- Changes are never deleted - they move to archive preserving decision history
