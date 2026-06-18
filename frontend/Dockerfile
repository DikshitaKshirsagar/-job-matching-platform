# Multi-stage Dockerfile for React frontend production build
# Stage 1: Build the React application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy the rest of the application source
COPY . .

# Build the production bundle
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=$REACT_APP_API_URL
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy the built assets from the build stage
COPY --from=build /app/build /usr/share/nginx/html

# Copy nginx configuration for single-page application
# Default nginx config for SPA with proper fallback routing
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.frontend.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]