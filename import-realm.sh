#!/usr/bin/env bash
set -euo pipefail

KC_CONTAINER=keycloak             # Keycloak container name
REALM_JSON=./realm_export.json    # Path to the Realm JSON file to import
ADMIN_USER=admin
ADMIN_PASS=admin
KEYCLOAK_URL=http://localhost:8080

# Login to master realm
docker exec $KC_CONTAINER \
  /opt/keycloak/bin/kcadm.sh config credentials \
    --server $KEYCLOAK_URL \
    --realm master \
    --user $ADMIN_USER \
    --password $ADMIN_PASS

# Import Realm
echo "Importing realm from $REALM_JSON..."
docker exec -i $KC_CONTAINER \
  /opt/keycloak/bin/kcadm.sh create realms \
    -f /dev/stdin <<EOF
$(cat $REALM_JSON)
EOF

echo "Realm import complete."
