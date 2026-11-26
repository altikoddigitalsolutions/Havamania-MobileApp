# API uç nokta referansı

26 Kasım 2026 — Depodaki FastAPI OpenAPI sözleşmesinden üretilmiştir. Canlı sunucu doğrulaması değildir.

Ana rehber: [Mevcut API dokümantasyonu](MEVCUT_API_DOKUMANTASYONU.md). Makine tarafından okunabilir sözleşme: [OpenAPI](API_OPENAPI.json).

## GET /health

Healthcheck

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"additionalProperties": {"type": "string"}, "type": "object", "title": "Response Healthcheck Health Get"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/account/deletion/availability

Deletion Availability

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/account/deletion

Delete Account

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

- `authorization` (header), isteğe bağlı: `{"anyOf": [{"type": "string"}, {"type": "null"}], "title": "Authorization"}`

İstek gövdesi:

```json
{
  "required": true,
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/DeletionRequest"
      }
    }
  }
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/auth/signup

Signup Endpoint

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/SignupRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **201**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/TokenResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/auth/login

Login Endpoint

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/LoginRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/TokenResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/auth/refresh

Refresh Endpoint

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/RefreshRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/TokenResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/auth/logout

Logout Endpoint

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/RefreshRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/MessageResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/profile

Get Profile

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/ProfileResponse"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## PATCH /v1/profile

Update Profile

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/ProfileUpdateRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/ProfileResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/profile/locations

List Locations

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"items": {"$ref": "#/components/schemas/LocationResponse"}, "type": "array", "title": "Response List Locations V1 Profile Locations Get"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/profile/locations

Create Location

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/LocationCreateRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **201**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/LocationResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## PATCH /v1/profile/locations/{location_id}

Update Location

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `location_id` (path), zorunlu: `{"type": "string", "title": "Location Id"}`

İstek gövdesi:

```json
{
  "required": true,
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/LocationUpdateRequest"
      }
    }
  }
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/LocationResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## DELETE /v1/profile/locations/{location_id}

Delete Location

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `location_id` (path), zorunlu: `{"type": "string", "title": "Location Id"}`

Belgelenmiş yanıtlar:

- **204**: Successful Response
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/profile/notifications

Get Notification Preferences

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/NotificationPreferenceResponse"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## PATCH /v1/profile/notifications

Update Notification Preferences

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/NotificationPreferenceUpdateRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/NotificationPreferenceResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/profile/avatar

