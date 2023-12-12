# WebDAV Servlet

This project contains a Java servlet that offers basic WebDAV support. 
It originally started as a fork from [webdav-servlet](https://github.com/ceefour/webdav-servlet).
Many thanks go out to the original authors and contributors. The license is kept as-is.

To implement a WebDAV store only one Java interface (`IWebdavStorage`) needs to be implemented.
An example store (`LocalFileSystemStorage`), which uses the local filesystem, is provided.
  
This servlet only supports the most basic data access options. 
Versioning or user management are not supported
  
## Requirements

- JDK 1.7 or higher

## Installation & configuration

1. Add a dependency in your Java web project so that the `webdav-servlet.jar` will be added to the `/WEB-INF/lib/` of your webapp 
2. Add the following to the `web.xml` file in your Java webapp:
```xml  
  	<servlet>
		<servlet-name>webdav</servlet-name>
		<servlet-class>
			net.sf.webdav.WebdavServlet
		</servlet-class>
		<init-param>
			<param-name>ResourceHandlerImplementation</param-name>
			<param-value>
				net.sf.webdav.LocalFileSystemStore
			</param-value>
			<description>
				name of the class that implements
				net.sf.webdav.WebdavStore
			</description>
		</init-param>
		<init-param>
			<param-name>rootpath</param-name>
			<param-value>/tmp/webdav</param-value>
			<description>
				folder where webdavcontent on the local filesystem is stored
			</description>
		</init-param>
		<init-param>
			<param-name>storeDebug</param-name>
			<param-value>0</param-value>
			<description>
				triggers debug output of the
				ResourceHandlerImplementation (0 = off , 1 = on) off by default
			</description>
		</init-param>
	</servlet>
	<servlet-mapping>
		<servlet-name>webdav</servlet-name>
		<url-pattern>/*</url-pattern>
	</servlet-mapping>
```
             
Further configuration options:
- If you want to use the reference implementation (`LocalFileSystemStore`), set the parameter `rootpath` to where you want to store your files.
- if you have implemented your own store, specify your store's Java class in the parameter `ResourceHandlerImplementation`.
- With the default `/*` servlet mapping, every request to the webapp is handled by the servlet. Change this if you wish.
- Using the `storeDebug` init parameter you can trigger the reference store implementation to log debug messages at every method call. This parameter is optional and can be omitted.


