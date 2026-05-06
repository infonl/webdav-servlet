# WebDAV Servlet

[![GitHub CI](https://github.com/infonl/webdav-servlet/actions/workflows/build-test-publish.yml/badge.svg)](https://github.com/infonl/webdav-servlet/actions/workflows/build-test-publish.yml)
[![Codecov](https://codecov.io/gh/infonl/webdav-servlet/branch/main/graph/badge.svg)](https://app.codecov.io/gh/infonl/webdav-servlet/)
[![License](https://img.shields.io/badge/License-EUPL_1.2-blue.svg)](https://opensource.org/license/eupl-1-2/)

This project contains a Java servlet that offers basic [WebDAV](https://en.wikipedia.org/wiki/WebDAV) support. 
It is maintained by [INFO](https://info.nl), and is currently primarily funded by [Dimpact](https://www.dimpact.nl/).
The project originally started as a fork from [webdav-servlet](https://github.com/ceefour/webdav-servlet).
Many thanks go out to the original authors and contributors. 
The [license](LICENSE.md) was changed from Apache 2.0 to EUPL 1.2 or later in 2026.

To implement a WebDAV store only one Java interface (`IWebdavStorage`) needs to be implemented.
An example store (`LocalFileSystemStorage`), which uses the local filesystem, is provided.
  
This servlet only supports the most basic data access options. 
Versioning or user management are not supported
  
## Installation & configuration

Please see [INSTALL.md](./INSTALL.md).

## Claude and OpenSpec AI tooling

The project supports specific instructions for [Claude](https://claude.ai/) and [OpenSpec](https://openspec.dev/) AI tooling.