Upload Avatar

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "multipart/form-data": {
      "schema": {
        "$ref": "#/components/schemas/Body_upload_avatar_v1_profile_avatar_post"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/ProfileResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/weather/current

Get Current Weather

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

- `lat` (query), zorunlu: `{"type": "number", "maximum": 90, "minimum": -90, "title": "Lat"}`
- `lon` (query), zorunlu: `{"type": "number", "maximum": 180, "minimum": -180, "title": "Lon"}`

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/WeatherCurrentResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/weather/hourly

Get Hourly Weather

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

- `lat` (query), zorunlu: `{"type": "number", "maximum": 90, "minimum": -90, "title": "Lat"}`
- `lon` (query), zorunlu: `{"type": "number", "maximum": 180, "minimum": -180, "title": "Lon"}`
- `hours` (query), isteğe bağlı: `{"type": "integer", "maximum": 72, "minimum": 1, "default": 24, "title": "Hours"}`

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/WeatherHourlyResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/weather/daily

Get Daily Weather

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

- `lat` (query), zorunlu: `{"type": "number", "maximum": 90, "minimum": -90, "title": "Lat"}`
- `lon` (query), zorunlu: `{"type": "number", "maximum": 180, "minimum": -180, "title": "Lon"}`
- `days` (query), isteğe bağlı: `{"type": "integer", "maximum": 14, "minimum": 1, "default": 7, "title": "Days"}`

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/WeatherDailyResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/weather/map-layers

Get Premium Map Layer

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `lat` (query), zorunlu: `{"type": "number", "maximum": 90, "minimum": -90, "title": "Lat"}`
- `lon` (query), zorunlu: `{"type": "number", "maximum": 180, "minimum": -180, "title": "Lon"}`
- `layer` (query), isteğe bağlı: `{"type": "string", "default": "temperature", "title": "Layer"}`

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"type": "object", "additionalProperties": true, "title": "Response Get Premium Map Layer V1 Weather Map Layers Get"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/alerts

List Alerts

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `location_id` (query), isteğe bağlı: `{"anyOf": [{"type": "string"}, {"type": "null"}], "title": "Location Id"}`
- `severity` (query), isteğe bağlı: `{"anyOf": [{"$ref": "#/components/schemas/AlertSeverity"}, {"type": "null"}], "title": "Severity"}`
- `grouped` (query), isteğe bağlı: `{"type": "boolean", "default": true, "title": "Grouped"}`

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"anyOf": [{"$ref": "#/components/schemas/GroupedAlertsResponse"}, {"type": "array", "items": {"$ref": "#/components/schemas/AlertResponse"}}], "title": "Response List Alerts V1 Alerts Get"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/alerts/{alert_id}

Get Alert

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `alert_id` (path), zorunlu: `{"type": "string", "title": "Alert Id"}`

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/AlertResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/chatbot/ask

Ask Chatbot

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `X-App-Version` (header), isteğe bağlı: `{"anyOf": [{"type": "string", "maxLength": 40}, {"type": "null"}], "title": "X-App-Version"}`

İstek gövdesi:

```json
{
  "required": true,
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/ChatbotAskRequest"
      }
    }
  }
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/ChatbotAskResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/chatbot/usage

Chatbot Usage

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/ChatbotUsageResponse"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/chatbot/ask/premium

Ask Chatbot Premium

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

- `X-App-Version` (header), isteğe bağlı: `{"anyOf": [{"type": "string", "maxLength": 40}, {"type": "null"}], "title": "X-App-Version"}`

İstek gövdesi:

```json
{
  "required": true,
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/ChatbotAskRequest"
      }
    }
  }
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/ChatbotAskResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/chatbot/consent

Get Assistant Consent

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/chatbot/consent

Update Assistant Consent

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/AssistantConsentUpdate"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## GET /v1/subscription/status

Get Subscription Status

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/SubscriptionStatusResponse"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/subscription/validate-receipt

Validate Receipt

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/ValidateReceiptRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/SubscriptionStatusResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/subscription/webhook/store

Store Webhook

Kimlik doğrulama: OpenAPI güvenlik bağımlılığı yok; hesap silme gibi uçlarda özel doğrulama uygulanabilir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/StoreWebhookRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **200**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/SubscriptionStatusResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## POST /v1/devices/push-token

Register Push Token

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/PushTokenRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **201**: Successful Response — `application/json`: `{"$ref": "#/components/schemas/PushTokenResponse"}`
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

## DELETE /v1/devices/push-token

Unregister Push Token

Kimlik doğrulama: Bearer erişim belirteci; ilgili uç noktanın ek yetki koşulları geçerlidir.

İstek gövdesi:

```json
{
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/PushTokenRequest"
      }
    }
  },
  "required": true
}
```

Belgelenmiş yanıtlar:

- **204**: Successful Response
- **422**: Validation Error — `application/json`: `{"$ref": "#/components/schemas/HTTPValidationError"}`

Çalışma zamanında ek hata kodları dönebilir; ana rehberdeki hata notlarına bakın.

# Veri modelleri

Şema adları OpenAPI içindeki `#/components/schemas/…` başvurularıyla eşleşir. Aşağıdaki JSON, alan sınırlarını, zorunlu alanları ve varsayılanları eksiksiz korur.

## AlertResponse

```json
{
  "properties": {
    "id": {
      "type": "string",
      "title": "Id"
    },
    "location_id": {
      "type": "string",
      "title": "Location Id"
    },
    "severity": {
      "type": "string",
      "title": "Severity"
    },
    "title": {
      "type": "string",
      "title": "Title"
    },
    "description": {
      "type": "string",
      "title": "Description"
    },
    "starts_at": {
      "type": "string",
      "format": "date-time",
      "title": "Starts At"
    },
    "ends_at": {
      "type": "string",
      "format": "date-time",
      "title": "Ends At"
    }
  },
  "type": "object",
  "required": [
    "id",
    "location_id",
    "severity",
    "title",
    "description",
    "starts_at",
    "ends_at"
  ],
  "title": "AlertResponse"
}
```

