# ==============================================================================
# Graham Select Frontend - Flutter Web Production Container (NGINX SPA)
# ==============================================================================
FROM nginx:1.27-alpine

# Remove default nginx configurations
RUN rm -rf /etc/nginx/conf.d/*

# Copy optimized SPA nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled Flutter web release bundle
COPY build/web /workspace/build/web

# Expose port 8080 (unprivileged/standard internal port)
EXPOSE 8080

# Run nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
