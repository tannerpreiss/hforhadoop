#!/bin/sh
echo "Updating logger files..."
cp ../../elmerland/dist/css/logger/styles.css       html/css/logger/styles.css
cp ../../elmerland/dist/css/libs/normalize.css      html/css/libs/normalize.css

cp ../../elmerland/dist/js/logger/script.js         html/js/logger/script.js
cp ../../elmerland/dist/js/libs/jquery-2.1.1.min.js html/js/libs/jquery-2.1.1.min.js

cp ../../elmerland/dist/logger.html                 html/logger.html

echo "Updating host monitor files..."

cp ../../elmerland/dist/css/host_monitor/styles.css html/css/host_monitor/styles.css
cp ../../elmerland/dist/css/libs/normalize.css      html/css/libs/normalize.css

cp ../../elmerland/dist/js/host_monitor/script.js   html/js/host_monitor/script.js
cp ../../elmerland/dist/js/libs/jquery-2.1.1.min.js html/js/libs/jquery-2.1.1.min.js

cp ../../elmerland/dist/host_monitor.html           html/host_monitor.html