FROM node:20 AS build
WORKDIR /app
COPY . .
ENV DBHUB_WEBPACK_MODE=production
ARG DBHUB_API_URL
ENV DBHUB_API_URL=$DBHUB_API_URL 
RUN npm install
RUN npm run build

FROM nginx:alpine
RUN rm /etc/nginx/conf.d/default.conf
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d
CMD ["nginx", "-g", "daemon off;"]
