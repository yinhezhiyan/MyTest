FROM node:20-alpine AS builder
WORKDIR /app/vue
COPY vue/package*.json ./
RUN npm ci
COPY vue ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/vue/dist /usr/share/nginx/html
EXPOSE 80
