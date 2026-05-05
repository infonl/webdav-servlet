## 1. Fix XMLWriter.writeData

- [x] 1.1 In `XMLWriter.writeData`, replace every `]]>` in `data` with `]]]]><![CDATA[>` before appending to the buffer; handle `null` by treating it as empty string (consistent with `escapeXml`)

## 2. Fix AbstractMethod.sendReport

- [x] 2.1 In `AbstractMethod.sendReport`, add `resp.setContentType("text/xml; charset=UTF-8")` immediately after `resp.setStatus(WebdavStatus.SC_MULTI_STATUS)` and before writing any XML body

## 3. Tests

- [x] 3.1 Add unit test: `writeData` with input containing `]]>` — verify output is well-formed XML and decoded text equals original input
- [x] 3.2 Add unit test: `writeData` with input containing no `]]>` — verify output equals `<![CDATA[<input>]]>` unchanged
- [x] 3.3 Add unit test: `writeData` with `null` — verify output is `<![CDATA[]]>` with no exception
- [x] 3.4 Add unit test: `sendReport` with multiple errors — verify `Content-Type: text/xml; charset=UTF-8` is set on the response

## 4. Verify

- [x] 4.1 Run `./gradlew test` — all tests pass
- [x] 4.2 Run `./gradlew spotlessCheck` — no style violations
