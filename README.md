## A Pure Java Piping Server

This project brings the power of data piping directly to your Java applications, eliminating the need for external binaries or complex configurations. Buckle up and get ready to stream data with ease! (original: https://github.com/nwtgck/piping-server)


### Motivation:
My attempts to incorporate the server binary into Java using JNI or WASM proved unsuccessful. Additionally, navigating the complexities of running binaries on Android Q, especially without rooting, posed significant challenges.

Given the absence of a Java or JVM implementation, it seems opportune for me to consider developing one.

### Implementation Details:
Two implementation options are available:
* JDK Core Implementation: This option prioritizes simplicity and core functionality, utilizing the base Java Development Kit.

* Jetty Integration: This option leverages the robust Jetty library for a more comprehensive solution. It offers partial multipart form support, but may be less portable compared to the core JDK implementation.

### Functionality:

* Flexible Connection Order: Senders and receivers can establish connections in any sequence, enhancing adaptability within data pipelines.
* Multi-Receiver Support: The server can efficiently handle data broadcast scenarios, supporting configurations like `?n=3` for one to three data stream.

### Future Enhancements:
* Documentation & Assets: Development of comprehensive help documentation, a favicon, and support for additional paths is ongoing. Contributions to these areas are highly encouraged.
* Community-Driven Release: We seek the community's collaboration for code refinements and a polished release that empowers Java developers around the world.

### Beyond
We invite developers to join the development process and contribute to building a reliable, pure Java data piping server that empowers the global Java community.


### Side Note
Initially, I attempted to implement the functionality using only the plain JDK, but encountered a roadblock with multipart form handling. Realizing I needed additional libraries, I delved into implementation and research, only to discover that the issue lay not with the server's capabilities, but with the browser's handling of multipart forms.

Upon reflection, it became clear that simplicity on the server-side might be the optimal approach, delegating the heavier lifting to the client. With this insight, I returned to the initial plain JDK implementation, prioritizing simplicity and efficiency.

The jetty implementation partially support multipart form, but I prefer the plain JDK implementation, which is self-contain.





