# syntax=docker/dockerfile:1
# ─────────────────────────────────────────────────────────────────────────────
# Multi-stage build for the React (Vite) frontend.
#
# WHY: in production we don't run `npm run dev`. We compile the app to static
# HTML/CSS/JS once, then serve those files with nginx — a tiny, fast web server
# with no Node runtime. The build tools stay in the first stage and are dropped.
#
# nginx also REVERSE-PROXIES /api to the backend (see nginx.conf), so the browser
# only ever talks to one origin. That means no CORS, and no backend URL baked
# into the JS bundle — VITE_API_URL is left empty so API calls are relative.
# ─────────────────────────────────────────────────────────────────────────────

# --- build stage -------------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

# Install deps from the lockfile first (cached until package*.json changes).
COPY package.json package-lock.json ./
RUN npm ci

# Empty base URL -> client.ts issues relative /api requests -> same origin.
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL

COPY . .
RUN npm run build

# --- runtime stage -----------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# SPA routing + /api proxy config replaces nginx's default site.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
