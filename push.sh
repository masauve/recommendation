mvn clean install
docker build -t quay.io/msauve/recommendation:v5-ext .
docker push quay.io/msauve/recommendation:v5-ext
