#!/bin/sh
# Login and get token
RESP=$(wget -q -O- --post-data='{"email":"endgear@admin.de","password":"egHealthforge91!"}' \
  --header='Content-Type: application/json' \
  http://localhost:8080/v1/auth/login 2>/dev/null)
TOKEN=$(echo "$RESP" | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])' 2>/dev/null)

echo "Token erhalten: ${TOKEN:0:20}..."

# Trigger ETL
wget -q -O- --post-data="" \
  --header="Authorization: Bearer $TOKEN" \
  --header='Content-Type: application/json' \
  "http://localhost:8080/admin/v1/etl/run?source=USDA_FDC" 2>&1
