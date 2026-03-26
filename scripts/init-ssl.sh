#!/bin/bash
# ============================================================
# init-ssl.sh — Run ONCE on the server before first deploy
# ============================================================
set -e

DOMAIN="wisecart.ph"
EMAIL="admin@wisecart.ph"

echo "1. Creating Docker network..."
docker network create wisecart_ecommerce-network 2>/dev/null || echo "Network already exists"

echo "2. Creating required directories..."
mkdir -p certbot/www certbot/conf logs/nginx nginx

echo "3. Starting temporary nginx for ACME challenge..."
cat > /tmp/nginx-init.conf <<'EOF'
events { worker_connections 1024; }
http {
  server {
    listen 80;
    server_name wisecart.ph www.wisecart.ph shop.wisecart.ph;
    location /.well-known/acme-challenge/ {
      root /var/www/certbot;
    }
    location / { return 404; }
  }
}
EOF

docker run --rm -d --name nginx-init -p 80:80 \
  -v $(pwd)/certbot/www:/var/www/certbot \
  -v /tmp/nginx-init.conf:/etc/nginx/nginx.conf:ro \
  nginx:alpine

echo "4. Obtaining SSL certificate..."
docker run --rm \
  -v $(pwd)/certbot/www:/var/www/certbot \
  -v $(pwd)/certbot/conf:/etc/letsencrypt \
  certbot/certbot certonly \
  --webroot -w /var/www/certbot \
  --email "$EMAIL" \
  -d "$DOMAIN" -d "www.$DOMAIN" \
  --agree-tos --no-eff-email

echo "5. Stopping init nginx..."
docker stop nginx-init

echo ""
echo "✅ SSL certificates obtained successfully!"
echo ""
echo "👉 Next steps:"
echo "   1. Copy your .env file to this directory"
echo "   2. Copy nginx/nginx.conf to ./nginx/nginx.conf"
echo "   3. Run: docker-compose -f docker-compose.prod.yml up -d"