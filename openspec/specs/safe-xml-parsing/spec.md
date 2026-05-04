## Requirements

### Requirement: DocumentBuilder disables DTD processing
The `AbstractMethod.getDocumentBuilder()` method SHALL configure the `DocumentBuilderFactory` to disallow DOCTYPE declarations before returning the `DocumentBuilder` instance.

#### Scenario: Request body with DOCTYPE declaration is rejected
- **WHEN** a PROPFIND or LOCK request body contains a DOCTYPE declaration
- **THEN** the parse call throws an exception and the servlet returns HTTP 500

#### Scenario: Request body without DOCTYPE parses normally
- **WHEN** a well-formed PROPFIND or LOCK request body contains no DOCTYPE declaration
- **THEN** the document is parsed successfully and the request proceeds normally

### Requirement: DocumentBuilder disables external entity resolution
The `AbstractMethod.getDocumentBuilder()` method SHALL configure the `DocumentBuilderFactory` to disable resolution of external general entities, external parameter entities, and external DTD loading.

#### Scenario: External entity reference in request body is not resolved
- **WHEN** a request body references an external entity (e.g. `<!ENTITY xxe SYSTEM "file:///etc/passwd">`)
- **THEN** the parser does not perform any file-system or network access to resolve the entity

### Requirement: DocumentBuilder disables XInclude and entity expansion
The `AbstractMethod.getDocumentBuilder()` method SHALL set `XIncludeAware` to `false` and `ExpandEntityReferences` to `false` on the factory.

#### Scenario: XInclude directive in request body is not processed
- **WHEN** a request body contains an XInclude directive
- **THEN** the directive is not processed and no external resource is fetched
