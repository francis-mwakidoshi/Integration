#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
IMAGE_NAME="${IMAGE_NAME:-country-info-service:1.0.0}"
NAMESPACE="${NAMESPACE:-country-info}"

echo "==> Building Docker image: ${IMAGE_NAME}"
docker build -t "${IMAGE_NAME}" "${PROJECT_DIR}"

echo "==> Applying Kubernetes manifests"
kubectl apply -f "${SCRIPT_DIR}/namespace.yaml"
kubectl apply -f "${SCRIPT_DIR}/mysql-secret.yaml"
kubectl apply -f "${SCRIPT_DIR}/mysql-deployment.yaml"
kubectl apply -f "${SCRIPT_DIR}/mysql-service.yaml"
kubectl apply -f "${SCRIPT_DIR}/configmap.yaml"
kubectl apply -f "${SCRIPT_DIR}/app-secret.yaml"
kubectl apply -f "${SCRIPT_DIR}/deployment.yaml"
kubectl apply -f "${SCRIPT_DIR}/service.yaml"
kubectl apply -f "${SCRIPT_DIR}/hpa.yaml"

echo "==> Waiting for MySQL to become ready"
kubectl rollout status deployment/mysql -n "${NAMESPACE}" --timeout=180s

echo "==> Waiting for application to become ready"
kubectl rollout status deployment/country-info-service -n "${NAMESPACE}" --timeout=300s

echo "==> Deployment complete"
kubectl get pods,svc,hpa -n "${NAMESPACE}"
