## ADDED Requirements

### Requirement: PROPFIND renders requested live properties
The test suite SHALL verify that `DoPropfind` renders each supported live property when explicitly requested via `<prop>`, for both files and folders.

#### Scenario: Individual properties on a file
- **WHEN** a PROPFIND body requests `getcontentlength`, `getcontenttype`, `getetag`, `getlastmodified`, `creationdate`, `displayname`, `resourcetype`, `supportedlock` and `lockdiscovery` for a file
- **THEN** the 207 response contains each property with its value in a `200 OK` propstat

#### Scenario: Individual properties on a folder
- **WHEN** the same properties are requested for a folder
- **THEN** `resourcetype` contains `<collection/>` and file-only properties (`getcontentlength`, `getcontenttype`, `getetag`) are reported in a `404 Not Found` propstat

#### Scenario: Unknown property
- **WHEN** a PROPFIND body requests a property the server does not know
- **THEN** that property is listed in a `404 Not Found` propstat

### Requirement: PROPFIND supports propname and allprop
The test suite SHALL verify the `propname` and `allprop` request variants.

#### Scenario: propname request
- **WHEN** the body contains `<propname/>`
- **THEN** the response lists property names without values

#### Scenario: allprop request on folder with depth 1
- **WHEN** the body contains `<allprop/>` and `Depth: 1` on a folder with children
- **THEN** the response contains one `<response>` per resource with all live properties

### Requirement: PROPFIND reports lock discovery
The test suite SHALL verify lock discovery output for locked resources.

#### Scenario: Exclusive lock on resource
- **WHEN** PROPFIND targets a resource holding an exclusive write lock
- **THEN** `lockdiscovery` contains `activelock` with `exclusive` scope, depth, owner, timeout and lock token

#### Scenario: Shared lock on resource
- **WHEN** PROPFIND targets a resource holding a shared lock
- **THEN** `lockdiscovery` contains `activelock` with `shared` scope

### Requirement: PROPFIND handles missing resources and errors
The test suite SHALL verify error branches in `DoPropfind.execute`.

#### Scenario: Resource not found
- **WHEN** the store returns `null` for the requested path
- **THEN** `404 Not Found` is sent

#### Scenario: Store access denied
- **WHEN** the store throws `AccessDeniedException`
- **THEN** `403 Forbidden` is sent

#### Scenario: Malformed request body
- **WHEN** the request body is not well-formed XML
- **THEN** `500 Internal Server Error` is sent and no response body is written

### Requirement: PROPPATCH handles set and remove
The test suite SHALL verify PROPPATCH set/remove processing and error branches.

#### Scenario: Set and remove properties
- **WHEN** a PROPPATCH body contains both `<set>` and `<remove>` elements
- **THEN** a 207 multistatus lists each property with `200 OK` and no properties are written to the store (current behavior; see findings.md #3)

#### Scenario: Read-only servlet
- **WHEN** PROPPATCH is executed with `readOnly=true`
- **THEN** `403 Forbidden` is sent

#### Scenario: Resource locked by other owner
- **WHEN** the target is locked and the request carries no matching lock token
- **THEN** `423 Locked` is returned

#### Scenario: Resource not found
- **WHEN** the store returns `null` for the target
- **THEN** `404 Not Found` is sent

#### Scenario: Malformed body
- **WHEN** the body is not well-formed XML
- **THEN** `500 Internal Server Error` is sent
