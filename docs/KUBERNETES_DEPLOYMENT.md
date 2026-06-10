# Deploying to Kubernetes

> **About this document:** Runbook for deploying the Country Info Service to a Kubernetes cluster using the manifests in k8s/.
>
> **Update when:** Manifest files, image tags, environment variables, or probe configuration change.

---

## What gets deployed

Everything runs in the `country-info` namespace:

```
  External client
        │
        ▼
  LoadBalancer (country-info-service, port 80)
        │
        ▼
  App pods (2 replicas, scales to 6 via HPA)
        │
        ├──► MySQL pod (ClusterIP, internal only)
        │
        └──► Oorsprong SOAP API (external, over the internet)
```

The application pods are stateless. MySQL runs as a single in-cluster pod for this case study.

---

## Before you start

You will need:

- A working Kubernetes cluster (minikube, Docker Desktop, EKS, GKE, AKS, or similar)
- `kubectl` configured to talk to that cluster
- Docker, to build the application image

**minikube:** run `minikube tunnel` in a separate terminal if you want a LoadBalancer external IP.

**Docker Desktop:** enable Kubernetes in settings. Locally built images are available to the cluster without pushing to a registry.

---

## Manifest reference

All files are in the `k8s/` directory:

| File | What it does                                                  |
|------|---------------------------------------------------------------|
| `namespace.yaml` | Creates the `country-info` namespace                          |
| `mysql-secret.yaml` | Root password, database name, and app DB user credentials     |
| `mysql-deployment.yaml` | MySQL 8.0 pod                                                 |
| `mysql-service.yaml` | Internal DNS name `mysql` on port 3307                        |
| `configmap.yaml` | Non-sensitive app settings (DB URL, pool size, SOAP timeouts) |
| `app-secret.yaml` | Database password consumed by the application                 |
| `deployment.yaml` | Application pods with health probes and resource limits       |
| `service.yaml` | LoadBalancer, maps port 80 to container port 8080             |
| `hpa.yaml` | Scales between 2 and 6 replicas at 70% CPU                    |

Deploy scripts (`deploy.sh`, `deploy.ps1`) apply these files in the correct order and wait for rollouts to finish. `teardown.sh` removes everything.

---

## Deployment steps

### 1. Build the Docker image

From the project root:

```bash
docker build -t country-info-service:1.0.0 .
```

The image tag should match the version in `pom.xml`. When you release a new version, update both.

**minikube** — the cluster cannot see images in your host Docker daemon unless you load them:

```bash
minikube image load country-info-service:1.0.0
```

### 2. Apply the manifests
**Windows (PowerShell):**

```powershell
cd k8s
.\deploy.ps1
```

The script applies all manifests, waits for MySQL to become ready, then waits for the application rollout.

To apply manifests manually instead:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/mysql-secret.yaml
kubectl apply -f k8s/mysql-deployment.yaml
kubectl apply -f k8s/mysql-service.yaml
kubectl wait --for=condition=available deployment/mysql -n country-info --timeout=180s
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/app-secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
kubectl rollout status deployment/country-info-service -n country-info
```

### 3. Confirm everything is running

```bash
kubectl get all -n country-info
```

You should see:

- `mysql` deployment: 1/1 ready
- `country-info-service` deployment: 2/2 ready
- A LoadBalancer service (external IP may take a minute to appear)

Check application logs if anything is not ready:

```bash
kubectl logs -l app=country-info-service -n country-info --tail=50
```

### 4. Reach the application

**Option A — LoadBalancer**

```bash
kubectl get svc country-info-service -n country-info
```

Use the `EXTERNAL-IP` value:

```bash
curl http://<EXTERNAL-IP>/actuator/health
```

**Option B — minikube**

```bash
minikube service country-info-service -n country-info --url
```

**Option C — port forward (works on any cluster)**

```bash
kubectl port-forward svc/country-info-service 8080:80 -n country-info
curl http://localhost:8080/actuator/health
```

### 5. Smoke test

```bash
curl -X POST http://localhost:8080/api/v1/countries/import \
  -H "Content-Type: application/json" \
  -d '{"name": "Kenya"}'
```

A `201 Created` response with country details confirms the full path: REST → SOAP → MySQL.

---

## Changing configuration

### Non-sensitive settings (ConfigMap)

Edit `k8s/configmap.yaml`. Common values:

| Key | Purpose |
|-----|---------|
| `DB_URL` | JDBC connection string (host must be `mysql` inside the cluster) |
| `DB_USERNAME` | Must match the user created by `mysql-secret.yaml` |
| `DB_POOL_SIZE` | HikariCP maximum pool size per pod |
| `SOAP_CONNECT_TIMEOUT_MS` | SOAP connection timeout |
| `SOAP_READ_TIMEOUT_MS` | SOAP read timeout |

Apply and restart:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl rollout restart deployment/country-info-service -n country-info
```

### Secrets

Database passwords live in `k8s/mysql-secret.yaml` and `k8s/app-secret.yaml`. The username in the ConfigMap and the password in `app-secret.yaml` must match what MySQL was configured with.


---

## Rolling out a new version

```bash
docker build -t country-info-service:1.0.1 .
kubectl set image deployment/country-info-service \
  country-info-service=country-info-service:1.0.1 \
  -n country-info
kubectl rollout status deployment/country-info-service -n country-info
```

The deployment uses a rolling update with `maxUnavailable: 0`, so at least one pod stays up during the rollout.

To roll back:

```bash
kubectl rollout undo deployment/country-info-service -n country-info
```

---

## Scaling

The HPA keeps between 2 and 6 replicas based on CPU utilisation (target: 70%):

```bash
kubectl get hpa -n country-info
```

Manual override:

```bash
kubectl scale deployment/country-info-service --replicas=4 -n country-info
```

Note: HPA will adjust the count again unless you change or remove the HPA resource.

---

## Teardown

Remove all resources in the namespace:

```bash
cd k8s
./teardown.sh
```

Or delete the entire namespace:

```bash
kubectl delete namespace country-info
```
