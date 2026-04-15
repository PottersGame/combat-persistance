## 2024-05-18 - Prevent SSRF and URL Injection in External API Calls
**Vulnerability:** Unsanitized user inputs (Minecraft usernames and UUIDs) were concatenated directly into URLs for Mojang API requests in `SkinManager.java`.
**Learning:** This practice can lead to URL injection or Server-Side Request Forgery (SSRF) if a user manages to provide a maliciously crafted username or UUID, potentially altering the API endpoint or query parameters accessed by the server.
**Prevention:** Always sanitize and URL-encode user-controlled identifiers using `java.net.URLEncoder.encode(input, StandardCharsets.UTF_8)` before incorporating them into URLs.
