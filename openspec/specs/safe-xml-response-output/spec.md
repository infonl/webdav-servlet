## Requirements

### Requirement: XMLWriter escapes XML special characters in text content
`XMLWriter.writeText(String)` SHALL XML-escape `&`, `<`, and `>` in the supplied string before appending it to the output buffer, so that user-controlled values cannot inject XML markup.

#### Scenario: Text containing ampersand is escaped
- **WHEN** `writeText` is called with a string containing `&`
- **THEN** the buffer SHALL contain `&amp;` in place of `&`

#### Scenario: Text containing less-than is escaped
- **WHEN** `writeText` is called with a string containing `<`
- **THEN** the buffer SHALL contain `&lt;` in place of `<`

#### Scenario: Text containing greater-than is escaped
- **WHEN** `writeText` is called with a string containing `>`
- **THEN** the buffer SHALL contain `&gt;` in place of `>`

#### Scenario: Text containing no special characters is unchanged
- **WHEN** `writeText` is called with a string that contains no `&`, `<`, or `>`
- **THEN** the buffer SHALL contain the string unchanged

### Requirement: XMLWriter escapes XML special characters in property values
`XMLWriter.writeProperty(String, String)` SHALL XML-escape `&`, `<`, and `>` in the value argument before appending it to the output buffer.

#### Scenario: Property value containing ampersand is escaped
- **WHEN** `writeProperty` is called with a value containing `&`
- **THEN** the rendered XML element content SHALL contain `&amp;` in place of `&`

#### Scenario: Property value containing less-than is escaped
- **WHEN** `writeProperty` is called with a value containing `<`
- **THEN** the rendered XML element content SHALL contain `&lt;` in place of `<`

### Requirement: sendReport XML-escapes error paths in multi-status response
`AbstractMethod.sendReport()` SHALL produce a well-formed XML multi-status response even when error paths contain XML special characters, because `writeText` now escapes them.

#### Scenario: Error path with ampersand in multi-status response
- **WHEN** `sendReport` is called with an error list whose path contains `&`
- **THEN** the XML response SHALL contain `&amp;` in the `<href>` element and the response SHALL be parseable as well-formed XML

#### Scenario: Error path with angle brackets in multi-status response
- **WHEN** `sendReport` is called with an error list whose path contains `<` or `>`
- **THEN** the XML response SHALL contain `&lt;` or `&gt;` in the `<href>` element and the response SHALL be parseable as well-formed XML

### Requirement: XMLWriter prevents CDATA injection in writeData
`XMLWriter.writeData(String)` SHALL escape the CDATA terminator sequence `]]>` in the supplied string by replacing every occurrence with `]]]]><![CDATA[>` before appending it to the output buffer, so that user-controlled values cannot inject XML markup by closing the CDATA section prematurely.

#### Scenario: Data containing CDATA terminator is escaped
- **WHEN** `writeData` is called with a string containing `]]>`
- **THEN** the buffer SHALL NOT contain a raw `]]>` that closes the wrapping CDATA section, and the full output SHALL be parseable as well-formed XML

#### Scenario: Data containing CDATA terminator produces semantically equivalent content
- **WHEN** `writeData` is called with a string containing `]]>`
- **THEN** the text content of the resulting XML element, after parsing, SHALL equal the original input string

#### Scenario: Data containing no CDATA terminator is unchanged
- **WHEN** `writeData` is called with a string that does not contain `]]>`
- **THEN** the buffer SHALL contain `<![CDATA[` + the original string + `]]>` with no modifications

#### Scenario: Null data produces empty CDATA section
- **WHEN** `writeData` is called with `null`
- **THEN** the buffer SHALL contain `<![CDATA[]]>` and no NullPointerException SHALL be thrown

### Requirement: sendReport sets XML content type before writing response
`AbstractMethod.sendReport()` SHALL call `resp.setContentType("text/xml; charset=UTF-8")` before writing the XML multi-status response body, so that browsers cannot sniff the response as HTML and misinterpret XML-escaped user-controlled values as live markup.

#### Scenario: Multi-status response has correct content type
- **WHEN** `sendReport` is called with more than one error entry
- **THEN** the HTTP response SHALL have `Content-Type: text/xml; charset=UTF-8` set before any body bytes are written

#### Scenario: Single-error path is unaffected
- **WHEN** `sendReport` is called with exactly one error entry
- **THEN** `resp.sendError` SHALL be used (no XML body is written) and content type behaviour is unchanged
