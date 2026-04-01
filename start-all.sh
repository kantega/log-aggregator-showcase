#!/bin/bash
set -e

# Port assignments:
#   frontend          -> 4200 (Angular default)
#   log-manager       -> 8080
#   edge              -> 8081
#   adapter-noark-a   -> 8082
#   adapter-noark-b   -> 8083
#   external-apis-mock -> 8084
#
# Infrastructure (via docker-compose):
#   MySQL             -> 3307
#   MongoDB           -> 27017
#   RabbitMQ          -> 5672 (AMQP), 15672 (Management UI)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Starting infrastructure ==="
docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d

echo "=== Waiting for infrastructure to be ready ==="
sleep 5

echo "=== Starting Spring Boot services ==="
cd "$SCRIPT_DIR/log-manager"       && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080" &
cd "$SCRIPT_DIR/edge"              && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" &
cd "$SCRIPT_DIR/adapter-noark-a"   && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082" &
cd "$SCRIPT_DIR/adapter-noark-b"   && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083" &
cd "$SCRIPT_DIR/external-apis-mock" && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084" &

echo "=== Starting Angular frontend ==="
cd "$SCRIPT_DIR/frontend" && npm start &

echo ""
echo "=== All services starting ==="
echo "  Frontend:           http://localhost:4200"
echo "  Log Manager:        http://localhost:8080"
echo "  Edge:               http://localhost:8081"
echo "  Adapter Noark A:    http://localhost:8082"
echo "  Adapter Noark B:    http://localhost:8083"
echo "  External APIs Mock: http://localhost:8084"
echo "  RabbitMQ Management: http://localhost:15672"
echo ""
echo "Type 'stop' or press Ctrl+C to stop all services"
echo ""

cleanup() {
  echo ""
  echo "=== Stopping all services ==="
  pkill -f 'spring-boot:run'
  kill $(jobs -p) 2>/dev/null
  echo "=== All services stopped ==="
  exit 0
}

trap cleanup INT TERM

while read -r line; do
  if [ "$line" = "stop" ]; then
    cleanup
  fi
done
