#!/usr/bin/env bash
set -euo pipefail

# 1. (Optional) Explicit build step—CLI does this internally on publish,
#    but you can dump bytecode as Base64 for inspection or further automation.
sui move build \
  --dump-bytecode-as-base64 \
  --ignore-chain   # bypasses the need for client.yaml when dumping :contentReference[oaicite:0]{index=0}

# 2. Publish and capture JSON output
OUTPUT=$(sui client publish \
  --gas-budget 5000000 \
  --gas-price 1000 \
  --json)           # machine‑readable format for scripting :contentReference[oaicite:1]{index=1}

# 3. Extract the PackageID
PACKAGE_ID=$(echo "$OUTPUT" | jq -r '
  .effects.objectChanges[]
  | select(.type=="Published")
  | .packageId
')

echo "✅ Published package: $PACKAGE_ID"

# 4. Export for CI (e.g. GitHub Actions)
echo "PACKAGE_ID=$PACKAGE_ID" >> "$GITHUB_ENV"
