$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir
$ImageName = if ($env:IMAGE_NAME) { $env:IMAGE_NAME } else { "country-info-service:1.0.0" }
$Namespace = if ($env:NAMESPACE) { $env:NAMESPACE } else { "country-info" }

Write-Host "==> Building Docker image: $ImageName"
docker build -t $ImageName $ProjectDir

Write-Host "==> Applying Kubernetes manifests"
kubectl apply -f "$ScriptDir\namespace.yaml"
kubectl apply -f "$ScriptDir\mysql-secret.yaml"
kubectl apply -f "$ScriptDir\mysql-deployment.yaml"
kubectl apply -f "$ScriptDir\mysql-service.yaml"
kubectl apply -f "$ScriptDir\configmap.yaml"
kubectl apply -f "$ScriptDir\app-secret.yaml"
kubectl apply -f "$ScriptDir\deployment.yaml"
kubectl apply -f "$ScriptDir\service.yaml"
kubectl apply -f "$ScriptDir\hpa.yaml"

Write-Host "==> Waiting for MySQL to become ready"
kubectl rollout status deployment/mysql -n $Namespace --timeout=180s

Write-Host "==> Waiting for application to become ready"
kubectl rollout status deployment/country-info-service -n $Namespace --timeout=300s

Write-Host "==> Deployment complete"
kubectl get pods,svc,hpa -n $Namespace
