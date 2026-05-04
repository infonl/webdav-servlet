## 1. Harden DocumentBuilder factory

- [x] 1.1 In `AbstractMethod.getDocumentBuilder()`, add the four `setFeature()` calls to disable DTDs and external entities (OWASP set: `disallow-doctype-decl`, `external-general-entities`, `external-parameter-entities`, `load-external-dtd`)
- [x] 1.2 Add `setXIncludeAware(false)` and `setExpandEntityReferences(false)` to the factory before creating the builder
- [x] 1.3 Update the SPDX copyright line in `AbstractMethod.java` to append `, 2026 INFO.nl`

## 2. Refactor DoProppatch

- [x] 2.1 Replace the inline `DocumentBuilderFactory` setup in `DoProppatch.execute()` (lines 128–138) with a call to `getDocumentBuilder()` from `AbstractMethod`
- [x] 2.2 Remove the now-unused `javax.xml.parsers.DocumentBuilder` and `javax.xml.parsers.DocumentBuilderFactory` imports from `DoProppatch.java`
- [x] 2.3 Update the SPDX copyright line in `DoProppatch.java` to append `, 2026 INFO.nl`

## 3. Unit tests

- [x] 3.1 In `AbstractMethodTest` (or a new `AbstractMethodXxeTest`), add a test that feeds a DOCTYPE-containing XML input to `getDocumentBuilder().parse(...)` and asserts a `SAXParseException` is thrown (verifying DTD blocking)
- [x] 3.2 Add a test that parses a well-formed PROPFIND body (no DOCTYPE) and asserts it succeeds, confirming legitimate traffic is unaffected

## 4. Verify & clean up

- [x] 4.1 Run `./gradlew test` and confirm all tests pass
- [x] 4.2 Run `./gradlew spotlessCheck` and fix any style issues with `./gradlew spotlessApply`
