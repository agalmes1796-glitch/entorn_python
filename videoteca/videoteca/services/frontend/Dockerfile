# ==============================================================================
# Videoteca — microservi "frontend" (Nuxt 3)
#
# Imatge multi-etape, lleugera, per a producció. L'aplicació escolta al
# port 3001 (NITRO_PORT) dins del contenidor.
#
# Variable d'entorn en execució:
#   NUXT_GATEWAY_URL  — URL base del gateway (ex: http://gateway:3000)
#   El /api/movies d'aquest microservi crida a {NUXT_GATEWAY_URL}/api/movies
# ==============================================================================

# ---- 1) Dependències ----
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci

# ---- 2) Build ----
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NUXT_GATEWAY_URL s'avalua en temps d'execució, no de build:
# no la fixem aquí perquè la imatge sigui portable entre entorns.
RUN npm run build

# ---- 3) Runtime ----
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production \
    NITRO_HOST=0.0.0.0 \
    NITRO_PORT=3001
COPY --from=build /app/.output ./.output
EXPOSE 3001
CMD ["node", ".output/server/index.mjs"]
