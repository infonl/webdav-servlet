## 1. Fix XMLWriter

- [x] 1.1 Add private static `escapeXml(String text)` method to `XMLWriter` that replaces `&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`
- [x] 1.2 Update `writeText(String text)` to call `escapeXml(text)` before appending to `_buffer`
- [x] 1.3 Update `writeProperty(String name, String value)` to call `escapeXml(value)` before appending to `_buffer`

## 2. Test XMLWriter

- [x] 2.1 Create `src/test/java/nl/info/webdav/fromcatalina/XMLWriterTest.java` with SPDX header
- [x] 2.2 Add test: `writeText` with `&` produces `&amp;` in buffer
- [x] 2.3 Add test: `writeText` with `<` produces `&lt;` in buffer
- [x] 2.4 Add test: `writeText` with `>` produces `&gt;` in buffer
- [x] 2.5 Add test: `writeText` with no special characters leaves content unchanged
- [x] 2.6 Add test: `writeProperty(String, String)` with `&` in value produces `&amp;` in XML output
- [x] 2.7 Add test: `writeProperty(String, String)` with `<` in value produces `&lt;` in XML output

## 3. Test AbstractMethod.sendReport

- [x] 3.1 Add test to `AbstractMethodTest`: multi-status response with error path containing `&` produces `&amp;` in XML `<href>` content
- [x] 3.2 Add test to `AbstractMethodTest`: multi-status response with error path containing `<` produces `&lt;` in XML `<href>` content

## 4. Verify

- [x] 4.1 Run `./gradlew spotlessApply` and resolve any style issues
- [x] 4.2 Run `./gradlew test` and confirm all tests pass
