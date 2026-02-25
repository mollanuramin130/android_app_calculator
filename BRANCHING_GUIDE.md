# Branching Guide – Sunset Coral Calculator

Use this workflow for development and releases.

---

## Branch structure

```
main                          ← production / release (merge only when ready to release)
  │
  └── development/v1.0.0      ← integration branch for version 1.0.0 (final code here)
        │
        ├── development/v1.0.0_dev1   ← your work (merge here after review)
        ├── development/v1.0.0_dev2   ← next iteration
        ├── development/v1.0.0_dev3   ← and so on...
        └── ...
```

- **main** – Only gets code when you are ready to **release** (e.g. from `development/v1.0.0`).
- **development/v1.0.0** – Where all v1.0.0 work is integrated. Dev branches merge **here**, not into main.
- **development/v1.0.0_dev1, _dev2, …** – Your daily work branches. After review, merge into `development/v1.0.0`.

---

## First-time upload (push all branches to GitHub)

Branches are created locally. Run these commands **once** in Terminal to upload everything (you will be asked for your GitHub username and password/token):

```bash
cd /Users/nuramin/Desktop/Android_App_Project/Custom_Calculator_App

# Push main (production)
git push -u origin main

# Push integration branch for v1.0.0
git push -u origin development/v1.0.0

# Push your first dev branch (you are currently on development/v1.0.0_dev1)
git push -u origin development/v1.0.0_dev1
```

If GitHub asks for a password, use a **Personal Access Token**: [GitHub → Settings → Developer settings → Personal access tokens](https://github.com/settings/tokens) (scope: `repo`).

---

## Daily workflow

### 1. Start a new development branch (e.g. dev1, dev2, …)

Always branch **from** `development/v1.0.0` so you have the latest integrated code:

```bash
# Update your local integration branch
git checkout development/v1.0.0
git pull origin development/v1.0.0

# Create your dev branch (use _dev1, _dev2, _dev3, ...)
git checkout -b development/v1.0.0_dev1

# Work on your code...
```

Use **development/v1.0.0_dev2**, **development/v1.0.0_dev3**, etc. for the next rounds of work.

### 2. Commit and push your dev branch

```bash
git add .
git commit -m "Describe what you did"
git push -u origin development/v1.0.0_dev1
```

### 3. After review: merge into development/v1.0.0 (not main)

Merge the dev branch **into** `development/v1.0.0`:

```bash
# Switch to the integration branch and update it
git checkout development/v1.0.0
git pull origin development/v1.0.0

# Merge your dev branch (e.g. dev1) into it
git merge development/v1.0.0_dev1 -m "Merge development/v1.0.0_dev1 into development/v1.0.0"

# Push the updated integration branch
git push origin development/v1.0.0
```

Optional: delete the dev branch after merge (local and remote):

```bash
git branch -d development/v1.0.0_dev1
git push origin --delete development/v1.0.0_dev1
```

### 4. When v1.0.0 is ready for release: merge into main

Only when everything is tested and you want to release:

```bash
git checkout main
git pull origin main
git merge development/v1.0.0 -m "Release v1.0.0"
git push origin main
```

---

## Starting the next version (e.g. v1.1.0)

When you start work on a new version:

```bash
git checkout main
git pull origin main
git checkout -b development/v1.1.0
git push -u origin development/v1.1.0
```

Then create dev branches from **development/v1.1.0** (e.g. `development/v1.1.0_dev1`, `development/v1.1.0_dev2`, …) and merge them into `development/v1.1.0`, same as above. When ready, merge `development/v1.1.0` into `main`.

---

## Quick reference

| What you want to do              | Branch from              | Merge into                |
|----------------------------------|--------------------------|---------------------------|
| Start dev work (dev1, dev2, …)   | development/v1.0.0       | —                         |
| After review                     | —                        | development/v1.0.0        |
| Release v1.0.0                   | —                        | main (from development/v1.0.0) |
| Start v1.1.0                     | main                     | — (create development/v1.1.0) |

---

## Summary

1. **main** = production; only merge from an integration branch when releasing.
2. **development/v1.0.0** = integration for v1.0.0; all _dev branches merge here.
3. **development/v1.0.0_dev1, _dev2, …** = your work branches; merge into `development/v1.0.0` after review, never directly into main.
