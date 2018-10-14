FROM nginx:1.15-alpine

COPY k8s/nginx.conf /etc/nginx/conf.d/default.conf
