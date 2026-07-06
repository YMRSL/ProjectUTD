# GitHub Setup

The machine has Git installed and Git LFS available, but GitHub CLI (`gh`) was not found during the initial check.

## Option A: Connect to an Existing GitHub Repository

Use this when you already created an empty GitHub repo in the browser.

```powershell
cd D:\MC\ProjectUTD
git remote add origin https://github.com/<OWNER>/<REPO>.git
git branch -M main
git push -u origin main
```

Replace `<OWNER>/<REPO>` with your actual GitHub repository path.

## Option B: Install GitHub CLI Later

After installing `gh`, authenticate and create/push a private repo:

```powershell
gh auth login
cd D:\MC\ProjectUTD
gh repo create <OWNER>/<REPO> --private --source . --remote origin --push
```

## First Commit Recommendation

Commit only the repository-management files first:

```powershell
git add README.md .gitignore .gitattributes docs/
git commit -m "Initialize ProjectUTD repository management"
```

Then import real source directories in small reviewed batches, for example:

```powershell
git add SocialWill/Picasso
git status --short
git commit -m "Add Picasso tooling"
```

Before adding any large or binary-heavy directory, run:

```powershell
git status --short
git add --dry-run <path>
```