## AlertSeverity

```json
{
  "type": "string",
  "enum": [
    "advisory",
    "active",
    "critical"
  ],
  "title": "AlertSeverity"
}
```

## AssistantClientContext

```json
{
  "properties": {
    "city": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 100
        },
        {
          "type": "null"
        }
      ],
      "title": "City"
    },
    "weather": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 2000
        },
        {
          "type": "null"
        }
      ],
      "title": "Weather"
    },
    "weather_data": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/AssistantWeatherData"
        },
        {
          "type": "null"
        }
      ]
    },
    "trips": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 2000
        },
        {
          "type": "null"
        }
      ],
      "title": "Trips"
    }
  },
  "additionalProperties": false,
  "type": "object",
  "title": "AssistantClientContext"
}
```

## AssistantConsentUpdate

```json
{
  "properties": {
    "assistant_accepted": {
      "type": "boolean",
      "title": "Assistant Accepted"
    },
    "consent_version": {
      "anyOf": [
        {
          "type": "integer",
          "minimum": 1.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Consent Version"
    },
    "consent_hash": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^[0-9a-f]{64}$"
        },
        {
          "type": "null"
        }
      ],
      "title": "Consent Hash"
    },
    "health_accepted": {
      "type": "boolean",
      "title": "Health Accepted",
      "default": false
    },
    "health_consent_version": {
      "anyOf": [
        {
          "type": "integer",
          "minimum": 1.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Health Consent Version"
    },
    "health_consent_hash": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^[0-9a-f]{64}$"
        },
        {
          "type": "null"
        }
      ],
      "title": "Health Consent Hash"
    }
  },
  "type": "object",
  "required": [
    "assistant_accepted"
  ],
  "title": "AssistantConsentUpdate"
}
```

## AssistantWeatherData

```json
{
  "properties": {
    "observed_at": {
      "type": "string",
      "title": "Observed At"
    },
    "timezone": {
      "type": "string",
      "maxLength": 64,
      "title": "Timezone"
    },
    "location": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/WeatherLocation"
        },
        {
          "type": "null"
        }
      ]
    },
    "current": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/CurrentWeather"
        },
        {
          "type": "null"
        }
      ]
    },
    "forecast": {
      "anyOf": [
        {
          "items": {
            "$ref": "#/components/schemas/ForecastWeather"
          },
          "type": "array",
          "maxItems": 10
        },
        {
          "type": "null"
        }
      ],
      "title": "Forecast"
    },
    "stale": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ],
      "title": "Stale"
    },
    "source": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 100
        },
        {
          "type": "null"
        }
      ],
      "title": "Source"
    }
  },
  "type": "object",
  "required": [
    "observed_at",
    "timezone"
  ],
  "title": "AssistantWeatherData"
}
```

## Body_upload_avatar_v1_profile_avatar_post

```json
{
  "properties": {
    "file": {
      "type": "string",
      "format": "binary",
      "title": "File"
    }
  },
  "type": "object",
  "required": [
    "file"
  ],
  "title": "Body_upload_avatar_v1_profile_avatar_post"
}
```

## ChatbotAskRequest

```json
{
  "properties": {
    "conversation_id": {
      "anyOf": [
        {
          "type": "string",
          "format": "uuid"
        },
        {
          "type": "null"
        }
      ],
      "title": "Conversation Id"
    },
    "mode": {
      "type": "string",
      "enum": [
        "genel",
        "agro",
        "fly"
      ],
      "title": "Mode",
      "default": "genel"
    },
    "tone": {
      "anyOf": [
        {
          "type": "string",
          "enum": [
            "samimi",
            "resmi",
            "dengeli",
            "kisa",
            "uzman"
          ]
        },
        {
          "type": "null"
        }
      ],
      "title": "Tone"
    },
    "personalization_enabled": {
      "type": "boolean",
      "title": "Personalization Enabled",
      "default": false
    },
    "selected_interests": {
      "anyOf": [
        {
          "items": {
            "type": "string",
            "maxLength": 50
          },
          "type": "array",
          "maxItems": 20
        },
        {
          "type": "null"
        }
      ],
      "title": "Selected Interests"
    },
    "context": {
      "anyOf": [
        {
          "$ref": "#/components/schemas/AssistantClientContext"
        },
        {
          "type": "null"
        }
      ]
    },
    "request_id": {
      "anyOf": [
        {
          "type": "string",
          "format": "uuid"
        },
        {
          "type": "null"
        }
      ],
      "title": "Request Id"
    },
    "consent_version": {
      "anyOf": [
        {
          "type": "integer",
          "minimum": 1.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Consent Version"
    },
    "health_consent": {
      "type": "boolean",
      "title": "Health Consent",
      "default": false
    },
    "health_consent_version": {
      "anyOf": [
        {
          "type": "integer",
          "minimum": 1.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Health Consent Version"
    },
    "question": {
      "type": "string",
      "maxLength": 4000,
      "minLength": 1,
      "title": "Question"
    }
  },
  "type": "object",
  "required": [
    "question"
  ],
  "title": "ChatbotAskRequest"
}
```

