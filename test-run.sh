#!/bin/bash

docker-compose -p dbhub -f test-docker-compose.yaml up -d

sleep 60

cookie=$(curl -s -c - -d '{"username":"admin","password":"admin"}' http://localhost:8082/login)

curl -b <(echo "$cookie") -H 'Content-Type: application/json' -d '{"habr.com":{"type":"HTML","config":"{\"urlWithPageVar\":\"https://habr.com/ru/news/page{page}\",\"itemSelector\":\".tm-articles-list__item:not(.tm-voice-article)\",\"linkSelector\":\".tm-title__link\",\"titleSelector\":\".tm-title\",\"textSelector\":\".article-formatted-body\",\"timeSelector\":\".tm-article-datetime-published time\",\"timeFormat\":\"<ISO>\",\"usesTimeTag\":true,\"maxPage\":50,\"useLinkForItemInfo\":false}"},"tarantool.io":{"type":"HTML","config":"{\"urlWithPageVar\":\"https://www.tarantool.io/blog/ru/\",\"itemSelector\":\".Card_on_main__vJ7bG\",\"linkSelector\":\"a\",\"titleSelector\":\".CoverBlog_title__jM6Pf\",\"textSelector\":\".article\",\"timeSelector\":\".CoverBlog_published__JZv_a p\",\"timeFormat\":\"dd.MM.yyyy\",\"timeZone\":\"Europe/Moscow\",\"usesTimeTag\":false,\"maxPage\":1,\"useLinkForItemInfo\":true}"}}' http://localhost:8082/admin/collector-config/sources 

curl -b <(echo "$cookie") -H 'Content-Type: application/json' -d '{"rate":60,"maxArticlesPerSource":3,"keywords":["субд", "бд", "база данных", "базы данных", "базе данных", "базу данных", "базой данных", "баз данных", "базам данных", "базами данных", "базах данных", "db", "tarantool"]}' http://localhost:8082/admin/collector-config/collector

echo "LAUNCHED SUCCESSFULLY! Page available at http://localhost:8079"
echo "Open it and in 1-2 mins, after first news collecting is finished, you'll see them"

