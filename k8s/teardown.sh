#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-country-info}"

echo "==> Removing Kubernetes resources"
kubectl delete -f "$(dirname "$0")" --ignore-not-found

echo "==> Teardown complete for namespace ${NAMESPACE}"
