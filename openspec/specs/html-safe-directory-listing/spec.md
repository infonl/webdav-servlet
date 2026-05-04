## Requirements

### Requirement: HTML-encode path in directory listing title and heading
The system SHALL HTML-encode the `path` value before inserting it into the `<title>` element and the default `<h1>` heading of the directory listing response, so that characters such as `<`, `>`, `"`, `&`, and `'` do not break HTML structure or execute as script.

#### Scenario: Path contains HTML special characters in title
- **WHEN** a GET request is made for a folder whose path contains `<`, `>`, `"`, `&`, or `'`
- **THEN** the HTML `<title>` element SHALL contain the HTML-encoded form of those characters (e.g. `&lt;`, `&gt;`, `&quot;`, `&amp;`, `&#x27;`) and no raw unencoded special characters

#### Scenario: Path contains HTML special characters in heading
- **WHEN** a GET request is made for a folder whose path contains `<`, `>`, `"`, `&`, or `'`
- **THEN** the default `<h1>` heading returned by `getHeader` SHALL contain the HTML-encoded form of those characters and no raw unencoded special characters

#### Scenario: Path contains no special characters
- **WHEN** a GET request is made for a folder whose path contains no HTML special characters
- **THEN** the title and heading SHALL display the path unchanged

### Requirement: HTML-encode child resource names in directory listing rows
The system SHALL HTML-encode each child resource name before using it as an `href` attribute value and as visible link text in the directory listing table, so that names containing HTML special characters cannot inject markup or script.

#### Scenario: Child name contains HTML special characters in link text
- **WHEN** a folder listing is rendered and a child resource name contains `<`, `>`, `"`, `&`, or `'`
- **THEN** the visible link text in the table row SHALL display the HTML-encoded form of those characters

#### Scenario: Child name contains HTML special characters in href attribute
- **WHEN** a folder listing is rendered and a child resource name contains `<`, `>`, `"`, `&`, or `'`
- **THEN** the `href` attribute value SHALL contain the HTML-encoded form of those characters, preventing attribute injection

#### Scenario: Child name contains no special characters
- **WHEN** a folder listing is rendered and a child resource name contains no HTML special characters
- **THEN** the link text and href SHALL display the name unchanged
