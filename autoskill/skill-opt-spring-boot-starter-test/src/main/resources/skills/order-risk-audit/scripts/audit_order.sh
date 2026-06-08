#!/bin/sh
set -eu

input="${1:-examples/high-risk-order.json}"
if [ ! -f "$input" ]; then
  printf '{"orderId":"","decision":"REVIEW","riskScore":100,"reasons":["missing_input"]}\n'
  exit 2
fi

order_id=$(sed -n 's/.*"orderId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$input" | head -n 1)
amount=$(sed -n 's/.*"amount"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$input" | head -n 1)
country=$(sed -n 's/.*"country"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$input" | head -n 1)

order_id=${order_id:-unknown}
amount=${amount:-0}
country=${country:-UNKNOWN}
risk_score=12
reasons=""

add_reason() {
  if [ -z "$reasons" ]; then
    reasons="\"$1\""
  else
    reasons="$reasons,\"$1\""
  fi
}

if [ "$amount" -ge 10000 ]; then
  risk_score=$((risk_score + 50))
  add_reason "high_amount"
fi

case "$country" in
  IR|KP|SY)
    risk_score=$((risk_score + 25))
    add_reason "blocked_country"
    ;;
esac

if grep -q '"newDevice"[[:space:]]*:[[:space:]]*true' "$input"; then
  risk_score=$((risk_score + 13))
  add_reason "new_device"
fi

if [ "$risk_score" -ge 70 ]; then
  decision="REVIEW"
  add_reason "manual_review_required"
else
  decision="APPROVE"
fi

if [ -z "$reasons" ]; then
  reasons="\"standard_order\""
fi

printf '{"orderId":"%s","decision":"%s","riskScore":%s,"reasons":[%s]}\n' \
  "$order_id" "$decision" "$risk_score" "$reasons"
