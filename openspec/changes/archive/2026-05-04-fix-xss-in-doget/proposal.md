## Why

GitHub CodeQL scanning flagged a stored/reflected XSS vulnerability (code-scanning alert #16) in `DoGet.folderBody`: path segments and child resource names are interpolated directly into HTML output without encoding, allowing a malicious store backend or path value to inject arbitrary HTML/JavaScript.

## What Changes

- HTML-encode all user-controlled values (path, child names) before writing them into the HTML directory listing in `DoGet`
- HTML-encode the path value in the default `getHeader()` implementation
- Introduce a shared HTML escaping utility (or use an existing one already on the classpath)

## Capabilities

### New Capabilities
- `html-safe-directory-listing`: Directory listing HTML output escapes all user-supplied values to prevent XSS

### Modified Capabilities

## Impact

- `DoGet.folderBody` — path and child name interpolations must be escaped
- `DoGet.getHeader` — path interpolation must be escaped
- No API or interface changes; `IWebdavStore` and `IMethodExecutor` contracts unchanged
- No new runtime dependencies required (Apache Commons Text or hand-rolled escaper both viable)
