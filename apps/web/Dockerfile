# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS base
WORKDIR /app
RUN apk add --no-cache libc6-compat

# ---- deps ----
FROM base AS deps
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --prefer-offline

# ---- dev ----
FROM base AS dev
ENV NODE_ENV=development
COPY --from=deps /app/node_modules ./node_modules
COPY . .
EXPOSE 3000
CMD ["npm", "run", "dev"]

# ---- builder ----
FROM base AS builder
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- prod ----
FROM base AS prod
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1
RUN addgroup -S caqi -g 1000 && adduser -S caqi -G caqi -u 1000
COPY --from=builder /app/public ./public
COPY --from=builder --chown=caqi:caqi /app/.next/standalone ./
COPY --from=builder --chown=caqi:caqi /app/.next/static ./.next/static
USER caqi
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:3000/api/health || exit 1
CMD ["node", "server.js"]
