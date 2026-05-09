FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
COPY node_modules ./node_modules
COPY . .
RUN npm run build --configuration=production || true

FROM nginx:alpine
COPY --from=build /app/dist/backoffice/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80