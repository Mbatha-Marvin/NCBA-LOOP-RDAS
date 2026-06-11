# Deployment Guide

## Prerequisites

- Docker
- k3d (or any Kubernetes cluster)
- kubectl configured for your cluster
- Java 17+ and Maven (for local builds)

## Quick Deploy to k3d

### 1. Verify Cluster

```bash
k3d cluster list
kubectl cluster-info
```

### 2. Build the Application

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
mvn clean package -DskipTests
```

### 3. Build Docker Image

```bash
# Build image and load into k3d
docker build -t ncba-loop/rdas:1.0.0 .
k3d image import ncba-loop/rdas:1.0.0 -c ncba-loop
```

### 4. Deploy to Kubernetes

```bash
# Using kubectl directly
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Or using kustomize
kubectl apply -k k8s/
```

### 5. Verify Deployment

```bash
# Check deployment status
kubectl get deployments -n ncba-loop

# Check pods
kubectl get pods -n ncba-loop

# Check service
kubectl get svc -n ncba-loop

# Watch logs
kubectl logs -f deployment/rdas -n ncba-loop
```

### 6. Access the Application

```bash
# Port-forward to access locally
kubectl port-forward svc/rdas -n ncba-loop 8080:8080

# Now access at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 7. Test the API

```bash
# Health check
curl http://localhost:8080/actuator/health

# List continents
curl http://localhost:8080/api/v1/continents

# Search countries
curl "http://localhost:8080/api/v1/countries?page=0&size=5&sort=name:asc"
```

## Configuration

The application is configured via environment variables (set in ConfigMap):

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_PORT` | `8080` | Server port |
| `LOG_LEVEL` | `INFO` | Application log level |
| `SOAP_ENDPOINT` | `http://webservices.oorsprong.org/...` | SOAP service URL |

### Updating Configuration

```bash
kubectl edit configmap rdas-config -n ncba-loop
kubectl rollout restart deployment/rdas -n ncba-loop
```

## Scaling

```bash
# Scale to multiple replicas
kubectl scale deployment rdas -n ncba-loop --replicas=3
```

## Cleanup

```bash
kubectl delete -k k8s/
kubectl delete namespace ncba-loop
```
