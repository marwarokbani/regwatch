# RegWatch — Frontend

Angular 22 + Angular Material. Nécessite **Node ≥ 24.15** (ou ≥ 22.22.3) pour le développement local.

```bash
npm ci
npm start      # http://localhost:4200, /api et /health relayés vers le backend (proxy.conf.json)
npm test       # tests unitaires (Vitest)
```

En Docker, l'application est construite puis servie par nginx, qui relaie aussi `/api` vers le backend.
