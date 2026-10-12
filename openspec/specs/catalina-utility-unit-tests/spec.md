# catalina-utility-unit-tests Specification

## Purpose
Unit tests for the Catalina helpers used in production: `RequestUtil.URLDecode`, `URLEncoder` and `XMLHelper`.
## Requirements
### Requirement: RequestUtil.URLDecode decodes percent-encoding
The test suite SHALL verify the `URLDecode` overloads used by `DoCopy`.

#### Scenario: Percent-encoded UTF-8
- **WHEN** `URLDecode("/a%20b/%C3%A9", "UTF8")` is called
- **THEN** returns `"/a b/é"`

#### Scenario: Plus sign
- **WHEN** a string containing `+` is decoded
- **THEN** `+` is converted to a space

#### Scenario: Null input
- **WHEN** `URLDecode(null)` or `URLDecode((byte[]) null)` is called
- **THEN** returns `null`

#### Scenario: Unsupported encoding
- **WHEN** an unknown encoding name is passed to `URLDecode(String, enc)`
- **THEN** returns `null` without throwing (current behavior; see findings.md #32)

#### Scenario: Unsupported encoding on byte array
- **WHEN** an unknown encoding name is passed to `URLDecode(byte[], enc)`
- **THEN** decoding falls back to the platform default without throwing

### Requirement: URLEncoder encodes unsafe characters
The test suite SHALL verify `URLEncoder.encode`.

#### Scenario: Safe characters untouched
- **WHEN** a path containing only safe characters (letters, digits and characters added with `addSafeCharacter`) is encoded
- **THEN** it is returned unchanged

#### Scenario: Slash unsafe by default
- **WHEN** `/a/b` is encoded by a `URLEncoder` without `/` added as safe character
- **THEN** returns `%2Fa%2Fb` (`AbstractMethod` adds `/` explicitly)

#### Scenario: Unsafe and non-ASCII characters
- **WHEN** a path containing spaces, `#`, `%` and non-ASCII characters is encoded
- **THEN** each unsafe byte is emitted as `%XX` using its UTF-8 representation

### Requirement: XMLHelper finds elements and properties
The test suite SHALL verify `findSubElement` and `getPropertiesFromXML`.

#### Scenario: Sub-element found by local name
- **WHEN** `findSubElement` is called with a namespaced parent containing the named child
- **THEN** the child node is returned

#### Scenario: Sub-element absent
- **WHEN** no child with the local name exists, or only text nodes are present
- **THEN** returns `null`

#### Scenario: Properties extracted
- **WHEN** `getPropertiesFromXML` is called with a `<prop>` node containing `<D:a/>` and `<D:b/>`
- **THEN** returns a vector with the namespace-qualified names `"DAV::a"` and `"DAV::b"`, ignoring non-element children

