## 1. Add HTML escaping utility

- [x] 1.1 Add `private static String escapeHtml(String input)` method to `DoGet` that replaces `&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`, `"` → `&quot;`, `'` → `&#x27;`

## 2. Fix XSS injection points in `folderBody`

- [x] 2.1 Wrap the `path` value appended to the `<title>` element with `escapeHtml`
- [x] 2.2 Wrap the `child` name appended as the `href` attribute value with `escapeHtml`
- [x] 2.3 Wrap the `child` name appended as link text with `escapeHtml`

## 3. Fix XSS injection point in `getHeader`

- [x] 3.1 Wrap the `path` value interpolated into the `<h1>` in the default `getHeader` implementation with `escapeHtml`

## 4. Tests

- [x] 4.1 Add unit test in `DoGetTest` covering `folderBody` with a path containing `<script>` — assert response body does not contain raw `<script>` tag
- [x] 4.2 Add unit test in `DoGetTest` covering `folderBody` with a child name containing `<script>` — assert link text and href are properly encoded
- [x] 4.3 Run `./gradlew test` and confirm all tests pass

## 5. Code style and build

- [x] 5.1 Run `./gradlew spotlessApply` to enforce formatter
- [x] 5.2 Run `./gradlew build` and confirm clean build
