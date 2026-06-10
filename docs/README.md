# Documentation Index

This folder contains the operational and setup guides for the Country Info Service. Each file is plain Markdown — edit it directly in any text editor or IDE.

## Files

| File | Purpose | Update when… |
|------|---------|--------------|
| [RUNNING_AND_TESTING.md](RUNNING_AND_TESTING.md) | **Steps to run and test the application** | API endpoints, Postman collection, or local setup changes |
| [SOAPUI_SETUP.md](SOAPUI_SETUP.md) | Manual SOAP testing with SoapUI | WSDL URL, SOAP operations, or request/response mapping changes |
| [KUBERNETES_DEPLOYMENT.md](KUBERNETES_DEPLOYMENT.md) | Deploying to a Kubernetes cluster | K8s manifests, image tags, or environment variables change |
| [KUBERNETES_TROUBLESHOOTING.md](KUBERNETES_TROUBLESHOOTING.md) | Diagnosing common cluster issues | Probes, logging, or error handling behaviour changes |

The main [README.md](../README.md) at the project root includes API overview and **Postman collection** location (`postman/` folder).

[HELP.md](../HELP.md) covers local setup (XAMPP port 3307) and running the application.

## Conventions used across these docs

- **Project root** means the directory containing `pom.xml` (where you cloned or extracted this repository).
- **Commands** assume you are already at the project root unless stated otherwise.
- **Placeholders** like `<pod-name>` or `<EXTERNAL-IP>` should be replaced with values from your environment.
- **Version numbers** (e.g. `1.0.0`) match the `version` field in `pom.xml`. Update both when releasing a new build.

## Keeping docs in sync with code

When you change the application, check whether these areas need a doc update:

1. **New or renamed API endpoints** → `README.md`, `postman/Country-Info-Service.postman_collection.json`, and `RUNNING_AND_TESTING.md`
2. **New environment variables** → `HELP.md` and `k8s/configmap.yaml` comments in `KUBERNETES_DEPLOYMENT.md`
3. **SOAP client changes** → `SOAPUI_SETUP.md`
4. **Kubernetes manifest changes** → `KUBERNETES_DEPLOYMENT.md` manifest table
5. **New failure modes or log messages** → `KUBERNETES_TROUBLESHOOTING.md`
