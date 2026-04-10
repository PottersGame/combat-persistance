## 2026-04-10 - Thread Starvation DoS & URL Injection
**Vulnerability:** Blocking operations (I/O, hashing) were running on the default ForkJoinPool, and user inputs were appended to URLs without encoding.
**Learning:** Default async pools can be easily exhausted, and APIs without input sanitization are vulnerable to injection.
**Prevention:** Use dedicated executors for I/O and always URL-encode user input before using it in URLs.
