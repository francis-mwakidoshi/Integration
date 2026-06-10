# Troubleshooting on Kubernetes

> **About this document:** Diagnostic runbook for common problems when running the Country Info Service in Kubernetes.
>
> **Update when:** Health probes, logging format, error messages, or dependency configuration change.

---

## Quick checks

Run these first:

```bash
# Overall state
kubectl get all -n country-info

# Pod details including restart count
kubectl get pods -n country-info -o wide

# Recent cluster events (often points to the root cause)
kubectl get events -n country-info --sort-by='.lastTimestamp'

# Application logs from all replicas
kubectl logs -l app=country-info-service -n country-info --tail=100

# MySQL logs
kubectl logs -l app=mysql -n country-info --tail=50
```

If a specific pod is failing, inspect it directly:

```bash
kubectl describe pod <pod-name> -n country-info
kubectl logs <pod-name> -n country-info --previous   # logs from the last crashed container
```

---

## Pod stuck in CrashLoopBackOff

**What you see:** Pod status is `CrashLoopBackOff` or `Error`. The deployment reports `0/N` ready replicas.

**What to check:**

```bash
kubectl logs <pod-name> -n country-info --previous
kubectl describe pod <pod-name> -n country-info
```

**Typical causes:**

| Symptom in logs or events | Likely cause | What to do |
|---------------------------|--------------|------------|
| `Communications link failure`, `Connection refused` | App started before MySQL was ready | Wait for MySQL, then restart the app deployment |
| `Access denied for user` | DB username/password mismatch | Align `k8s/configmap.yaml` username with `k8s/mysql-secret.yaml` and `k8s/app-secret.yaml` password |
| `ErrImagePull`, `ImagePullBackOff` | Image not available to the cluster | Rebuild the image; on minikube run `minikube image load country-info-service:1.0.0` |
| `OOMKilled` in `describe` output | Container exceeded memory limit | Increase memory limits in `k8s/deployment.yaml` |
| `Address already in use` | Port conflict inside the container | Check that `SERVER_PORT` is 8080 and matches the container port |

**Restart after MySQL is healthy:**

```bash
kubectl wait --for=condition=available deployment/mysql -n country-info --timeout=180s
kubectl rollout restart deployment/country-info-service -n country-info
kubectl rollout status deployment/country-info-service -n country-info
```

---

## Readiness probe failing

**What you see:** Pod is running but not receiving traffic. Events show `Readiness probe failed: HTTP probe failed with statuscode: 503`.

**What to check:**

```bash
kubectl exec -it <pod-name> -n country-info -- \
  wget -qO- http://localhost:8080/actuator/health/readiness
```

The readiness endpoint stays down until the application can connect to MySQL. Common reasons:

- MySQL pod is not ready yet — wait and recheck.
- `DB_URL` in the ConfigMap points to `localhost` instead of `mysql`. Inside the cluster, the hostname must be the Kubernetes service name: `mysql:3307`.
- The application is still starting. The startup probe allows up to ~2 minutes; if your cluster is slow, increase `startupProbe.failureThreshold` in `k8s/deployment.yaml`.

Example adjustment:

```yaml
startupProbe:
  failureThreshold: 18
readinessProbe:
  initialDelaySeconds: 45
```

Apply the change and restart the deployment.

---

## Pod restarting due to liveness probe

**What you see:** Pod restart count climbing. Events mention `Liveness probe failed`.

**What to check:**

```bash
kubectl logs <pod-name> -n country-info --previous
kubectl top pod -n country-info
```

Common reasons:

- A slow SOAP call blocked the JVM long enough for the liveness probe to fail. Ensure `SOAP_READ_TIMEOUT_MS` (ConfigMap) is lower than the probe's failure window.
- Memory pressure causing long GC pauses. Check `kubectl top pod` and consider raising memory limits.
- `livenessProbe.initialDelaySeconds` is too low for a cold start. Try 90 seconds or more.

Liveness failures are worth taking seriously — they mean Kubernetes is killing and recreating your containers.

---

## REST API returns 502 (SOAP errors)

**What you see:** HTTP 502 with a message like "Country information service is temporarily unavailable." Application logs contain `SOAP CountryISOCode fallback triggered` or similar.

**What to check:**

```bash
kubectl logs -l app=country-info-service -n country-info | grep -i soap
```

Verify the pod can reach the external SOAP service:

```bash
kubectl exec -it <pod-name> -n country-info -- \
  wget -qO- "http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL"
```

If that command fails or hangs, the cluster may not have outbound internet access. Configure egress (NAT gateway, proxy, or firewall rules) as required by your platform.

Other causes:

| Cause | Fix |
|-------|-----|
| Oorsprong service temporarily down | Wait and retry; the circuit breaker will close again after failures stop |
| Read timeout too aggressive | Increase `SOAP_READ_TIMEOUT_MS` in the ConfigMap and restart the deployment |
| Invalid country name | SOAP returns a fault; use names the service recognises (`Kenya`, not `KE`) |