## ChatbotAskResponse

```json
{
  "properties": {
    "conversation_id": {
      "anyOf": [
        {
          "type": "string",
          "format": "uuid"
        },
        {
          "type": "null"
        }
      ],
      "title": "Conversation Id"
    },
    "message_id": {
      "anyOf": [
        {
          "type": "string",
          "format": "uuid"
        },
        {
          "type": "null"
        }
      ],
      "title": "Message Id"
    },
    "disclaimer": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Disclaimer"
    },
    "answer": {
      "type": "string",
      "title": "Answer"
    },
    "used_messages_today": {
      "type": "integer",
      "title": "Used Messages Today"
    },
    "remaining_messages_today": {
      "type": "integer",
      "title": "Remaining Messages Today"
    },
    "is_premium": {
      "type": "boolean",
      "title": "Is Premium"
    }
  },
  "type": "object",
  "required": [
    "answer",
    "used_messages_today",
    "remaining_messages_today",
    "is_premium"
  ],
  "title": "ChatbotAskResponse"
}
```

## ChatbotUsageResponse

```json
{
  "properties": {
    "used_messages_today": {
      "type": "integer",
      "title": "Used Messages Today"
    },
    "remaining_messages_today": {
      "type": "integer",
      "title": "Remaining Messages Today"
    },
    "daily_limit": {
      "type": "integer",
      "title": "Daily Limit"
    },
    "is_premium": {
      "type": "boolean",
      "title": "Is Premium"
    }
  },
  "type": "object",
  "required": [
    "used_messages_today",
    "remaining_messages_today",
    "daily_limit",
    "is_premium"
  ],
  "title": "ChatbotUsageResponse"
}
```

## CurrentWeather

```json
{
  "properties": {
    "temp_c": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 60.0,
          "minimum": -90.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Temp C"
    },
    "feels_like_c": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 70.0,
          "minimum": -90.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Feels Like C"
    },
    "condition": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 100
        },
        {
          "type": "null"
        }
      ],
      "title": "Condition"
    },
    "humidity_pct": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 100.0,
          "minimum": 0.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Humidity Pct"
    },
    "wind_kmh": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 500.0,
          "minimum": 0.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Wind Kmh"
    },
    "precip_mm": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 2000.0,
          "minimum": 0.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Precip Mm"
    }
  },
  "type": "object",
  "title": "CurrentWeather"
}
```

## DeletionRequest

```json
{
  "properties": {
    "continuation_key": {
      "type": "string",
      "pattern": "^[0-9a-f]{64}$",
      "title": "Continuation Key"
    }
  },
  "type": "object",
  "required": [
    "continuation_key"
  ],
  "title": "DeletionRequest"
}
```

## ForecastWeather

```json
{
  "properties": {
    "date": {
      "type": "string",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}$",
      "title": "Date"
    },
    "min_c": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 60.0,
          "minimum": -90.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Min C"
    },
    "max_c": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 70.0,
          "minimum": -90.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Max C"
    },
    "condition": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 100
        },
        {
          "type": "null"
        }
      ],
      "title": "Condition"
    },
    "precip_prob_pct": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 100.0,
          "minimum": 0.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Precip Prob Pct"
    }
  },
  "type": "object",
  "required": [
    "date"
  ],
  "title": "ForecastWeather"
}
```

## GroupedAlertsResponse

