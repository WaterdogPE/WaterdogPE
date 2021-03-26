# Compiling from source

## Requirements

To compile WaterdogPE you need JDK 11 (or JDK 8 with `java-8` branch, may be outdated).

### Recommended libraries and versions

- Project uses awesome Protocol library provided by CloudBurst. Their library should be compatible with WDPE. However,
  we recommend using our [forked repository](https://github.com/WaterdogPE/Protocol) of this library.
- **For advanced users:** If you require using custom build of raknet library, you can change it inside of `pom.xml`
  file using `raklib.version` property. Make sure your version is compatible with WDPE and Protocol library.

## How to complete this guide

- To get latest WDPE source we recommend cloning our GitHub repository
  using `git clone https://github.com/WaterdogPE/WaterdogPE.git`.
- cd into `WaterdogPE` folder.
- Compile sources using Maven. You can use Maven Wrapper `mvnw clean install`.
- Once Maven finishes the build you can find your executable `waterdog-1.0.0-SNAPSHOT.jar` in `target` folder.