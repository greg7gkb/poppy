#!/usr/bin/env bash
# Serve the Poppy repo so this example can resolve its relative imports
# (../../packages/client-web/dist/...) regardless of where you invoke this
# script from. Defaults to port 8000; pass a number to override.
#
# Usage: ./serve.sh         # http://localhost:8000/examples/web/
#        ./serve.sh 9000    # http://localhost:9000/examples/web/

set -euo pipefail
PORT="${1:-8000}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
echo "Serving $REPO_ROOT"
echo "Open http://localhost:$PORT/examples/web/"
exec python3 -m http.server "$PORT" --directory "$REPO_ROOT"
