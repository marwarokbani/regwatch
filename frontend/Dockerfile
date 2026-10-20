# --- Build : Node 24 (Angular 22 exige Node >= 22.22.3 ou >= 24.15) ---
FROM node:24.22.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# --- Runtime : fichiers statiques servis par nginx, qui fait aussi proxy vers l'API ---
FROM nginx:1.30.6-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