```json
{
  "properties": {
    "critical": {
      "items": {
        "$ref": "#/components/schemas/AlertResponse"
      },
      "type": "array",
      "title": "Critical"
    },
    "active": {
      "items": {
        "$ref": "#/components/schemas/AlertResponse"
      },
      "type": "array",
      "title": "Active"
    },
    "advisory": {
      "items": {
        "$ref": "#/components/schemas/AlertResponse"
      },
      "type": "array",
      "title": "Advisory"
    }
  },
  "type": "object",
  "required": [
    "critical",
    "active",
    "advisory"
  ],
  "title": "GroupedAlertsResponse"
}
```

## HTTPValidationError

```json
{
  "properties": {
    "detail": {
      "items": {
        "$ref": "#/components/schemas/ValidationError"
      },
      "type": "array",
      "title": "Detail"
    }
  },
  "type": "object",
  "title": "HTTPValidationError"
}
```

## LocationCreateRequest

```json
{
  "properties": {
    "label": {
      "type": "string",
      "maxLength": 255,
      "minLength": 1,
      "title": "Label"
    },
    "lat": {
      "type": "number",
      "maximum": 90.0,
      "minimum": -90.0,
      "title": "Lat"
    },
    "lon": {
      "type": "number",
      "maximum": 180.0,
      "minimum": -180.0,
      "title": "Lon"
    },
    "is_primary": {
      "type": "boolean",
      "title": "Is Primary",
      "default": false
    },
    "is_tracking_enabled": {
      "type": "boolean",
      "title": "Is Tracking Enabled",
      "default": true
    }
  },
  "type": "object",
  "required": [
    "label",
    "lat",
    "lon"
  ],
  "title": "LocationCreateRequest"
}
```

## LocationResponse

```json
{
  "properties": {
    "id": {
      "type": "string",
      "title": "Id"
    },
    "user_id": {
      "type": "string",
      "title": "User Id"
    },
    "label": {
      "type": "string",
      "title": "Label"
    },
    "lat": {
      "type": "number",
      "title": "Lat"
    },
    "lon": {
      "type": "number",
      "title": "Lon"
    },
    "is_primary": {
      "type": "boolean",
      "title": "Is Primary"
    },
    "is_tracking_enabled": {
      "type": "boolean",
      "title": "Is Tracking Enabled"
    }
  },
  "type": "object",
  "required": [
    "id",
    "user_id",
    "label",
    "lat",
    "lon",
    "is_primary",
    "is_tracking_enabled"
  ],
  "title": "LocationResponse"
}
```

## LocationUpdateRequest

```json
{
  "properties": {
    "label": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 255,
          "minLength": 1
        },
        {
          "type": "null"
        }
      ],
      "title": "Label"
    },
    "lat": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 90.0,
          "minimum": -90.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Lat"
    },
    "lon": {
      "anyOf": [
        {
          "type": "number",
          "maximum": 180.0,
          "minimum": -180.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Lon"
    },
    "is_primary": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ],
      "title": "Is Primary"
    },
    "is_tracking_enabled": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ],
      "title": "Is Tracking Enabled"
    }
  },
  "type": "object",
  "title": "LocationUpdateRequest"
}
```

## LoginRequest

```json
{
  "properties": {
    "email": {
      "type": "string",
      "format": "email",
      "title": "Email"
    },
    "password": {
      "type": "string",
      "maxLength": 128,
      "minLength": 8,
      "title": "Password"
    }
  },
  "type": "object",
  "required": [
    "email",
    "password"
  ],
  "title": "LoginRequest"
}
```

## MessageResponse

```json
{
  "properties": {
    "detail": {
      "type": "string",
      "title": "Detail"
    }
  },
  "type": "object",
  "required": [
    "detail"
  ],
  "title": "MessageResponse"
}
```

## NotificationPreferenceResponse

```json
{
  "properties": {
    "user_id": {
      "type": "string",
      "title": "User Id"
    },
    "severe_alert_enabled": {
      "type": "boolean",
      "title": "Severe Alert Enabled"
    },
    "daily_summary_enabled": {
      "type": "boolean",
      "title": "Daily Summary Enabled"
    },
    "rain_alert_enabled": {
      "type": "boolean",
      "title": "Rain Alert Enabled"
    }
  },
  "type": "object",
  "required": [
    "user_id",
    "severe_alert_enabled",
    "daily_summary_enabled",
    "rain_alert_enabled"
  ],
  "title": "NotificationPreferenceResponse"
}
```

