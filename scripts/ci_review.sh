#!/usr/bin/env bash
set -euo pipefail
WORKDIR=$(pwd)
REPORT_DIR="$WORKDIR/.issues"
mkdir -p "$REPORT_DIR"
TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
REPORT_FILE="$REPORT_DIR/issue_$TIMESTAMP.md"
REMOTE_URL=$(git remote get-url origin 2>/dev/null || true)

echo "# Automated review report — $TIMESTAMP" > "$REPORT_FILE"
echo "Repository: ${REMOTE_URL:-local}" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

FOUND=0

# 1) Quick grep checks: TODOs/FIXMEs
echo "## Quick code smells" >> "$REPORT_FILE"
if grep -R --line-number -E "TODO|FIXME" --exclude-dir=.git --exclude-dir=.venv . | sed -n '1,200p' >> "$REPORT_FILE"; then
    FOUND=1
else
    echo "No TODO/FIXME markers found." >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"

# 2) Detect project type and run light checks if possible
if [ -f pom.xml ]; then
  echo "## Maven project checks" >> "$REPORT_FILE"
  if command -v mvn >/dev/null 2>&1; then
    echo "Running: mvn -DskipTests=true -q package (quick)" >> "$REPORT_FILE"
    if mvn -DskipTests=true -q package 2>> /tmp/ci_review_mvn_err.log; then
      echo "Maven package: success" >> "$REPORT_FILE"
    else
      echo "Maven package: failed (see logs)" >> "$REPORT_FILE"
      sed -n '1,200p' /tmp/ci_review_mvn_err.log >> "$REPORT_FILE"
      FOUND=1
    fi
    # Try spotbugs/checkstyle if configured
    if mvn help:effective-pom | grep -i spotbugs >/dev/null 2>&1; then
      echo "Running spotbugs (if available)" >> "$REPORT_FILE"
      mvn -q spotbugs:check 2>> /tmp/ci_review_spot_err.log || true
      sed -n '1,200p' /tmp/ci_review_spot_err.log >> "$REPORT_FILE" || true
    fi
  else
    echo "Maven not installed; skipping deep checks." >> "$REPORT_FILE"
  fi
elif ls *.gradle* >/dev/null 2>&1; then
  echo "## Gradle project checks" >> "$REPORT_FILE"
  if command -v gradle >/dev/null 2>&1 || command -v ./gradlew >/dev/null 2>&1; then
    echo "Running: gradle check (quick)" >> "$REPORT_FILE"
    if command -v gradle >/dev/null 2>&1; then
      gradle check --no-daemon --console=plain >> /tmp/ci_review_gradle.log 2>&1 || true
    else
      ./gradlew check --console=plain >> /tmp/ci_review_gradle.log 2>&1 || true
    fi
    sed -n '1,200p' /tmp/ci_review_gradle.log >> "$REPORT_FILE" || true
  else
    echo "Gradle not installed; skipping deep checks." >> "$REPORT_FILE"
  fi
else
  echo "## No Maven/Gradle build detected — skipping build checks" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"

# 3) Simple Java patterns: large files
echo "## Simple heuristics" >> "$REPORT_FILE"
find . -name '*.java' -type f -exec wc -l {} + | sort -n -r | head -n 10 >> "$REPORT_FILE" || true

echo "" >> "$REPORT_FILE"

# 4) If FOUND, create a local issue file and optionally call GitHub API (if GITHUB_TOKEN present)
if [ $FOUND -ne 0 ]; then
  ISSUE_TITLE="Automated review: potential improvements ($TIMESTAMP)"
  echo "---" > "$REPORT_FILE.tmp"
  echo "title: $ISSUE_TITLE" >> "$REPORT_FILE.tmp"
  echo "---" >> "$REPORT_FILE.tmp"
  cat "$REPORT_FILE" >> "$REPORT_FILE.tmp"
  mv "$REPORT_FILE.tmp" "$REPORT_FILE"
  echo "[AUTOMATED] Review produced findings. Report saved to $REPORT_FILE"
  # If GITHUB_TOKEN is present and remote is GitHub, try to open an issue
  if [ -n "${GITHUB_TOKEN-}" ] && echo "$REMOTE_URL" | grep -q "github.com"; then
    OWNER_REPO=$(echo "$REMOTE_URL" | sed -e 's#.*github.com[:/]##' -e 's/\.git$//')
    echo "Creating GitHub issue on $OWNER_REPO"
    API_URL="https://api.github.com/repos/$OWNER_REPO/issues"
    # Build JSON payload using python to avoid jq portability issues
    python3 - <<PY > /tmp/gh_issue.out
import json
with open(r"""$REPORT_FILE""","r",encoding="utf-8") as f:
    body = f.read()
print(json.dumps({"title": "$ISSUE_TITLE", "body": body}))
PY
    HTTP_STATUS=$(curl -s -o /tmp/gh_response.out -w "%{http_code}" -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/json" -d @/tmp/gh_issue.out "$API_URL" || echo "000")
    if [ "$HTTP_STATUS" = "201" ]; then
      if grep -q '"html_url"' /tmp/gh_response.out 2>/dev/null; then
        html=$(python3 -c "import json
print(json.load(open('/tmp/gh_response.out'))['html_url'])")
        echo "GitHub issue created: $html" >> "$REPORT_FILE"
      else
        echo "GitHub issue created but response parsing failed." >> "$REPORT_FILE"
      fi
    else
      echo "Failed to create GitHub issue. HTTP status: $HTTP_STATUS" >> "$REPORT_FILE"
      sed -n '1,200p' /tmp/gh_response.out >> "$REPORT_FILE"
    fi
  else
    echo "No GITHUB_TOKEN or remote not GitHub — saved local report only." >> "$REPORT_FILE"
  fi
else
  rm -f "$REPORT_FILE"
  echo "No issues detected by automated checks."
fi

exit 0
