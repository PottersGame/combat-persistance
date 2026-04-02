## 2024-04-02 - Server-Side Request Forgery in Mojang API
**Vulnerability:** Unsanitized user input from the `/skin` command is directly concatenated into Mojang API URLs in `SkinManager.java`, creating a Server-Side Request Forgery (SSRF) and URL injection vulnerability.
**Learning:** Developers assumed player names and identifiers were inherently safe or constrained, forgetting that command arguments can contain arbitrary strings that break URL structures.
**Prevention:** Always URL-encode user-controlled identifiers using `java.net.URLEncoder.encode(input, StandardCharsets.UTF_8)` before incorporating them into external API calls.
