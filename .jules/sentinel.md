## 2024-04-04 - URL Injection Risk in External API Calls
**Vulnerability:** User-controlled strings (`identifier`, `uuid`) were concatenated directly into Mojang API HTTP request URLs in `SkinManager.java`, presenting a risk of URL injection or SSRF.
**Learning:** Even if data enters from a source that is typically expected to be safe or lightly validated (like command arguments), any user-controlled parameter bound for a network request must be correctly encoded to prevent path traversal or parameter injection on external APIs.
**Prevention:** Always use `java.net.URLEncoder.encode(input, StandardCharsets.UTF_8)` when incorporating arbitrary strings into URL paths or query string parameters.
