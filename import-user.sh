#!/usr/bin/env bash
KC=keycloak            # Container name
REALM=my-groupware     # Your Realm name
CLIENT_SCRIPT=/opt/keycloak/bin/kcadm.sh

# Login to master realm
docker exec $KC $CLIENT_SCRIPT config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

CLIENT_UUID=$(docker exec $KC $CLIENT_SCRIPT get clients \
  -r my-groupware \
  -q clientId=groupware-backend \
| jq -r '.[0].id')
echo "my-groupware UUID: $CLIENT_UUID"

docker exec $KC $CLIENT_SCRIPT get clients/$CLIENT_UUID/client-secret \
  -r my-groupware \


docker exec $KC $CLIENT_SCRIPT create roles \
    -r "$REALM" \
    -s name="admin"

docker exec $KC $CLIENT_SCRIPT create roles \
    -r "$REALM" \
    -s name="user"

docker exec $KC $CLIENT_SCRIPT create roles \
    -r "$REALM" \
    -s name="guest"

# Create users line by line
while IFS=, read username email password role; do
  echo ">>> Creating $username"
  docker exec $KC $CLIENT_SCRIPT create users \
    -r $REALM \
    -s username="$username" \
    -s enabled=true \
    -s email="$email"

  echo ">>> Setting password"
  docker exec $KC $CLIENT_SCRIPT set-password \
    -r $REALM \
    --username "$username" \
    --new-password "$password"

  echo ">>> Assigning role $role"
  docker exec $KC $CLIENT_SCRIPT add-roles \
    -r $REALM \
    --uusername "$username" \
    --cclientid "groupware-backend" \
    --rolename "$role"
done < users.csv