## NotificationPreferenceUpdateRequest

```json
{
  "properties": {
    "severe_alert_enabled": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ],
      "title": "Severe Alert Enabled"
    },
    "daily_summary_enabled": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ],
      "title": "Daily Summary Enabled"
    },
    "rain_alert_enabled": {
      "anyOf": [
        {
          "type": "boolean"
        },
        {
          "type": "null"
        }
      ],
      "title": "Rain Alert Enabled"
    }
  },
  "type": "object",
  "title": "NotificationPreferenceUpdateRequest"
}
```

## ProfileResponse

```json
{
  "properties": {
    "user_id": {
      "type": "string",
      "title": "User Id"
    },
    "primary_location_id": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Primary Location Id"
    },
    "temperature_unit": {
      "type": "string",
      "title": "Temperature Unit"
    },
    "wind_unit": {
      "type": "string",
      "title": "Wind Unit"
    },
    "theme": {
      "type": "string",
      "title": "Theme"
    },
    "language": {
      "type": "string",
      "title": "Language",
      "default": "tr"
    },
    "avatar_emoji": {
      "type": "string",
      "title": "Avatar Emoji",
      "default": "🧑"
    },
    "avatar_url": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Avatar Url"
    },
    "interest": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Interest"
    },
    "health_sensitivities": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Health Sensitivities"
    },
    "travel_preferences": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Travel Preferences"
    },
    "activity_types": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Activity Types"
    },
    "assistant_tone": {
      "type": "string",
      "title": "Assistant Tone",
      "default": "friendly"
    }
  },
  "type": "object",
  "required": [
    "user_id",
    "primary_location_id",
    "temperature_unit",
    "wind_unit",
    "theme"
  ],
  "title": "ProfileResponse"
}
```

## ProfileUpdateRequest

```json
{
  "properties": {
    "temperature_unit": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^(C|F)$"
        },
        {
          "type": "null"
        }
      ],
      "title": "Temperature Unit"
    },
    "wind_unit": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^(kmh|mph)$"
        },
        {
          "type": "null"
        }
      ],
      "title": "Wind Unit"
    },
    "theme": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^(light|dark|system)$"
        },
        {
          "type": "null"
        }
      ],
      "title": "Theme"
    },
    "primary_location_id": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Primary Location Id"
    },
    "language": {
      "anyOf": [
        {
          "type": "string",
          "pattern": "^(tr|en)$"
        },
        {
          "type": "null"
        }
      ],
      "title": "Language"
    },
    "avatar_emoji": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 16
        },
        {
          "type": "null"
        }
      ],
      "title": "Avatar Emoji"
    },
    "avatar_url": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 255
        },
        {
          "type": "null"
        }
      ],
      "title": "Avatar Url"
    },
    "interest": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Interest"
    },
    "health_sensitivities": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Health Sensitivities"
    },
    "travel_preferences": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Travel Preferences"
    },
    "activity_types": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Activity Types"
    },
    "assistant_tone": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 32
        },
        {
          "type": "null"
        }
      ],
      "title": "Assistant Tone"
    }
  },
  "type": "object",
  "title": "ProfileUpdateRequest"
}
```

## PushTokenRequest

```json
{
  "properties": {
    "platform": {
      "type": "string",
      "pattern": "^(ios|android)$",
      "title": "Platform"
    },
    "token": {
      "type": "string",
      "maxLength": 512,
      "minLength": 10,
      "title": "Token"
    },
    "installation_id": {
      "anyOf": [
        {
          "type": "string",
          "format": "uuid"
        },
        {
          "type": "null"
        }
      ],
      "title": "Installation Id"
    },
    "revision": {
      "anyOf": [
        {
          "type": "integer",
          "maximum": 9007199254740991.0,
          "minimum": 1.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Revision"
    },
    "owner_protocol": {
      "anyOf": [
        {
          "type": "integer",
          "maximum": 1.0,
          "minimum": 1.0
        },
        {
          "type": "null"
        }
      ],
      "title": "Owner Protocol"
    }
  },
  "type": "object",
  "required": [
    "platform",
    "token"
  ],
  "title": "PushTokenRequest"
}
```

## PushTokenResponse

