
Flow:
curl → poc-sm-crypto:8089 (seed data) → poc-sm-crypto builds EPB locally → poc-sm-crypto posts to payment-gateway:8221 (issuer-side verify) → returns gateway’s result.

Below are the drop-in changes for poc-sm-crypto and the cURL you asked for.

cURL into 8089 (POS/ICC sim)

1) Build + forward for verification (single call)

# Verify: seed → local EPB → forward to payment-gateway:/gateway/process
✅curl -sS -X POST http://localhost:8089/api/pin/verify \
  -H 'Content-Type: application/json' \
  -d '{
    "pan": "5081470082639564",
    "pin": "1234",
    "keySlot": "9001",
    "translateTo": { "pinFormatOut": "CLEAR-ISO-0" }
  }' | jq


curl -sS -X POST http://localhost:8089/api/pin/verify \
  -H 'Content-Type: application/json' \
  -d '{
    "pan": "5081470082639564",
    "pin": "1234",

    // choose ONE way to identify the ZPK used by the POS
    "keySlot": "9001",
    // "keyName": "ZPK-POS1",
    // "zpkHex": "0123456789ABCDEFFEDCBA9876543210"

    // optional: ask issuer to translate to clear (useful for debugging)
    "translateTo": { "pinFormatOut": "CLEAR-ISO-0" }
  }' | jq

What happens:
poc-sm-crypto builds ISO-0 EPB under the provided ZPK (slot/name/hex), then POSTs to payment-gateway:8221 /emv/empt with:

{
  "pan": "...",
  "pinBlock": "<EPB_HEX>",
  "pinFormatIn": "ISO-0",
  "zpkRef": { "keySlot": "9001" },  // or { "keyName": "..." } or { "keyValue": "<zpkHex>" }
  "translateTo": { "pinFormatOut": "CLEAR-ISO-0" }
}

2) (Optional) Just build EPB locally (no forward)

✅curl -sS -X POST http://localhost:8089/api/pin/encrypt \
  -H 'Content-Type: application/json' \
  -d '{
    "pan": "5081470082639564",
    "pin": "1234",
    "zpkHex": "0123456789ABCDEFFEDCBA9876543210"
  }' | jq

curl -sS -X POST http://localhost:8089/api/pin/encrypt \
  -H 'Content-Type: application/json' \
  -d '{
    "pan": "5081470082639564",
    "pin": "1234",
    "zpkHex": "0123456789ABCDEFFEDCBA9876543210"
  }' | jq



