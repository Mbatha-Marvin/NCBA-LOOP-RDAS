# Troubleshooting Guide

## Common Issues and Resolutions

### 1. Pods Not Starting

**Check pod status:**
```bash
kubectl get pods -n ncba-loop
kubectl describe pod -l app=rdas -n ncba-loop
```

**Common causes:**
- **ImagePullBackOff**: The Docker image isn't imported into k3d.
  ```bash
  k3d image import ncba-loop/rdas:1.0.0 -c ncba-loop
  ```
- **CrashLoopBackOff**: Application fails to start. Check logs:
  ```bash
  kubectl logs -l app=rdas -n ncba-loop --tail=100
  ```

### 2. Application Fails to Start

**Check logs for Spring Boot errors:**
```bash
kubectl logs deployment/rdas -n ncba-loop
```

**Common issues:**
- Missing configuration: Verify the ConfigMap is applied
  ```bash
  kubectl get configmap rdas-config -n ncba-loop
  ```
- SOAP endpoint unreachable: The external SOAP service may be down. The circuit breaker will handle this gracefully.

### 3. Liveness/Readiness Probe Failures

**Check probe configuration:**
```bash
kubectl describe pod -l app=rdas -n ncba-loop | grep -A 15 "Liveness\|Readiness"
```

**Check if actuator is working:**
```bash
kubectl port-forward svc/rdas -n ncba-loop 8080:8080
curl http://localhost:8080/actuator/health
```

### 4. 503 Service Unavailable Responses

This means the SOAP service is unavailable AND there's no cached data:
```bash
kubectl logs -l app=rdas -n ncba-loop | grep -i "circuit\service unavailable"
```

**Resolution:** Wait for the SOAP service to recover. The circuit breaker will auto-recover when it goes half-open.

### 5. High Memory Usage

**Check resource usage:**
```bash
kubectl top pods -n ncba-loop
```

The Caffeine cache is bounded at 500 entries. If memory pressure persists:
- Increase resource limits in `k8s/deployment.yaml`
- Scale horizontally: `kubectl scale deployment rdas -n ncba-loop --replicas=2`

### 6. Debug Mode

**Increase log level:**
```bash
kubectl set env deployment/rdas -n ncba-loop LOG_LEVEL=DEBUG
# Or edit the ConfigMap and restart:
kubectl edit configmap rdas-config -n ncba-loop
kubectl rollout restart deployment/rdas -n ncba-loop
```

### 7. Network Connectivity Issues

**Test SOAP endpoint from inside the pod:**
```bash
kubectl exec -it deployment/rdas -n ncba-loop -- sh
# Inside the pod:
wget -qO- "http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL"
```

### 8. Viewing Metrics

```bash
# Port-forward and check Prometheus metrics
kubectl port-forward svc/rdas -n ncba-loop 8080:8080
curl http://localhost:8080/actuator/prometheus

# Key metrics to watch:
#   - resilience4j_circuitbreaker_state (0=closed, 1=open, 2=half_open)
#   - cache_gets_total{cache="fullCountries",result="hit"}
#   - cache_gets_total{cache="fullCountries",result="miss"}
#   - http_server_requests_seconds_count
```

## Quick Health Check Script

```bash
#!/bin/bash
echo "=== Pod Status ==="
kubectl get pods -n ncba-loop
echo ""
echo "=== Service Status ==="
kubectl get svc -n ncba-loop
echo ""
echo "=== Recent Logs ==="
kubectl logs -l app=rdas -n ncba-loop --tail=20
echo ""
echo "=== Health Check ==="
kubectl port-forward svc/rdas -n ncba-loop 8080:8080 &
PF_PID=$!
sleep 2
curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8080/actuator/health
kill $PF_PID 2>/dev/null
```
