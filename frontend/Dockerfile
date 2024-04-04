FROM node:20 AS build
WORKDIR /app
COPY . .
ENV DBHUB_WEBPACK_MODE=production
ARG DBHUB_API_URL
ENV DBHUB_API_URL=$DBHUB_API_URL 
RUN npm install
RUN npm run build

FROM nginx:alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=build /app/dist .
