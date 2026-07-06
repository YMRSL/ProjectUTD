# GitHub Setup

The machine has Git installed and Git LFS available, but GitHub CLI (`gh`) was not found during the initial check.

## Option A: Create an Empty Repository in the Browser

1. Open <https://github.com/new>.
2. Repository owner: `YMRSL`.
3. Repository name: `ProjectUTD` or another short project name.
4. Visibility: choose `Private` unless you intentionally want the project public.
5. Do not enable `Add a README file`.
6. Do not enable `.gitignore`.
7. Do not choose a license yet.
8. Click `Create repository`.

Then connect this local repository to the empty GitHub repository:

```powershell
cd D:\MC\ProjectUTD
git remote add origin https://github.com/YMRSL/<REPO>.git
git branch -M main
git push -u origin main
```

Replace `<REPO>` with the repository name you created, for example `ProjectUTD`.

## Option B: Install GitHub CLI Later

The first automated install attempt for `gh` did not complete on this machine. If GitHub CLI is installed later, authenticate and create/push a private repo:

```powershell
gh auth login
cd D:\MC\ProjectUTD
gh repo create YMRSL/<REPO> --private --source . --remote origin --push
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
