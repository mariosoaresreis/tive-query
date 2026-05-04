# tive-query

Read/query service for the Tive platform (CQRS read side).

It runs in the **same GCP project** as `tive-webhook-receiver`, but in a **different Kubernetes namespace** (`tive-query`).

## What this service does

- Reads current tracker position from Redis keys written by `tive-webhook-receiver`
- Reads alert history from PostgreSQL table `tive_alerts`
- Exposes query APIs for trackers and alerts
- Protects API endpoints with `X-Api-Key`
- Exposes `/actuator/prometheus` for Prometheus scraping

## API endpoints

All endpoints (except `/actuator/**`) require header `X-Api-Key`.

- `GET /api/v1/trackers`
- `GET /api/v1/trackers/{trackerId}/position`
- `GET /api/v1/trackers/{trackerId}/alerts?from=...&to=...&type=...&page=0&size=20`
- `GET /api/v1/alerts?from=...&to=...&type=...&page=0&size=20`

## Local run

Start dependencies (Redis + Postgres) and service:

```bash
docker compose up -d postgres redis
mvn -B -ntp clean verify
TIVE_QUERY_API_KEY=local-dev-key mvn spring-boot:run
```

Query example:

```bash
curl -H "X-Api-Key: local-dev-key" http://localhost:8081/api/v1/trackers
```

## Build container

```bash
docker build -t tive-query:local .
```

## GKE deployment (same GCP project, different namespace)

Namespace is defined in `k8s/namespace.yaml` as `tive-query`.

1. Build and push image (Cloud Build):

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_REGION=us-central1,_REPOSITORY=tive-repo,_GKE_CLUSTER=tive-cluster
```

2. Apply manifests manually (optional path):

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/serviceaccount.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

4. Expose traffic (choose one option):

```bash
# Option A: GKE Ingress (built-in)
kubectl apply -f k8s/ingress.yaml

# Option B: Gateway API (if your cluster has Gateway/HTTPRoute configured)
kubectl apply -f k8s/httproute.yaml
```

Before applying, set the correct hostname and gateway references:

- `k8s/ingress.yaml` -> `spec.rules[0].host`
- `k8s/httproute.yaml` -> `spec.hostnames`, `spec.parentRefs`

3. Create secrets (from template):

```bash
cp k8s/secret.example.yaml k8s/secret.yaml
# edit k8s/secret.yaml with real values
kubectl apply -f k8s/secret.yaml
```

## Required runtime variables/secrets

Environment/config values:

- `SPRING_PROFILES_ACTIVE=gcp`
- `DB_NAME`
- `DB_USER`
- `CLOUD_SQL_INSTANCE`
- `REDIS_HOST`
- `REDIS_PORT`
- `PORT=8081`

Secrets:

- `DB_PASSWORD`
- `TIVE_QUERY_API_KEY`

## Notes

- This service is read-only. Database migrations remain owned by `tive-webhook-receiver`.
- Redis prefix defaults to `tive:tracker:position:` and must match the writer service.



