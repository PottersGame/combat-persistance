## 2024-04-14 - Fix URL Injection / SSRF in SkinManager
**Vulnerability:** The application was vulnerable to URL injection and potential Server-Side Request Forgery (SSRF) because user-provided skin names and UUIDs were concatenated directly into API URLs without URL encoding. A malicious user could craft an identifier that includes query parameters or path traversal sequences.
**Learning:** External API URLs constructed using dynamic user input must always sanitize or URL-encode the input. The memory instructed: "always URL-encode user-controlled identifiers... before incorporating them into URL strings".
**Prevention:** Use `java.net.URLEncoder.encode(input, StandardCharsets.UTF_8)` whenever constructing URLs containing user input.
