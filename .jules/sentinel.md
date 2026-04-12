## 2024-04-12 - Prevent SSRF and Path Traversal via URL Injection
**Vulnerability:** The Mojang API request URLs were built by blindly concatenating user-supplied identifiers (names and UUIDs) without URL encoding them. This allowed an attacker to supply a crafted string (e.g., containing slashes or URL query parameters) to manipulate the endpoint path or query string, leading to SSRF or path traversal within the API calls.
**Learning:** Whenever incorporating unvalidated or user-controlled input into a URL path or query parameter, especially when calling external APIs, the input must be properly URL-encoded.
**Prevention:** Always use `java.net.URLEncoder.encode(input, StandardCharsets.UTF_8)` or an equivalent secure URL building mechanism for variables interpolated into URLs.
