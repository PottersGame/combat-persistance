## 2024-05-24 - URL Injection in API Calls
**Vulnerability:** User-provided skin identifiers and player UUIDs were directly concatenated into Mojang API URLs without URL encoding in `SkinManager.java`.
**Learning:** This could allow attackers to manipulate the API request URL structure (e.g., injecting query parameters or altering the path) if they could control the identifier input.
**Prevention:** Always URL-encode user-controlled data using `java.net.URLEncoder.encode(input, StandardCharsets.UTF_8)` before embedding it in URLs.
