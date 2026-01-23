# --- Stage 1: Build ---
FROM node:20-alpine AS build-stage

WORKDIR /app

# Cache des dépendances
COPY package*.json ./
RUN npm ci

# Build de l'app
COPY . .
RUN npm run build

# --- Stage 2: Runtime ---
FROM nginx:alpine

# Copie de la config optimisée
COPY nginx.conf /etc/nginx/nginx.conf

# Nettoyage du dossier par défaut de nginx
RUN rm -rf /usr/share/nginx/html/*

# Copie du build (Vérifie bien le nom du dossier dans dist/)
COPY --from=build-stage /app/dist/book-network-ui/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]