## ADDED Requirements

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
