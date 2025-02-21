set VERSION=0.0.1
docker build -t grozadanut/cloud-anaf-connector:latest -t grozadanut/cloud-anaf-connector:%VERSION% --target production .
docker push grozadanut/cloud-anaf-connector:%VERSION%
docker push grozadanut/cloud-anaf-connector:latest
PAUSE