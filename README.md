# WebDAV Servlet

This project contains a Java servlet that offers basic WebDAV support. 
It is maintained by [INFO](https://info.nl) who is a partner of [Lifely](https://lifely.nl/) in the work we do for [Dimpact](https://www.dimpact.nl/).
The project originally started as a fork from [webdav-servlet](https://github.com/ceefour/webdav-servlet).
Many thanks go out to the original authors and contributors. The license is kept as-is.

To implement a WebDAV store only one Java interface (`IWebdavStorage`) needs to be implemented.
An example store (`LocalFileSystemStorage`), which uses the local filesystem, is provided.
  
This servlet only supports the most basic data access options. 
Versioning or user management are not supported
  
## Installation & configuration

Please see [INSTALL.md](./INSTALL.md).


