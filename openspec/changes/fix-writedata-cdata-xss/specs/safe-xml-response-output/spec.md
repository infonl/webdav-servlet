## ADDED Requirements

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
