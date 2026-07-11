# ProjectUTD

ProjectUTD is a Minecraft project workspace containing mod source experiments, migration notes, NPC scripts, tooling, and supporting design documents.

## Project Operations

The project-level sprint status, priorities, blockers, and acceptance criteria are maintained in [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md). Module architecture documents remain authoritative for technical design.

## Current Repository Scope

This repository is prepared as a source-management root, not as a full Minecraft client backup. The first Git pass should focus on:

- `UtilWeDie-Neo-1.21.1/ModsSourceCode/` hand-maintained ports and patches
- `UtilWeDie-Neo-1.21.1/npc/` NPC scripts and resource-pack work
- `SocialWill/Picasso/` Python tooling
- `Code/ItemsReFresh/` and other code that is actually maintained locally
- `Migration-1.21.1/` migration notes

The following are intentionally excluded until reviewed:

- Minecraft launchers, PCL folders, runtime versions, logs, crash reports
- downloaded archives and packaged mod/client files
- build output, decompiled output, extracted jars, and temporary inspection folders
- large map/world/client backup directories

See [docs/repository-map.md](docs/repository-map.md) for the initial inventory and [docs/github-setup.md](docs/github-setup.md) for GitHub connection steps.

## First Git Workflow

```powershell
git status --short
git add README.md .gitignore .gitattributes docs/
git commit -m "Initialize ProjectUTD repository management"
```

After the first management commit, import project source areas in small batches and review each batch before committing.