After the external service recovers, restart the pods to reset a half-open circuit breaker:

```bash
kubectl rollout restart deployment/country-info-service -n country-info
```

---

## Database connection problems

**What you see:** Logs mention `HikariPool - Connection is not available` or `Unknown database 'countryinfo'`.

**Verify MySQL is accepting connections:**

```bash
kubectl exec -it deployment/mysql -n country-info -- \
  mysql -ucountryuser -pcountrypassword -e "SHOW DATABASES;"
```

**Verify the app pod has the right environment:**

```bash
kubectl exec -it <app-pod> -n country-info -- sh -c 'echo $DB_URL; echo $DB_USERNAME'
```

Checklist:

1. MySQL pod status is `Running` and `Ready`.
2. `DB_URL` host is `mysql`, not `localhost`.
3. `DB_USERNAME` and `DB_PASSWORD` match the values in the secrets.
4. The JDBC URL includes `allowPublicKeyRetrieval=true` (already set in the default ConfigMap).

---

## Cannot reach the service from outside the cluster

**What you see:** `curl` to the external IP times out, or the LoadBalancer `EXTERNAL-IP` shows `<pending>`.

```bash
kubectl get svc country-info-service -n country-info
kubectl describe svc country-info-service -n country-info
```

| Environment | What to do |
|-------------|------------|
| minikube | Run `minikube tunnel`, or use `minikube service country-info-service -n country-info --url` |
| Docker Desktop | Wait for `EXTERNAL-IP` to become `localhost` |
| Cloud (EKS/GKE/AKS) | Confirm the cloud load balancer controller is installed; check the cloud console for provisioning errors |
| Any cluster | Port-forward as a fallback: `kubectl port-forward svc/country-info-service 8080:80 -n country-info` |

---

## HPA not scaling

**What you see:** CPU looks high but replica count stays at 2.

```bash
kubectl describe hpa country-info-service-hpa -n country-info
kubectl top pods -n country-info
```

If `kubectl top` itself fails, the metrics server is not installed. On minikube:

```bash
minikube addons enable metrics-server
```

Also confirm the deployment defines CPU **requests** — the HPA calculates utilisation against requests, not limits. The default `k8s/deployment.yaml` sets `requests.cpu: 250m`.

---

## Reading application logs

Log lines include a short trace ID in brackets:

```
2026-06-10 10:15:30.123 [http-nio-8080-exec-1] INFO  [a1b2c3d4] c.f.service.CountryInfoService - Importing country by name raw=kenya normalized=Kenya
```

The trace ID is also returned in the `X-Trace-Id` response header. To follow a specific request across log lines:

```bash
kubectl logs -l app=country-info-service -n country-info | grep "a1b2c3d4"
```

You can supply your own trace ID when testing:

```bash
curl -H "X-Trace-Id: debug-001" \
  -X POST http://localhost:8080/api/v1/countries/import \
  -H "Content-Type: application/json" \
  -d '{"name": "Kenya"}'
```

---

## Health and metrics endpoints

Useful URLs (via port-forward or LoadBalancer):

| Endpoint | Used for |
|----------|----------|
| `/actuator/health` | General health check |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |
| `/actuator/metrics` | JVM, HTTP, and cache metrics |
| `/actuator/prometheus` | Prometheus scrape format |

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## HTTP status codes from the REST API

| Status | Meaning | Where to look |
|--------|---------|---------------|
| 400 | Invalid request body | Client payload; check validation rules in the README |
| 404 | Country ID not found | Confirm the ID exists: `GET /api/v1/countries` |
| 409 | Country already imported | That ISO code is already in the database |
| 502 | SOAP call failed | [REST API returns 502](#rest-api-returns-502-soap-errors) section above |
| 503 | Pod not ready | [Readiness probe failing](#readiness-probe-failing) section above |
| 500 | Unexpected error | Application logs for the stack trace |

---

## Recovery procedures

**Full redeploy** (destroys all data in the namespace):

```bash
kubectl delete namespace country-info
cd k8s && ./deploy.sh    # or .\deploy.ps1 on Windows
```

**Roll back a bad application version:**

```bash
kubectl rollout undo deployment/country-info-service -n country-info
kubectl rollout status deployment/country-info-service -n country-info
```

---

## Reporting an issue

If you need to escalate or ask for help, include:

1. Output of `kubectl get all -n country-info`
2. Last 200 lines of application logs: `kubectl logs -l app=country-info-service -n country-info --tail=200`
3. Recent events: `kubectl get events -n country-info --sort-by='.lastTimestamp' | tail -20`
4. The HTTP method, URL, request body, and response status you sent
5. The `X-Trace-Id` header from the response, if available