```json
{
  "properties": {
    "id": {
      "type": "string",
      "title": "Id"
    },
    "user_id": {
      "type": "string",
      "title": "User Id"
    },
    "platform": {
      "type": "string",
      "title": "Platform"
    },
    "token": {
      "type": "string",
      "title": "Token"
    },
    "installation_id": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Installation Id"
    },
    "revision": {
      "anyOf": [
        {
          "type": "integer"
        },
        {
          "type": "null"
        }
      ],
      "title": "Revision"
    },
    "owner_protocol": {
      "type": "integer",
      "title": "Owner Protocol",
      "default": 0
    }
  },
  "type": "object",
  "required": [
    "id",
    "user_id",
    "platform",
    "token"
  ],
  "title": "PushTokenResponse"
}
```

## RefreshRequest

```json
{
  "properties": {
    "refresh_token": {
      "type": "string",
      "title": "Refresh Token"
    }
  },
  "type": "object",
  "required": [
    "refresh_token"
  ],
  "title": "RefreshRequest"
}
```

## SignupRequest

```json
{
  "properties": {
    "email": {
      "type": "string",
      "format": "email",
      "title": "Email"
    },
    "password": {
      "type": "string",
      "maxLength": 128,
      "minLength": 8,
      "title": "Password"
    },
    "full_name": {
      "anyOf": [
        {
          "type": "string",
          "maxLength": 255
        },
        {
          "type": "null"
        }
      ],
      "title": "Full Name"
    }
  },
  "type": "object",
  "required": [
    "email",
    "password"
  ],
  "title": "SignupRequest"
}
```

## StoreWebhookRequest

```json
{
  "properties": {
    "user_id": {
      "type": "string",
      "title": "User Id"
    },
    "plan_code": {
      "type": "string",
      "title": "Plan Code"
    },
    "status": {
      "type": "string",
      "title": "Status"
    },
    "expires_at": {
      "anyOf": [
        {
          "type": "string",
          "format": "date-time"
        },
        {
          "type": "null"
        }
      ],
      "title": "Expires At"
    },
    "store": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Store"
    },
    "original_transaction_id": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Original Transaction Id"
    }
  },
  "type": "object",
  "required": [
    "user_id",
    "plan_code",
    "status"
  ],
  "title": "StoreWebhookRequest"
}
```

## SubscriptionStatusResponse

```json
{
  "properties": {
    "plan_code": {
      "type": "string",
      "title": "Plan Code"
    },
    "status": {
      "type": "string",
      "title": "Status"
    },
    "expires_at": {
      "anyOf": [
        {
          "type": "string",
          "format": "date-time"
        },
        {
          "type": "null"
        }
      ],
      "title": "Expires At"
    },
    "is_premium_active": {
      "type": "boolean",
      "title": "Is Premium Active"
    }
  },
  "type": "object",
  "required": [
    "plan_code",
    "status",
    "expires_at",
    "is_premium_active"
  ],
  "title": "SubscriptionStatusResponse"
}
```

## TokenResponse

```json
{
  "properties": {
    "access_token": {
      "type": "string",
      "title": "Access Token"
    },
    "refresh_token": {
      "type": "string",
      "title": "Refresh Token"
    },
    "token_type": {
      "type": "string",
      "title": "Token Type",
      "default": "bearer"
    }
  },
  "type": "object",
  "required": [
    "access_token",
    "refresh_token"
  ],
  "title": "TokenResponse"
}
```

## ValidateReceiptRequest

```json
{
  "properties": {
    "store": {
      "type": "string",
      "pattern": "^(ios|android)$",
      "title": "Store"
    },
    "receipt_data": {
      "type": "string",
      "title": "Receipt Data"
    },
    "plan_code": {
      "type": "string",
      "title": "Plan Code",
      "default": "premium_monthly"
    }
  },
  "type": "object",
  "required": [
    "store",
    "receipt_data"
  ],
  "title": "ValidateReceiptRequest"
}
```

## ValidationError

```json
{
  "properties": {
    "loc": {
      "items": {
        "anyOf": [
          {
            "type": "string"
          },
          {
            "type": "integer"
          }
        ]
      },
      "type": "array",
      "title": "Location"
    },
    "msg": {
      "type": "string",
      "title": "Message"
    },
    "type": {
      "type": "string",
      "title": "Error Type"
    },
    "input": {
      "title": "Input"
    },
    "ctx": {
      "type": "object",
      "title": "Context"
    }
  },
  "type": "object",
  "required": [
    "loc",
    "msg",
    "type"
  ],
  "title": "ValidationError"
}
```

## WeatherCurrentResponse

```json
{
  "properties": {
    "location": {
      "additionalProperties": true,
      "type": "object",
      "title": "Location"
    },
    "temperature": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Temperature"
    },
    "humidity": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Humidity"
    },
    "feels_like": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Feels Like"
    },
    "wind_speed": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Wind Speed"
    },
    "wind_direction": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Wind Direction"
    },
    "uv_index": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Uv Index"
    },
    "pressure": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Pressure"
    },
    "visibility": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Visibility"
    },
    "cloud_cover": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Cloud Cover"
    },
    "sunrise": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Sunrise"
    },
    "sunset": {
      "anyOf": [
        {
          "type": "string"
        },
        {
          "type": "null"
        }
      ],
      "title": "Sunset"
    },
    "provider": {
      "type": "string",
      "title": "Provider"
    }
  },
  "type": "object",
  "required": [
    "location",
    "temperature",
    "humidity",
    "feels_like",
    "wind_speed",
    "wind_direction",
    "uv_index",
    "pressure",
    "visibility",
    "cloud_cover",
    "provider"
  ],
  "title": "WeatherCurrentResponse"
}
```

## WeatherDailyItem

```json
{
  "properties": {
    "date": {
      "type": "string",
      "title": "Date"
    },
    "temp_max": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Temp Max"
    },
    "temp_min": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Temp Min"
    },
    "precipitation_probability": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Precipitation Probability"
    },
    "weather_code": {
      "anyOf": [
        {
          "type": "integer"
        },
        {
          "type": "null"
        }
      ],
      "title": "Weather Code"
    }
  },
  "type": "object",
  "required": [
    "date",
    "temp_max",
    "temp_min",
    "precipitation_probability",
    "weather_code"
  ],
  "title": "WeatherDailyItem"
}
```

## WeatherDailyResponse

```json
{
  "properties": {
    "location": {
      "additionalProperties": true,
      "type": "object",
      "title": "Location"
    },
    "days": {
      "type": "integer",
      "title": "Days"
    },
    "items": {
      "items": {
        "$ref": "#/components/schemas/WeatherDailyItem"
      },
      "type": "array",
      "title": "Items"
    },
    "provider": {
      "type": "string",
      "title": "Provider"
    }
  },
  "type": "object",
  "required": [
    "location",
    "days",
    "items",
    "provider"
  ],
  "title": "WeatherDailyResponse"
}
```

## WeatherHourlyItem

```json
{
  "properties": {
    "time": {
      "type": "string",
      "title": "Time"
    },
    "temperature": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Temperature"
    },
    "precipitation_probability": {
      "anyOf": [
        {
          "type": "number"
        },
        {
          "type": "null"
        }
      ],
      "title": "Precipitation Probability"
    },
    "weather_code": {
      "anyOf": [
        {
          "type": "integer"
        },
        {
          "type": "null"
        }
      ],
      "title": "Weather Code"
    }
  },
  "type": "object",
  "required": [
    "time",
    "temperature",
    "precipitation_probability",
    "weather_code"
  ],
  "title": "WeatherHourlyItem"
}
```

## WeatherHourlyResponse

```json
{
  "properties": {
    "location": {
      "additionalProperties": true,
      "type": "object",
      "title": "Location"
    },
    "hours": {
      "type": "integer",
      "title": "Hours"
    },
    "items": {
      "items": {
        "$ref": "#/components/schemas/WeatherHourlyItem"
      },
      "type": "array",
      "title": "Items"
    },
    "provider": {
      "type": "string",
      "title": "Provider"
    }
  },
  "type": "object",
  "required": [
    "location",
    "hours",
    "items",
    "provider"
  ],
  "title": "WeatherHourlyResponse"
}
```

## WeatherLocation

```json
{
  "properties": {
    "name": {
      "type": "string",
      "maxLength": 100,
      "minLength": 1,
      "title": "Name"
    }
  },
  "type": "object",
  "required": [
    "name"
  ],
  "title": "WeatherLocation"
}
```
